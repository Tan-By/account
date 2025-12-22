import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import routes from './router';
import { logout } from './api';
import './styles.scss';

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  const requiresAuth = to.meta.requiresAuth !== false;

  if (requiresAuth && !token) {
    // 需要登录但没有token，跳转到登录页
    next('/login');
  } else if (to.path === '/login' && token) {
    // 已登录但访问登录页，跳转到首页
    next('/');
  } else {
    next();
  }
});

createApp(App).use(router).mount('#app');


