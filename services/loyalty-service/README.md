# loyalty-service

Spring Boot microservice de fidélité — partie de la **customer-platform**.  
Gère les points, transactions et niveaux de fidélité des clients.

---

## Stack

| Outil | Version |
|-------|---------|
| Java | 21 |
| Spring Boot | 3.4.5 |
| PostgreSQL | 16 (via Docker) |
| Port | 8082 |

---

## Lancer l'application

```bash
# 1. Démarrer l'infrastructure (PostgreSQL + Keycloak)
cd infrastructure
docker compose up -d postgres keycloak

# 2. Attendre que Keycloak soit prêt (~30s), puis vérifier :
curl http://localhost:8080/realms/customer-platform

# 3. Builder et lancer le service
cd services/loyalty-service
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests -q
java -jar target/loyalty-service-0.0.1-SNAPSHOT.jar
```

## Arrêter

```bash
lsof -ti:8082 | xargs kill
```

---

## Configuration Keycloak (après chaque redémarrage Mac)

> Le realm est perdu à chaque redémarrage (pas de volume persistant). À recréer via l'API :

```bash
# Token admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli&username=admin&password=admin&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

# Créer le realm
curl -s -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"customer-platform","enabled":true}' -w " → %{http_code}\n"

# Créer le client
curl -s -X POST http://localhost:8080/admin/realms/customer-platform/clients \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"customer-platform-frontend","enabled":true,"publicClient":true,"directAccessGrantsEnabled":true,"redirectUris":["*"],"webOrigins":["*"]}' \
  -w " → %{http_code}\n"

# Créer les rôles
for ROLE in CUSTOMER ADMIN LOYALTY_MANAGER; do
  curl -s -X POST http://localhost:8080/admin/realms/customer-platform/roles \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$ROLE\"}" -w " → %{http_code}\n"
done

# Créer l'utilisateur de test (firstName/lastName requis par Keycloak 26)
curl -s -X POST http://localhost:8080/admin/realms/customer-platform/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"user.test","email":"user.test@test.com","firstName":"User","lastName":"Test","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' \
  -w " → %{http_code}\n"

# Assigner les rôles CUSTOMER et ADMIN à user.test
USER_ID=$(curl -s "http://localhost:8080/admin/realms/customer-platform/users?username=user.test" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")

for ROLE in CUSTOMER ADMIN; do
  ROLE_ID=$(curl -s "http://localhost:8080/admin/realms/customer-platform/roles/$ROLE" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  curl -s -X POST "http://localhost:8080/admin/realms/customer-platform/users/$USER_ID/role-mappings/realm" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "[{\"id\":\"$ROLE_ID\",\"name\":\"$ROLE\"}]" -w "Rôle $ROLE assigné → %{http_code}\n"
done
```

---

## Endpoints

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| `GET` | `/api/loyalty/me` | `CUSTOMER` / `ADMIN` / `LOYALTY_MANAGER` | Voir ses points et tier |
| `POST` | `/api/loyalty/me/points` | `CUSTOMER` / `ADMIN` / `LOYALTY_MANAGER` | Ajouter des points |
| `POST` | `/api/admin/batches/recalculate-tiers` | `ADMIN` | Recalculer tous les tiers (manuel) |
| `GET` | `/actuator/health` | Non | Santé du service |

> Le batch tourne aussi automatiquement chaque nuit à **02:00** via `LoyaltyBatchScheduler`.

---

## Niveaux de fidélité

| Points | Tier |
|--------|------|
| 0 – 999 | `BRONZE` |
| 1 000 – 4 999 | `SILVER` |
| 5 000+ | `GOLD` |

---

## Tester manuellement

### 1. Obtenir un token JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/customer-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=customer-platform-frontend&username=user.test&password=password&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")
```

### 2. Voir son compte fidélité

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/loyalty/me
```

Réponse :
```json
{"customerId": "...", "points": 0, "tier": "BRONZE"}
```

### 3. Ajouter des points (+ envoi email via notification-service)

> Requiert que **notification-service** tourne sur le port 8083 et que **Mailpit** soit lancé.

```bash
curl -s -X POST http://localhost:8082/api/loyalty/me/points \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"points": 500, "reason": "premier achat", "email": "user.test@test.com"}'
```

Réponse :
```json
{"customerId": "...", "points": 500, "tier": "BRONZE"}
```

L'email est visible dans Mailpit : http://localhost:8025

### 4. Lancer le batch de recalcul des tiers (rôle ADMIN requis)

```bash
curl -s -X POST http://localhost:8082/api/admin/batches/recalculate-tiers \
  -H "Authorization: Bearer $TOKEN"
```

Réponse : `Batch recalculation started`

### 5. Vérifier la santé

```bash
curl -s http://localhost:8082/actuator/health
```

---

## Structure du projet

```
src/main/java/com/customerplatform/loyalty/
├── LoyaltyServiceApplication.java
├── config/
│   ├── SecurityConfig.java           # OAuth2 + règles d'accès + @EnableMethodSecurity
│   ├── JwtAuthConverter.java         # realm_access.roles → ROLE_*
│   └── LoyaltyTierBatchConfig.java   # Job + Step Spring Batch
├── controller/
│   ├── LoyaltyController.java        # GET /api/loyalty/me, POST /api/loyalty/me/points
│   └── BatchController.java          # POST /api/admin/batches/recalculate-tiers
├── scheduler/
│   └── LoyaltyBatchScheduler.java    # Recalcul automatique chaque nuit à 02:00
├── dto/
│   ├── LoyaltyAccountResponse.java
│   ├── AddPointsRequest.java         # points + reason + email
│   └── LoyaltyPointsAddedEvent.java  # événement envoyé au notification-service
├── service/
│   ├── LoyaltyService.java
│   └── NotificationClient.java       # RestClient → POST /api/notifications/loyalty-points
├── model/
│   ├── LoyaltyAccount.java
│   ├── LoyaltyTransaction.java
│   └── LoyaltyTier.java
├── repository/
│   ├── LoyaltyAccountRepository.java
│   └── LoyaltyTransactionRepository.java
└── service/
    └── LoyaltyService.java
```
