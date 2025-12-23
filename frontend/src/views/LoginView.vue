<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">AC</div>
        <h1 class="login-title">轻量财务系统</h1>
        <p class="login-subtitle">类 GnuCash · 学习版</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username" class="form-label">用户名</label>
          <input
            id="username"
            v-model="username"
            type="text"
            class="form-input"
            placeholder="请输入用户名"
            required
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <label for="password" class="form-label">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="form-input"
            placeholder="请输入密码"
            required
            autocomplete="current-password"
          />
        </div>

        <div v-if="error" class="error-message">{{ error }}</div>

        <button type="submit" class="login-button" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="login-footer">
        <p>默认账号：admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '../api';

const router = useRouter();
const username = ref('admin');
const password = ref('');
const loading = ref(false);
const error = ref('');

const handleLogin = async () => {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码';
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    console.log('开始登录流程...');
    const result = await login(username.value, password.value);
    console.log('登录成功，结果:', result);
    
    // 验证 token 是否已保存
    const savedToken = localStorage.getItem('token');
    console.log('保存的 Token:', savedToken ? `已保存 (长度: ${savedToken.length})` : '未保存');
    console.log('Token 内容:', savedToken?.substring(0, 20) + '...');
    
    if (!savedToken) {
      throw new Error('Token 保存失败');
    }
    
    // 确保 token 已保存后再跳转
    console.log('准备跳转到首页...');
    console.log('当前路由:', router.currentRoute.value.path);
    
    // 使用 router.push 并等待完成
    await router.push('/');
    console.log('路由跳转完成，当前路由:', router.currentRoute.value.path);
    
    // 如果跳转后仍在登录页，可能是路由守卫拦截了
    if (router.currentRoute.value.path === '/login') {
      console.warn('警告：跳转后仍在登录页，可能是路由守卫拦截');
      const tokenAfterRedirect = localStorage.getItem('token');
      console.log('跳转后的 Token:', tokenAfterRedirect ? '存在' : '不存在');
    }
  } catch (err: any) {
    console.error('登录错误:', err);
    console.error('错误详情:', {
      message: err.message,
      response: err.response?.data,
      status: err.response?.status
    });
    error.value = err.response?.data?.message || err.message || '登录失败，请检查用户名和密码';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #f6f7fb 0%, #eceef3 100%);
  padding: 20px;
  animation: fade-in 320ms ease-out;
}

.login-card {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.06);
  padding: 40px;
  width: 100%;
  max-width: 400px;
  border: 1px solid rgba(60, 60, 67, 0.12);
  animation: card-entrance 460ms cubic-bezier(0.25, 0.1, 0.25, 1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(145deg, #0a84ff 0%, #007aff 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.04em;
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.25);
  animation: logo-pulse 2600ms ease-in-out infinite;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #6e6e73;
  margin: 0;
}

.login-form {
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #3c3c43;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  border: 1px solid rgba(60, 60, 67, 0.22);
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  box-sizing: border-box;
  background: #fbfbfd;

  &:focus {
    outline: none;
    border-color: #007aff;
    box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.16);
    background: #ffffff;
  }

  &::placeholder {
    color: #9ca3af;
  }
}

.error-message {
  padding: 12px;
  background: #fff2f2;
  border: 1px solid rgba(255, 59, 48, 0.24);
  border-radius: 10px;
  color: #d70015;
  font-size: 14px;
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(145deg, #0a84ff 0%, #007aff 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 10px 26px rgba(0, 122, 255, 0.24);

  &:hover:not(:disabled) {
    opacity: 0.96;
    transform: translateY(-1px);
    filter: saturate(1.05);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 6px 18px rgba(0, 122, 255, 0.18);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    box-shadow: none;
  }
}

.login-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid rgba(60, 60, 67, 0.12);

  p {
    font-size: 12px;
    color: #6e6e73;
    margin: 0;
  }
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes card-entrance {
  0% {
    opacity: 0;
    transform: translateY(14px) scale(0.985);
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
  }
  60% {
    opacity: 1;
    transform: translateY(-2px) scale(1.002);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

@keyframes logo-pulse {
  0%,
  100% {
    transform: translateY(0);
    box-shadow: 0 8px 24px rgba(0, 122, 255, 0.25);
  }
  50% {
    transform: translateY(-2px);
    box-shadow: 0 12px 30px rgba(0, 122, 255, 0.28);
  }
}
</style>

