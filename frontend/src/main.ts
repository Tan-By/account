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

  console.log('🔒 路由守卫检查:', {
    from: from.path,
    to: to.path,
    requiresAuth,
    hasToken: !!token,
    tokenLength: token?.length,
    tokenPreview: token ? token.substring(0, 20) + '...' : 'null'
  });

  // 如果需要认证但没有 token，跳转到登录页
  if (requiresAuth && !token) {
    console.log('❌ 需要认证但无 token，跳转到登录页');
    next('/login');
    return;
  }

  // 如果已登录但访问登录页，跳转到首页
  if (to.path === '/login' && token) {
    console.log('✅ 已登录，从登录页跳转到首页');
    next('/');
    return;
  }

  // 其他情况允许访问
  console.log('✅ 路由守卫通过，允许访问:', to.path);
  next();
});

createApp(App).use(router).mount('#app');


