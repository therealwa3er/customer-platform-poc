import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'customer-platform',
  clientId: 'customer-platform-frontend',
});

export default keycloak;
