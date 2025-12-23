<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">税务申报（US001）</div>
        <div class="page-header__subtitle">从系统数据生成申报草稿，模拟提交与状态流转</div>
      </div>
      <div class="toolbar">
        <label style="font-size: 12px; color: var(--text-muted); margin-right: 8px;">显示币种：</label>
        <select v-model="displayCurrency" @change="onCurrencyChange" class="currency-select">
          <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
            {{ curr.code }} - {{ curr.name }}
          </option>
        </select>
        <button class="btn btn--primary" @click="createDraft">生成申报草稿</button>
      </div>
    </div>

    <div class="card card--panel fade-in hover-lift">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">税种</label>
          <select v-model="taxType">
            <option value="增值税">增值税</option>
            <option value="企业所得税">企业所得税</option>
          </select>
        </div>
        <div class="form-col">
          <label class="form-label">申报期开始</label>
          <input type="date" v-model="periodStart" />
        </div>
        <div class="form-col">
          <label class="form-label">申报期结束</label>
          <input type="date" v-model="periodEnd" />
        </div>
      </div>
    </div>

    <div
      v-if="!current"
      class="empty-hero"
      style="margin-top: 16px;"
    >
      <div class="empty-hero__icon">🧾</div>
      <div class="empty-hero__title">尚未生成申报草稿</div>
      <div class="empty-hero__subtitle">
        选择税种与申报期间后，点击「生成申报草稿」从系统数据自动汇总。
      </div>
      <button class="btn btn--primary btn--pill empty-hero__action" @click="createDraft">
        生成申报草稿
      </button>
    </div>

    <div v-else class="card card--panel fade-in hover-lift" style="margin-top: 10px;">
      <div class="card-title">申报表草稿（ID：{{ current.id }}）</div>
      <div class="card-subtitle">状态：{{ current.status }}</div>
      <table class="sheet-table table-compact table-quiet">
        <tbody>
          <tr>
            <td style="width: 200px;">计税依据</td>
            <td>{{ current.taxableAmount?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>应纳税额</td>
            <td>{{ current.taxPayable?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>减免税额</td>
            <td>{{ current.taxRelief?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>已缴税额</td>
            <td>{{ current.taxPaid?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>应补(退)税额</td>
            <td>{{ current.taxDue?.toFixed?.(2) ?? '-' }}</td>
          </tr>
        </tbody>
      </table>

      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary btn--pill" @click="submit" :disabled="current.status !== '草稿'">
          提交申报
        </button>
        <button
          class="btn btn--ghost btn--pill"
          @click="markSuccess"
          :disabled="current.status !== '已提交'"
        >
          模拟税局确认成功
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';

const taxType = ref('增值税');
const today = new Date().toISOString().slice(0, 10);
const periodStart = ref(today);
const periodEnd = ref(today);
const currencies = ref<Array<{ code: string; name: string }>>([]);
const displayCurrency = ref('CNY');

const current = ref<any | null>(null);

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
  } catch (e: any) {
    console.error('加载币种失败:', e);
  }
};

const onCurrencyChange = () => {
  // 如果已有申报草稿，重新生成以转换币种
  if (current.value) {
    createDraft();
  }
};

const createDraft = async () => {
  const payload = {
    taxType: taxType.value,
    periodStart: periodStart.value,
    periodEnd: periodEnd.value,
    displayCurrency: displayCurrency.value
  };
  const resp = await api.post('/tax-declarations/draft', payload);
  current.value = resp.data;
};

const submit = async () => {
  if (!current.value) return;
  const resp = await api.post(`/tax-declarations/${current.value.id}/submit`);
  current.value = resp.data;
};

const markSuccess = async () => {
  if (!current.value) return;
  const resp = await api.post(`/tax-declarations/${current.value.id}/success`);
  current.value = resp.data;
};

onMounted(loadCurrencies);
</script>

<style scoped>
</style>


