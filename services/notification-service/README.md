# notification-service

Service de notification par email, construit avec **Spring WebFlux** + **Spring Integration** + **Spring Boot Mail**.

**Port :** `8083`

---

## Pourquoi WebFlux ?

Le notification-service est un bon candidat pour WebFlux car il fait surtout de l'I/O : appels externes, envoi email, intégrations. Le non-blocking permet de mieux gérer la concurrence sans bloquer inutilement des threads.

---

## Structure

```
src/main/java/com/customerplatform/notification/
  NotificationServiceApplication.java
  config/
    NotificationIntegrationConfig.java   ← canal + flow Spring Integration
  controller/
    NotificationController.java          ← POST /api/notifications/loyalty-points
  event/
    LoyaltyPointsAddedEvent.java         ← record métier
  service/
    EmailNotificationService.java        ← envoi email via JavaMailSender
```

---

## Endpoints

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| POST | `/api/notifications/loyalty-points` | Non | Déclenche un email de notification de points |
| GET | `/actuator/health` | Non | Health check |
| GET | `/actuator/info` | Non | Info |
| GET | `/actuator/metrics` | Non | Métriques |

---

## Prérequis — Mailpit

Mailpit est un serveur SMTP local de test (intercepte les emails sans les envoyer).

```bash
# Lancer via Docker
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

# Interface web
open http://localhost:8025
```

---

## Pipeline complet (via loyalty-service)

Pour déclencher une notification depuis loyalty-service :

Prérequis actifs : Keycloak `:8080`, PostgreSQL `:5432`, Mailpit `:1025`, loyalty-service `:8082`, notification-service `:8083`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/customer-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=customer-platform-frontend&username=user.test&password=password&grant_type=password" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))") \
&& curl -s -X POST http://localhost:8082/api/loyalty/me/points \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"points": 100, "reason": "Achat", "email": "user.test@test.com"}'
```

L'email apparaît dans Mailpit : http://localhost:8025

Flux : `loyalty-service :8082` → `RestClient` → `POST /api/notifications/loyalty-points` → `DirectChannel` → `EmailNotificationService` → Mailpit `:1025`

---

## Build & Run

```bash
cd services/notification-service

# Build
mvn clean package -DskipTests -q

# Lancer
java -jar target/notification-service-0.0.1-SNAPSHOT.jar

# Arrêter
lsof -ti:8083 | xargs kill
```

---

## Test

```bash
curl -s -X POST http://localhost:8083/api/notifications/loyalty-points \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "user-123",
    "email": "user.test@test.com",
    "pointsAdded": 50,
    "totalPoints": 350
  }'
# → HTTP 202 Accepted
# → Email visible dans Mailpit : http://localhost:8025
```
