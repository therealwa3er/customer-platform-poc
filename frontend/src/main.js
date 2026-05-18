import { createApp } from 'vue';
import App from './App.vue';
import keycloak from './keycloak';

keycloak.init({
  onLoad: 'login-required',
  checkLoginIframe: false,
}).then((authenticated) => {
  if (authenticated) {
    const app = createApp(App);
    app.provide('keycloak', keycloak);
    app.mount('#app');
  }
});
