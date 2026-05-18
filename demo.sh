#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# ─── Couleurs ────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}✓ $1${NC}"; }
info() { echo -e "${YELLOW}▶ $1${NC}"; }
err()  { echo -e "${RED}✗ $1${NC}"; exit 1; }

# ─── 1. Infrastructure Docker ────────────────────────────────────────────────
info "Démarrage de l'infrastructure (PostgreSQL · Keycloak · Mailpit)..."
cd "$ROOT/infrastructure"
docker compose up -d postgres mailpit keycloak
ok "Conteneurs démarrés"

# ─── 2. Attendre Keycloak ────────────────────────────────────────────────────
info "Attente de Keycloak (peut prendre ~30s)..."
until curl -sf http://localhost:8080/realms/master > /dev/null 2>&1; do
  printf "."
  sleep 3
done
echo ""
ok "Keycloak prêt"

# ─── 3. Configurer le realm (idempotent) ─────────────────────────────────────
info "Configuration du realm customer-platform..."

ADMIN_TOKEN=$(curl -sf -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli&username=admin&password=admin&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

# Realm
curl -sf -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"customer-platform","enabled":true}' > /dev/null 2>&1 || true

# Client
curl -sf -X POST http://localhost:8080/admin/realms/customer-platform/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"customer-platform-frontend","enabled":true,"publicClient":true,"directAccessGrantsEnabled":true,"redirectUris":["*"],"webOrigins":["*"]}' > /dev/null 2>&1 || true

# Rôles
for ROLE in CUSTOMER ADMIN LOYALTY_MANAGER; do
  curl -sf -X POST http://localhost:8080/admin/realms/customer-platform/roles \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$ROLE\"}" > /dev/null 2>&1 || true
done

# Utilisateur de test
curl -sf -X POST http://localhost:8080/admin/realms/customer-platform/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"user.test","email":"user.test@test.com","firstName":"User","lastName":"Test","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' > /dev/null 2>&1 || true

# Assignation des rôles CUSTOMER + ADMIN
USER_ID=$(curl -sf "http://localhost:8080/admin/realms/customer-platform/users?username=user.test" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id']) if d else print('')")

if [ -n "$USER_ID" ]; then
  for ROLE in CUSTOMER ADMIN; do
    ROLE_ID=$(curl -sf "http://localhost:8080/admin/realms/customer-platform/roles/$ROLE" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
    curl -sf -X POST "http://localhost:8080/admin/realms/customer-platform/users/$USER_ID/role-mappings/realm" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "[{\"id\":\"$ROLE_ID\",\"name\":\"$ROLE\"}]" > /dev/null 2>&1 || true
  done
fi
ok "Realm configuré (user.test / password)"

# ─── 4. Build des services ───────────────────────────────────────────────────
info "Build notification-service..."
cd "$ROOT/services/notification-service"
mvn clean package -DskipTests -q
ok "notification-service buildé"

info "Build loyalty-service..."
cd "$ROOT/services/loyalty-service"
mvn clean package -DskipTests -q
ok "loyalty-service buildé"

# ─── 5. Lancer les services ──────────────────────────────────────────────────
info "Arrêt des services existants..."
lsof -ti:8082 | xargs kill -9 2>/dev/null || true
lsof -ti:8083 | xargs kill -9 2>/dev/null || true
sleep 1

info "Démarrage des services..."
java -jar "$ROOT/services/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar" \
  > /tmp/notification-service.log 2>&1 &
java -jar "$ROOT/services/loyalty-service/target/loyalty-service-0.0.1-SNAPSHOT.jar" \
  > /tmp/loyalty-service.log 2>&1 &

# Attendre que les ports soient ouverts
for PORT in 8082 8083; do
  printf "Attente port $PORT"
  until lsof -ti:$PORT > /dev/null 2>&1; do printf "."; sleep 1; done
  echo ""
done
ok "Services démarrés (logs : /tmp/loyalty-service.log · /tmp/notification-service.log)"

# ─── 6. Résumé ───────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  customer-platform — PRÊT POUR LA DÉMO${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "  Keycloak   → http://localhost:8080  (admin / admin)"
echo "  Mailpit    → http://localhost:8025"
echo "  loyalty    → http://localhost:8082/actuator/health"
echo "  notif      → http://localhost:8083/actuator/health"
echo ""
echo -e "${YELLOW}  Test pipeline complet (copier-coller) :${NC}"
echo ""
cat << 'EOF'
TOKEN=$(curl -s -X POST http://localhost:8080/realms/customer-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=customer-platform-frontend&username=user.test&password=password&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))") \
&& curl -s -X POST http://localhost:8082/api/loyalty/me/points \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"points": 100, "reason": "Achat", "email": "user.test@test.com"}'
EOF
echo ""
