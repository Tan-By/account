<template>
  <div v-if="route.path === '/login'" class="login-wrapper">
    <RouterView />
  </div>
  <div v-else class="app-shell">
    <aside class="app-shell__sidebar">
      <div class="app-title">
        <div class="app-title__badge">
          <div class="app-title__badge-inner">AC</div>
        </div>
        <div>
          <div class="app-title__text-main">轻量财务系统</div>
          <div class="app-title__text-sub">类 GnuCash · 学习版</div>
        </div>
      </div>

      <div style="margin-top: 16px;">
        <div class="nav-group-title">基础</div>
        <ul class="nav-list">
          <li class="nav-item">
            <RouterLink to="/" class="nav-link" :class="{ 'nav-link--active': isActive('/') }">
              <span class="nav-link__bullet"></span>
              总览
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink to="/init" class="nav-link" :class="{ 'nav-link--active': isActive('/init') }">
              <span class="nav-link__bullet"></span>
              初始化企业
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink
              to="/accounts"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/accounts') }"
            >
              <span class="nav-link__bullet"></span>
              账户/科目
            </RouterLink>
          </li>
        </ul>

        <div class="nav-group-title">日常业务</div>
        <ul class="nav-list">
          <li class="nav-item">
            <RouterLink
              to="/transactions"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/transactions') }"
            >
              <span class="nav-link__bullet"></span>
              记账（凭证）
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink
              to="/posting"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/posting') }"
            >
              <span class="nav-link__bullet"></span>
              凭证过账
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink
              to="/contacts"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/contacts') }"
            >
              <span class="nav-link__bullet"></span>
              外部联系人
            </RouterLink>
          </li>
        </ul>

        <div class="nav-group-title">对账与报表</div>
        <ul class="nav-list">
          <li class="nav-item">
            <RouterLink
              to="/reconciliation"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/reconciliation') }"
            >
              <span class="nav-link__bullet"></span>
              银行对账
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink
              to="/reports"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/reports') }"
            >
              <span class="nav-link__bullet"></span>
              财务报表
            </RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink to="/tax" class="nav-link" :class="{ 'nav-link--active': isActive('/tax') }">
              <span class="nav-link__bullet"></span>
              税务申报
            </RouterLink>
          </li>
        </ul>

        <div class="nav-group-title">系统</div>
        <ul class="nav-list">
          <li class="nav-item">
            <RouterLink
              to="/users"
              class="nav-link"
              :class="{ 'nav-link--active': isActive('/users') }"
            >
              <span class="nav-link__bullet"></span>
              用户与权限
            </RouterLink>
          </li>
        </ul>
      </div>

      <div class="nav-footer">
        <div>课程实验 · 基于 GnuCash 思路</div>
        <div>后端 Spring Boot · 前端 Vue3</div>
      </div>
    </aside>

    <header class="app-shell__topbar">
      <div>
        <div style="font-size: 13px; color: var(--text-muted);">当前账套</div>
        <div style="font-size: 14px; font-weight: 500;">示例账套（本地 H2）</div>
      </div>
      <div class="toolbar">
        <div class="user-info" v-if="userName">
          <span class="user-name">{{ userName }}</span>
        </div>
        <button class="btn btn--ghost" @click="refreshCurrent">
          刷新
        </button>
        <button class="btn btn--ghost" @click="handleLogout" v-if="userName">
          登出
        </button>
      </div>
    </header>

    <main class="app-shell__main">
      <div class="layout-main-scroll">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.login-wrapper {
  min-height: 100vh;
}
</style>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router';
import { logout } from './api';

const route = useRoute();
const router = useRouter();

const userName = ref<string>('');

const updateUserName = () => {
  userName.value = localStorage.getItem('name') || localStorage.getItem('username') || '';
};

onMounted(() => {
  updateUserName();
});

// 监听路由变化，更新用户名显示
watch(() => route.path, () => {
  updateUserName();
});

const isActive = (path: string) => {
  return route.path === path;
};

const refreshCurrent = () => {
  router.replace({ path: '/_refresh' }).then(() => {
    router.replace(route.fullPath);
  });
};

const handleLogout = () => {
  logout();
  userName.value = '';
  router.push('/login');
};
</script>


