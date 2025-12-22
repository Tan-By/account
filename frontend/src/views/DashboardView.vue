<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">系统总览</div>
        <div class="page-header__subtitle">快速查看 7 个核心用例的当前状态</div>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px;">
      <div class="card">
        <div class="card-title">初始化企业</div>
        <div class="card-subtitle">账套信息与期初余额</div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span class="badge" :class="initStatus ? 'badge--ok' : 'badge--warn'">
            {{ initStatus ? '已初始化' : '未初始化' }}
          </span>
          <button class="btn btn--ghost" @click="$router.push('/init')">进入</button>
        </div>
      </div>

      <div class="card">
        <div class="card-title">记账 / 科目</div>
        <div class="card-subtitle">账户树 + 复式分录</div>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span class="badge">账户数：{{ accountCount }}</span>
          <button class="btn btn--ghost" @click="$router.push('/transactions')">进入</button>
        </div>
      </div>

      <div class="card">
        <div class="card-title">对账与报表</div>
        <div class="card-subtitle">银行对账 · 报表平衡校验</div>
        <div style="display: flex; gap: 6px;">
          <button class="btn btn--ghost" @click="$router.push('/reconciliation')">对账</button>
          <button class="btn btn--ghost" @click="$router.push('/reports')">报表</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';

const initStatus = ref(false);
const accountCount = ref(0);

onMounted(async () => {
  try {
    const accountsResp = await api.get('/accounts');
    accountCount.value = accountsResp.data.length;
    initStatus.value = accountCount.value > 0;
  } catch (e) {
    initStatus.value = false;
  }
});
</script>


