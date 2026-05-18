# Copilot Instructions — customer-platform

Ce fichier est lu automatiquement à chaque nouveau chat. Il décrit l'état du projet, les conventions et les règles à suivre.

---

## Projet

**customer-platform** : plateforme de gestion client composée de microservices Spring Boot, sécurisés via Keycloak (OAuth2/JWT).

Workspace root : `/Volumes/T8/a bit of lines/customer-platform`

---

## Structure

```
frontend/
infrastructure/
  docker-compose.yml
  keycloak/
kubernetes/
services/
  auth-service/
  customer-service/       ← service actif (port 8081)
  loyalty-service/        ← service actif (port 8082)
  notification-service/   ← service actif (port 8083)
```

---

## customer-service — État actuel

**Stack :** Spring Boot 3.4.5 · Java 21 · Maven  
**Port :** 8081  
**Auth :** OAuth2 Resource Server (JWT Keycloak)  
**Keycloak realm :** `customer-platform` sur `localhost:8080`

### Fichiers créés

| Fichier | Rôle |
|---------|------|
| `config/SecurityConfig.java` | Sécurité Spring : règles d'accès par rôle + JWT |
| `config/JwtAuthConverter.java` | Convertit `realm_access.roles` → `ROLE_*` Spring Security |
| `controller/PublicController.java` | `GET /api/public/ping` — public |
| `controller/CustomerController.java` | `GET /api/customers/me` — JWT requis |
| `controller/AdminController.java` | `GET /api/admin/dashboard` — rôle ADMIN |
| `controller/LoyaltyController.java` | `GET /api/loyalty/me` — rôles CUSTOMER/LOYALTY_MANAGER/ADMIN |

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| GET | `/api/public/ping` | Non |
| GET | `/api/customers/me` | JWT requis |
| GET | `/api/admin/dashboard` | Rôle `ADMIN` |
| GET | `/api/loyalty/me` | Rôles `CUSTOMER`, `LOYALTY_MANAGER`, `ADMIN` |
| GET | `/actuator/health` | Non |
| GET | `/actuator/info` | Non |
| GET | `/actuator/metrics` | Non |

---

## Environnement développeur

- **Java 21** installé via Homebrew : `/opt/homebrew/opt/openjdk@21`
- **Maven 3.9.15** via Homebrew (utilise Java 21 si JAVA_HOME est bien défini)
- `~/.zshrc` contient :
  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- `mvn spring-boot:run` ne fonctionne pas (bug ASM) → utiliser `java -jar`

### Commandes utiles

```bash
# Build
mvn clean package -DskipTests -q

# Lancer
java -jar target/customer-service-0.0.1-SNAPSHOT.jar

# Arrêter
lsof -ti:8081 | xargs kill

# Tester
curl -i http://localhost:8081/api/public/ping
curl -i http://localhost:8081/api/customers/me          # → 401
curl -i http://localhost:8081/actuator/health

# Obtenir un token JWT (user de test)
TOKEN=$(curl -s -X POST http://localhost:8080/realms/customer-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=customer-platform-frontend" \
  -d "username=user.test" \
  -d "password=password" \
  -d "grant_type=password" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/customers/me
```

### User de test

| Champ | Valeur |
|-------|--------|
| username | `user.test` |
| password | `password` |
| email | `user.test@test.com` |
| client_id | `customer-platform-frontend` |

---

## Règles à suivre systématiquement

1. **Après chaque ajout de fonctionnalité**, mettre à jour `services/customer-service/README.md` :
   - Ajouter l'endpoint dans le tableau
   - Ajouter la commande curl de test si pertinent
   - Mettre à jour la structure du projet si un nouveau fichier est créé

2. **Mettre à jour ce fichier** (`copilot-instructions.md`) quand :
   - Un nouveau service est activé
   - De nouveaux fichiers structurants sont créés
   - L'environnement change (port, version, etc.)

3. **Conventions de code** :
   - Package racine : `com.customerplatform.<service>`
   - Config dans `config/`, controllers dans `controller/`
   - Pas de docstrings inutiles, pas de sur-ingénierie

4. **Sécurité** : tout endpoint sous `/api/` est protégé par défaut sauf `/api/public/**`

---

## notification-service — État actuel

**Stack :** Spring Boot 3.4.5 · Java 21 · Maven · WebFlux · Spring Integration · Spring Boot Mail  
**Port :** 8083  
**Auth :** Aucune (endpoint interne appelé par loyalty-service)  
**SMTP :** Mailpit sur `localhost:1025` (interface web : `localhost:8025`)

### Fichiers créés

| Fichier | Rôle |
|---------|------|
| `event/LoyaltyPointsAddedEvent.java` | Record métier |
| `controller/NotificationController.java` | `POST /api/notifications/loyalty-points` |
| `config/NotificationIntegrationConfig.java` | Canal + flow Spring Integration |
| `service/EmailNotificationService.java` | Envoi email via JavaMailSender |

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/notifications/loyalty-points` | Non |
| GET | `/actuator/health` | Non |

### Commandes utiles

```bash
# Lancer Mailpit
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

# Build
cd services/notification-service && mvn clean package -DskipTests -q

# Lancer
java -jar target/notification-service-0.0.1-SNAPSHOT.jar

# Arrêter
lsof -ti:8083 | xargs kill

# Tester
curl -s -X POST http://localhost:8083/api/notifications/loyalty-points \
  -H "Content-Type: application/json" \
  -d '{"customerId":"user-123","email":"user.test@test.com","pointsAdded":50,"totalPoints":350}'
```

---

## Prochaines étapes connues

- Ajouter endpoint historique des transactions
- Connecter loyalty-service → notification-service via HTTP client
- Ajouter Mailpit au docker-compose.yml
