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
    await login(username.value, password.value);
    router.push('/');
  } catch (err: any) {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #718096;
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
  color: #374151;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;

  &:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }

  &::placeholder {
    color: #9ca3af;
  }
}

.error-message {
  padding: 12px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: 500;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.login-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;

  p {
    font-size: 12px;
    color: #6b7280;
    margin: 0;
  }
}
</style>

