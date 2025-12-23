<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">初始化企业（US007）</div>
        <div class="page-header__subtitle">设置账套信息与期初余额，校验“资产 = 负债 + 权益”</div>
      </div>
      <div class="toolbar">
        <button
          class="btn btn--primary"
          @click="doInit"
          :disabled="!canInit || initializing"
        >
          {{ initializing ? '初始化中...' : '完成初始化' }}
        </button>
      </div>
    </div>

    <div class="init-grid">
      <div class="card">
        <div class="card-title">账套信息</div>
        <div class="form-row">
          <div class="form-col">
            <label class="form-label">企业名称</label>
            <input v-model="companyName" placeholder="例如：XX科技有限公司" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-col">
            <label class="form-label">默认币种</label>
            <select v-model="currencyCode">
              <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
                {{ curr.code }} - {{ curr.name }}
              </option>
            </select>
          </div>
          <div class="form-col">
            <label class="form-label">启用日期</label>
            <input type="date" v-model="startDate" />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">期初科目与余额</div>
        <div class="card-subtitle">
          请确保：资产合计 = 负债合计 + 权益合计，且收入/费用类科目期初余额为 0
        </div>
        <div class="init-table-wrap">
          <table>
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>类别</th>
                <th>币种</th>
                <th>期初余额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in rows" :key="idx">
                <td><input v-model="row.code" style="width: 80px" /></td>
                <td><input v-model="row.name" /></td>
                <td>
                  <select v-model="row.type">
                    <option value="ASSET">资产</option>
                    <option value="LIABILITY">负债</option>
                    <option value="EQUITY">权益</option>
                    <option value="INCOME">收入</option>
                    <option value="EXPENSE">费用</option>
                  </select>
                </td>
                <td>
                  <select v-model="row.currencyCode" style="width: 100px">
                    <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
                      {{ curr.code }}
                    </option>
                  </select>
                </td>
                <td>
                  <input
                    v-model.number="row.initialBalance"
                    type="number"
                    step="0.01"
                    style="width: 90px; margin-right: 4px;"
                  />
                  <button class="btn btn--ghost btn--small" @click="removeRow(idx)">删行</button>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="4"><strong>资产合计</strong></td>
                <td>{{ assetTotal.toFixed(2) }}</td>
              </tr>
              <tr>
                <td colspan="4"><strong>负债合计</strong></td>
                <td>{{ liabilityTotal.toFixed(2) }}</td>
              </tr>
              <tr>
                <td colspan="4"><strong>权益合计</strong></td>
                <td>{{ equityTotal.toFixed(2) }}</td>
              </tr>
              <tr>
                <td colspan="4">
                  <strong>差额（资产 − 负债 − 权益）</strong>
                </td>
                <td :style="{ color: balanceDiff === 0 ? '#1e8e3e' : '#d93025', fontWeight: 600 }">
                  {{ balanceDiff.toFixed(2) }}
                </td>
              </tr>
              <tr>
                <td colspan="5" style="text-align: right; padding-top: 8px;">
                  <button class="btn btn--ghost btn--small" @click="addRow">＋ 添加科目行</button>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>

    <div v-if="message" style="margin-top: 8px;">
      <span class="badge" :class="messageType === 'ok' ? 'badge--ok' : 'badge--error'">
        {{ message }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '../api';

interface InitRow {
  code: string;
  name: string;
  type: string;
  currencyCode: string;
  initialBalance: number;
}

const companyName = ref('示例企业');
const currencyCode = ref('CNY');
const startDate = ref<string>(new Date().toISOString().slice(0, 10));
const currencies = ref<Array<{ code: string; name: string }>>([]);

const rows = ref<InitRow[]>([
  { code: '1001', name: '现金', type: 'ASSET', currencyCode: 'CNY', initialBalance: 1000 },
  { code: '3001', name: '实收资本', type: 'EQUITY', currencyCode: 'CNY', initialBalance: 1000 }
]);

const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');
const initializing = ref(false);

const assetTotal = computed(() =>
  rows.value
    .filter(r => r.type === 'ASSET')
    .reduce((sum, r) => sum + Number(r.initialBalance || 0), 0)
);

const liabilityTotal = computed(() =>
  rows.value
    .filter(r => r.type === 'LIABILITY')
    .reduce((sum, r) => sum + Number(r.initialBalance || 0), 0)
);

const equityTotal = computed(() =>
  rows.value
    .filter(r => r.type === 'EQUITY')
    .reduce((sum, r) => sum + Number(r.initialBalance || 0), 0)
);

const balanceDiff = computed(() =>
  Number((assetTotal.value - liabilityTotal.value - equityTotal.value).toFixed(2))
);

const hasNonZeroPl = computed(() =>
  rows.value.some(r =>
    (r.type === 'INCOME' || r.type === 'EXPENSE') &&
    Number(r.initialBalance || 0) !== 0
  )
);

const canInit = computed(() => balanceDiff.value === 0 && !hasNonZeroPl.value);

const addRow = () => {
  rows.value.push({
    code: '',
    name: '',
    type: 'ASSET',
    currencyCode: currencyCode.value,
    initialBalance: 0
  });
};

const removeRow = (idx: number) => {
  if (rows.value.length <= 1) return;
  rows.value.splice(idx, 1);
};

const loadCurrencies = async () => {
  try {
    const resp = await api.get('/currencies');
    currencies.value = resp.data.map((curr: any) => ({
      code: curr.code,
      name: curr.name
    }));
    if (currencies.value.length > 0 && !currencies.value.find(c => c.code === currencyCode.value)) {
      currencyCode.value = currencies.value[0].code;
    }
  } catch (e) {
    console.error('加载币种失败:', e);
  }
};

onMounted(loadCurrencies);

const doInit = async () => {
  if (!canInit.value) {
    message.value = hasNonZeroPl.value
      ? '损益类科目期初余额必须为 0，请修改后再试。'
      : '期初不平衡：请调整使资产 = 负债 + 权益。';
    messageType.value = 'error';
    return;
  }

  try {
    initializing.value = true;
    const payload = {
      companyName: companyName.value,
      defaultCurrencyCode: currencyCode.value,
      startDate: startDate.value,
      accounts: rows.value
    };
    await api.post('/init', payload);
    message.value = '初始化成功，期初余额已平衡。';
    messageType.value = 'ok';
  } catch (e: any) {
    message.value = e?.response?.data?.message || '初始化失败，请检查输入和期初平衡。';
    messageType.value = 'error';
  } finally {
    initializing.value = false;
  }
};
</script>


