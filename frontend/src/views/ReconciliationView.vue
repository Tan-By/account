<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">银行对账（US002）</div>
        <div class="page-header__subtitle">基于金额+日期自动匹配，生成对账结果与调节余额</div>
      </div>
      <div class="toolbar">
        <label style="font-size: 12px; color: var(--text-muted); margin-right: 8px;">显示币种：</label>
        <select v-model="displayCurrency" @change="onCurrencyChange" class="currency-select">
          <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
            {{ curr.code }} - {{ curr.name }}
          </option>
        </select>
        <button class="btn btn--primary" @click="doReconcile">开始对账</button>
      </div>
    </div>

    <div class="card card--panel fade-in hover-lift">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">银行账户</label>
          <div style="display: flex; gap: 8px; align-items: center;">
            <select v-model.number="bankAccountId" style="flex: 1;">
              <option :value="undefined">选择银行账户</option>
              <option
                v-for="acc in bankAccounts"
                :key="acc.id"
                :value="acc.id"
              >
                {{ acc.name }}（{{ acc.currencyCode }}）
              </option>
            </select>
            <button class="btn btn--ghost btn--pill btn--small" @click="openAddBankAccountModal" style="white-space: nowrap;">
              + 新增账户
            </button>
          </div>
        </div>
        <div class="form-col">
          <label class="form-label">对账期间开始</label>
          <input type="date" v-model="periodStart" />
        </div>
        <div class="form-col">
          <label class="form-label">对账期间结束</label>
          <input type="date" v-model="periodEnd" />
        </div>
      </div>
    </div>

    <div v-if="message" style="margin-top: 8px; margin-bottom: 8px;">
      <span class="badge" :class="messageType === 'ok' ? 'badge--ok' : 'badge--error'">
        {{ message }}
      </span>
    </div>

    <div
      v-if="!result"
      class="empty-hero"
      style="margin-top: 16px;"
    >
      <div class="empty-hero__icon">🏦</div>
      <div class="empty-hero__title">还未开始对账</div>
      <div class="empty-hero__subtitle">
        选择银行账户与对账期间后，点击「开始对账」生成自动匹配结果。
      </div>
      <button class="btn btn--primary btn--pill empty-hero__action" @click="doReconcile">
        开始对账
      </button>
    </div>

    <div v-else class="card card--panel fade-in hover-lift" style="margin-top: 10px;">
      <div class="card-title">对账结果摘要（任务ID：{{ result.taskId }}）</div>
      <table class="sheet-table table-compact table-quiet">
        <tbody>
          <tr>
            <td>已匹配记录数</td>
            <td>{{ result.matchedCount }}</td>
          </tr>
          <tr>
            <td>银行未匹配记录数</td>
            <td>{{ result.unmatchedBankCount }}</td>
          </tr>
          <tr>
            <td>账面未匹配记录数</td>
            <td>{{ result.unmatchedBookCount }}</td>
          </tr>
          <tr>
            <td>账面余额</td>
            <td>{{ result.bookBalance?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>银行余额</td>
            <td>{{ result.bankBalance?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>调节后账面余额</td>
            <td>{{ result.adjustedBookBalance?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>调节后银行余额</td>
            <td>{{ result.adjustedBankBalance?.toFixed?.(2) ?? '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 银行流水明细 -->
    <div v-if="result?.bankRecords?.length" class="card card--panel fade-in" style="margin-top: 10px;">
      <div class="card-title">银行流水明细</div>
      <table class="sheet-table table-compact table-quiet">
        <thead>
          <tr>
            <th style="width: 110px;">日期</th>
            <th>摘要</th>
            <th style="width: 110px;" class="text-right">借方</th>
            <th style="width: 110px;" class="text-right">贷方</th>
            <th style="width: 110px;" class="text-right">余额</th>
            <th style="width: 100px;">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in result.bankRecords" :key="row.id">
            <td>{{ formatDate(row.date) }}</td>
            <td>{{ row.description || '-' }}</td>
            <td class="text-right">{{ formatAmount(row.debitAmount) }}</td>
            <td class="text-right">{{ formatAmount(row.creditAmount) }}</td>
            <td class="text-right">{{ formatAmount(row.balance) }}</td>
            <td>
              <span class="badge" :class="row.matchStatus === '已匹配' ? 'badge--ok' : 'badge--warn'">
                {{ row.matchStatus || '未匹配' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 账面分录明细 -->
    <div v-if="result?.bookEntries?.length" class="card card--panel fade-in" style="margin-top: 10px;">
      <div class="card-title">账面分录明细</div>
      <table class="sheet-table table-compact table-quiet">
        <thead>
          <tr>
            <th style="width: 110px;">日期</th>
            <th>摘要</th>
            <th style="width: 90px;">借/贷</th>
            <th style="width: 120px;" class="text-right">金额</th>
            <th style="width: 110px;">凭证状态</th>
            <th style="width: 100px;">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in result.bookEntries" :key="row.id">
            <td>{{ formatDate(row.date) }}</td>
            <td>{{ row.description || '-' }}</td>
            <td>{{ row.debitCredit === 'DEBIT' ? '借' : '贷' }}</td>
            <td class="text-right">{{ formatAmount(row.amount) }}</td>
            <td>{{ row.voucherStatus || '-' }}</td>
            <td>
              <span class="badge" :class="row.matchStatus === '已匹配' ? 'badge--ok' : 'badge--warn'">
                {{ row.matchStatus || '未匹配' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增银行账户模态框 -->
    <div v-if="showAddBankAccountModal" class="modal-overlay" @click.self="closeAddBankAccountModal">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <h3 class="modal-title">新增银行账户</h3>
          <button class="modal-close" @click="closeAddBankAccountModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">账户名称 <span class="required">*</span></label>
            <input
              v-model="newBankAccount.name"
              type="text"
              class="form-input"
              placeholder="例如：中国银行-基本户"
              @keyup.enter="saveNewBankAccount"
            />
          </div>
          <div class="form-group">
            <label class="form-label">账户编码 <span class="required">*</span></label>
            <input
              v-model="newBankAccount.code"
              type="text"
              class="form-input"
              placeholder="例如：100201"
              @keyup.enter="saveNewBankAccount"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn--ghost" @click="closeAddBankAccountModal">取消</button>
          <button class="btn btn--primary" @click="saveNewBankAccount">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';

interface Account {
  id: number;
  name: string;
  currencyCode: string;
}

const bankAccounts = ref<Account[]>([]);
const bankAccountId = ref<number | undefined>();
const periodStart = ref<string>(new Date().toISOString().slice(0, 10));
const periodEnd = ref<string>(new Date().toISOString().slice(0, 10));
const currencies = ref<Array<{ code: string; name: string }>>([]);
const displayCurrency = ref('CNY');
const showAddBankAccountModal = ref(false);
const newBankAccount = ref({
  name: '',
  code: ''
});
const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');

const result = ref<any>(null);

const formatAmount = (amount?: number) => {
  if (amount === undefined || amount === null) return '-';
  return Number(amount).toFixed(2);
};

const formatDate = (date?: string) => {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN');
};

const loadBankAccounts = async () => {
  const resp = await api.get('/accounts');
  // 筛选真正的银行账户：名称包含"银行"或"存款"的资产账户
  bankAccounts.value = resp.data.filter((a: any) => {
    if (a.type !== 'ASSET' || !a.enabled) {
      return false;
    }
    const name = (a.name || '').toLowerCase();
    return name.includes('银行') || name.includes('存款') || name.includes('bank');
  });
  if (!bankAccountId.value && bankAccounts.value.length > 0) {
    bankAccountId.value = bankAccounts.value[0].id;
  }
};

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
  // 如果已有结果，重新加载以转换币种
  if (result.value) {
    doReconcile();
  }
};

const openAddBankAccountModal = () => {
  newBankAccount.value = {
    name: '',
    code: ''
  };
  showAddBankAccountModal.value = true;
};

const closeAddBankAccountModal = () => {
  showAddBankAccountModal.value = false;
  newBankAccount.value = {
    name: '',
    code: ''
  };
  message.value = '';
};

const saveNewBankAccount = async () => {
  if (!newBankAccount.value.name?.trim() || !newBankAccount.value.code?.trim()) {
    message.value = '请填写账户名称和编码';
    messageType.value = 'error';
    return;
  }

  try {
    const payload = {
      name: newBankAccount.value.name.trim(),
      code: newBankAccount.value.code.trim(),
      type: 'ASSET', // 银行账户是资产类型
      currencyCode: 'CNY', // 默认使用CNY
      enabled: true
    };
    const resp = await api.post('/accounts', payload);
    message.value = '创建成功';
    messageType.value = 'ok';
    closeAddBankAccountModal();
    await loadBankAccounts();
    // 自动选择新创建的账户
    bankAccountId.value = resp.data.id;
    setTimeout(() => {
      message.value = '';
    }, 3000);
  } catch (e: any) {
    message.value = e?.response?.data?.message || '保存失败';
    messageType.value = 'error';
  }
};

const doReconcile = async () => {
  if (!bankAccountId.value) return;
  const payload = {
    bankAccountId: bankAccountId.value,
    periodStart: periodStart.value,
    periodEnd: periodEnd.value,
    executor: 'demo-user',
    displayCurrency: displayCurrency.value
  };
  const resp = await api.post('/reconciliations', payload);
  result.value = resp.data;
};

onMounted(async () => {
  await loadCurrencies();
  await loadBankAccounts();
});
</script>

<style scoped>
</style>


