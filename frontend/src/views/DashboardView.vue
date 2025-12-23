<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">系统总览</div>
        <div class="page-header__subtitle">快速查看 11 个核心用例的当前状态</div>
      </div>
      <div class="toolbar">
        <label style="font-size: 12px; color: var(--text-muted); margin-right: 8px;">显示币种：</label>
        <select v-model="displayCurrency" @change="onCurrencyChange" class="currency-select">
          <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
            {{ curr.code }} - {{ curr.name }}
          </option>
        </select>
      </div>
    </div>

    <div class="card-grid">
      <div class="card card--panel fade-in hover-lift">
        <div class="card-title">初始化企业</div>
        <div class="card-subtitle">账套信息与期初余额</div>
        <div class="card__section-head">
          <span class="badge" :class="initStatus ? 'badge--ok' : 'badge--warn'">
            {{ initStatus ? '已初始化' : '未初始化' }}
          </span>
          <button class="btn btn--ghost btn--pill btn--small" @click="$router.push('/init')">进入</button>
        </div>
      </div>

      <div class="card card--panel fade-in hover-lift">
        <div class="card-title">记账 / 科目</div>
        <div class="card-subtitle">账户树 + 复式分录</div>
        <div class="card__section-head">
          <span class="badge">账户数：{{ accountCount }}</span>
          <button class="btn btn--ghost btn--pill btn--small" @click="$router.push('/transactions')">进入</button>
        </div>
      </div>

      <div class="card card--panel fade-in hover-lift">
        <div class="card-title">对账与报表</div>
        <div class="card-subtitle">银行对账 · 报表平衡校验</div>
        <div class="card__section-head" style="gap: 6px;">
          <button class="btn btn--ghost btn--pill btn--small" @click="$router.push('/reconciliation')">对账</button>
          <button class="btn btn--ghost btn--pill btn--small" @click="$router.push('/reports')">报表</button>
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
const currencies = ref<Array<{ code: string; name: string }>>([]);
const displayCurrency = ref('CNY');

const loadCurrencies = async () => {
  try {
    const resp = await api.get('/currencies');
    currencies.value = resp.data.map((curr: any) => ({
      code: curr.code,
      name: curr.name
    }));
    if (currencies.value.length > 0 && !currencies.value.find(c => c.code === displayCurrency.value)) {
      displayCurrency.value = currencies.value[0].code;
    }
  } catch (e) {
    console.error('加载币种失败:', e);
  }
};

const onCurrencyChange = async () => {
  await loadAccounts();
};

const loadAccounts = async () => {
  try {
    const accountsResp = await api.get('/accounts', {
      params: { displayCurrency: displayCurrency.value }
    });
    accountCount.value = accountsResp.data.length;
    initStatus.value = accountCount.value > 0;
  } catch (e) {
    initStatus.value = false;
  }
};

onMounted(async () => {
  await loadCurrencies();
  await loadAccounts();
});
</script>

<style scoped>
.currency-select {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s ease-out, box-shadow 0.15s ease-out;
}

.currency-select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
}
</style>


