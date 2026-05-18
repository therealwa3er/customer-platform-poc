# customer-platform — Architecture

---

## Vue d'ensemble

```mermaid
graph TB
    subgraph Browser["🌐 Browser"]
        FE["Vue 3 + Keycloak JS<br/>:5173"]
    end

    subgraph Infra["🐳 Docker"]
        KC["Keycloak<br/>:8080<br/>realm: customer-platform"]
        PG[("PostgreSQL<br/>:5432<br/>customer_platform")]
        MP["Mailpit<br/>SMTP :1025<br/>UI :8025"]
    end

    subgraph Services["☕ Spring Boot Services"]
        CS["customer-service<br/>:8081<br/>OAuth2 Resource Server"]
        LS["loyalty-service<br/>:8082<br/>OAuth2 Resource Server<br/>Spring Batch"]
        NS["notification-service<br/>:8083<br/>WebFlux + Spring Integration"]
    end

    FE -- "① login redirect (PKCE)" --> KC
    KC -- "② JWT (realm_access.roles)" --> FE
    FE -- "③ GET /api/loyalty/me<br/>Bearer JWT" --> LS
    FE -- "③ GET /api/customers/me<br/>Bearer JWT" --> CS

    LS -- "JPA" --> PG
    CS -- "JPA" --> PG

    LS -- "④ POST /api/notifications/loyalty-points<br/>RestClient" --> NS
    NS -- "⑤ JavaMailSender → SMTP" --> MP
```

---

## Flux 1 — Login + affichage points (démo frontend)

```mermaid
sequenceDiagram
    actor User
    participant Vue as Vue 3 :5173
    participant KC as Keycloak :8080
    participant LS as loyalty-service :8082

    User->>Vue: Ouvre http://localhost:5173
    Vue->>KC: Redirect login (PKCE)
    User->>KC: user.test / password
    KC-->>Vue: Authorization code → JWT
    Vue->>LS: GET /api/loyalty/me<br/>Authorization: Bearer <JWT>
    LS->>LS: Valide JWT (realm_access.roles → ROLE_CUSTOMER)
    LS-->>Vue: { customerId, points, tier }
    Vue-->>User: "Bonjour user.test — 900 pts — BRONZE"
```

---

## Flux 2 — Ajout de points + notification email

```mermaid
sequenceDiagram
    actor Client as Client (curl / Vue)
    participant LS as loyalty-service :8082
    participant PG as PostgreSQL :5432
    participant NS as notification-service :8083
    participant SI as Spring Integration<br/>DirectChannel
    participant Mail as Mailpit :1025

    Client->>LS: POST /api/loyalty/me/points<br/>{ points, reason, email } + JWT
    LS->>LS: Vérifie rôle CUSTOMER/ADMIN
    LS->>PG: Save LoyaltyAccount + LoyaltyTransaction
    LS->>NS: POST /api/notifications/loyalty-points<br/>RestClient { customerId, email, pointsAdded, totalPoints }
    NS->>SI: MessageBuilder → loyaltyNotificationChannel
    SI->>SI: IntegrationFlow.handle()
    SI->>Mail: SimpleMailMessage via JavaMailSender
    Mail-->>Client: Email visible sur http://localhost:8025
    NS-->>LS: 202 Accepted
    LS-->>Client: { customerId, points, tier }
```

---

## Sécurité JWT

```mermaid
graph LR
    KC["Keycloak<br/>realm: customer-platform"] -- "émet JWT" --> TOKEN

    TOKEN["JWT<br/>realm_access.roles:<br/>CUSTOMER, ADMIN"]

    TOKEN -- "envoyé dans<br/>Authorization: Bearer" --> RS["Resource Server<br/>loyalty-service / customer-service"]

    RS -- "JwtAuthConverter<br/>realm_access.roles → ROLE_*" --> SEC["Spring Security<br/>hasRole('CUSTOMER')<br/>hasRole('ADMIN')"]
```

---

## Structure des services

| Service | Port | Stack | Auth | Base |
|---------|------|-------|------|------|
| `customer-service` | 8081 | Spring MVC · OAuth2 | JWT Keycloak | — |
| `loyalty-service` | 8082 | Spring MVC · JPA · Batch | JWT Keycloak | PostgreSQL |
| `notification-service` | 8083 | WebFlux · Integration · Mail | Aucune (interne) | — |
| Keycloak | 8080 | Docker | — | — |
| PostgreSQL | 5432 | Docker | — | — |
| Mailpit | 1025/8025 | Docker | — | — |

---

## Lancer la démo

```bash
# Tout démarrer (infrastructure + services Java)
./demo.sh

# Frontend
cd frontend && npm run dev
# → http://localhost:5173

# Emails reçus
open http://localhost:8025
```

### Test pipeline complet (curl)

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
