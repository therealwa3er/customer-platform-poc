<script setup>
import { inject, onMounted, ref } from 'vue';

const keycloak = inject('keycloak');

const loyalty = ref(null);
const error = ref(null);

onMounted(async () => {
  try {
    await keycloak.updateToken(30);
    const response = await fetch('http://localhost:8082/api/loyalty/me', {
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Erreur API fidélité');
    }

    loyalty.value = await response.json();
  } catch (e) {
    error.value = e.message;
  }
});

const logout = () => {
  keycloak.logout({
    redirectUri: 'http://localhost:5173',
  });
};
</script>

<template>
  <main>
    <h1>Mon espace fidélité</h1>

    <p>Bonjour {{ keycloak.tokenParsed?.preferred_username }}</p>

    <button @click="logout">Se déconnecter</button>

    <section v-if="loyalty">
      <h2>Mes points</h2>
      <p>Points : {{ loyalty.points }}</p>
      <p>Niveau : {{ loyalty.tier }}</p>
    </section>

    <p v-if="error">{{ error }}</p>
  </main>
</template>
