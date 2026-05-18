# customer-service

Spring Boot microservice faisant partie de la **customer-platform**.  
Il expose des endpoints REST protégés par JWT via Keycloak (OAuth2 Resource Server).

---

## Liens importants

| Service | URL | Description |
|---------|-----|-------------|
| Keycloak Admin | http://localhost:8080/admin | Console admin — `admin` / `admin` |
| Keycloak Login | http://localhost:8080/realms/customer-platform/account | Page de connexion du realm |
| Mailpit | http://localhost:8025 | Boîte mail de dev — reçoit les codes OTP |
| customer-service | http://localhost:8081 | API REST |

### Tester le flow OTP email avec user.test

1. Aller sur **http://localhost:8080/realms/customer-platform/account**
2. Se connecter avec `user.test` / `<mot de passe>`
3. Keycloak envoie un code OTP par email → ouvrir **http://localhost:8025**
4. Copier le code reçu dans le sujet **"Your access code"** (valide 5 minutes)
5. Saisir le code dans Keycloak → connexion réussie

---

## Prérequis

| Outil | Version |
|-------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Keycloak | en cours d'exécution sur `localhost:8080` |

> **Java 21 doit être actif dans le terminal avant de lancer l'app.**  
> Si ce n'est pas le cas, ajouter dans `~/.zshrc` :
> ```bash
> export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
> export PATH="$JAVA_HOME/bin:$PATH"
> ```
> Puis : `source ~/.zshrc`

---

## Lancer l'application

```bash
cd services/customer-service

# Build + démarrage
mvn clean package -DskipTests -q && java -jar target/customer-service-0.0.1-SNAPSHOT.jar
```

L'app démarre sur **http://localhost:8081**

---

## Arrêter l'application

```bash
lsof -ti:8081 | xargs kill
```

---

## Endpoints

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| `GET` | `/api/public/ping` | Non | Vérifie que le service tourne |
| `GET` | `/api/customers/me` | JWT requis | Retourne les infos de l'utilisateur connecté |
| `GET` | `/api/admin/dashboard` | Rôle `ADMIN` | Dashboard admin |
| `GET` | `/api/loyalty/me` | Rôles `CUSTOMER`, `LOYALTY_MANAGER`, `ADMIN` | Points de fidélité |
| `GET` | `/actuator/health` | Non | État de santé du service |
| `GET` | `/actuator/info` | Non | Informations de l'application |
| `GET` | `/actuator/metrics` | Non | Métriques |

---

## Tester les endpoints

### Endpoint public (200 OK)
```bash
curl -i http://localhost:8081/api/public/ping
curl -i http://localhost:8081/actuator/health
```

### Endpoint protégé sans token (401 Unauthorized)
```bash
curl -i http://localhost:8081/api/customers/me
```

### Endpoint protégé avec token JWT
```bash
# 1. Obtenir un token depuis Keycloak
TOKEN=$(curl -s -X POST http://localhost:8080/realms/customer-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=customer-platform-frontend" \
  -d "username=user.test" \
  -d "password=password" \
  -d "grant_type=password" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# 2. Appeler l'endpoint protégé
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/customers/me
```

Réponse attendue :
```json
{
  "userId": "...",
  "username": "...",
  "email": "..."
}
```

---

## Configuration (`application.yml`)

```yaml
server:
  port: 8081

spring:
  application:
    name: customer-service
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/customer-platform

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## Structure du projet

```
src/main/java/com/customerplatform/customer/
├── CustomerServiceApplication.java   # Point d'entrée
├── config/
│   ├── SecurityConfig.java           # Configuration Spring Security + OAuth2
│   └── JwtAuthConverter.java         # Convertit realm_access.roles → ROLE_*
└── controller/
    ├── PublicController.java          # GET /api/public/ping
    ├── CustomerController.java        # GET /api/customers/me (JWT)
    ├── AdminController.java           # GET /api/admin/dashboard (ADMIN)
    └── LoyaltyController.java         # GET /api/loyalty/me (CUSTOMER/ADMIN)
```

---

## Contrôle d'accès par rôle

| Rôle Keycloak | `/api/customers/me` | `/api/loyalty/me` | `/api/admin/dashboard` |
|---------------|---------------------|-------------------|------------------------|
| `CUSTOMER` | ✅ | ✅ | ❌ 403 |
| `LOYALTY_MANAGER` | ✅ | ✅ | ❌ 403 |
| `ADMIN` | ✅ | ✅ | ✅ |
| _(aucun token)_ | ❌ 401 | ❌ 401 | ❌ 401 |
