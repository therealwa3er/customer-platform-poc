# frontend

Interface Vue 3 — espace fidélité client, sécurisée via Keycloak (PKCE).

**Port :** `http://localhost:5173`

---

## Flux

```
Ouverture http://localhost:5173
        ↓
Redirection login Keycloak (login-required)
        ↓
user.test / password
        ↓
Redirect → App.vue
        ↓
GET /api/loyalty/me avec Bearer JWT
        ↓
Affichage points + tier
```

---

## Prérequis actifs

| Service | Port | Rôle |
|---------|------|------|cd services/loyalty-service
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests -q
java -jar target/loyalty-service-0.0.1-SNAPSHOT.jar
| Keycloak | 8080 | Auth OAuth2/JWT |
| loyalty-service | 8082 | API points fidélité |
| PostgreSQL | 5432 | Base de données |

> Utilise `./demo.sh` depuis la racine pour tout démarrer automatiquement.

---

## Lancer le frontend

```bash
# Libérer le port si déjà occupé (Vite est configuré en strictPort)
lsof -ti:5173 | xargs kill 2>/dev/null; true

cd frontend
npm run dev
# → http://localhost:5173
```

---

## Structure

```
src/
  keycloak.js   ← config Keycloak (url, realm, clientId)
  main.js       ← init Keycloak → mount App
  App.vue       ← affiche username, points, tier + bouton logout
```

---

## Compte de test

| Champ | Valeur |
|-------|--------|
| username | `user.test` |
| password | `password` |
