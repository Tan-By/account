import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import routes from './router';
import './styles.scss';

const router = createRouter({
  history: createWebHistory(),
  routes
});

createApp(App).use(router).mount('#app');


