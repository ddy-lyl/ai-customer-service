import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';

import './styles/tokens.css';
import './styles/base.css';
import './styles/markdown.css';

const app = createApp(App);
app.use(createPinia());
app.use(router);

window.addEventListener('app:unauthorized', () => {
  if (router.currentRoute.value.name !== 'login') {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
  }
});

app.mount('#app');
