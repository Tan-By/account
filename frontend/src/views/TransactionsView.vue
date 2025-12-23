<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">记账（US003）</div>
        <div class="page-header__subtitle">类 GnuCash 分录表：有借必有贷，借贷必相等</div>
      </div>
      <div class="toolbar">
        <label style="font-size: 12px; color: var(--text-muted); margin-right: 8px;">显示币种：</label>
        <select v-model="displayCurrency" @change="onCurrencyChange" class="currency-select">
          <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
            {{ curr.code }} - {{ curr.name }}
          </option>
        </select>
        <button class="btn btn--primary" @click="postTransaction">保存/过账</button>
      </div>
    </div>

    <div class="card card--panel fade-in hover-lift">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">日期</label>
          <input type="date" v-model="date" />
        </div>
        <div class="form-col">
          <label class="form-label">摘要</label>
          <input v-model="description" placeholder="例如：支付办公用品" />
        </div>
      </div>

      <table class="sheet-table table-compact">
        <thead>
          <tr>
            <th>账户</th>
            <th>借方</th>
            <th>贷方</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(line, idx) in lines" :key="idx">
            <td>
              <select v-model.number="line.accountId">
                <option :value="undefined">选择账户</option>
                <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                  {{ acc.name }}（{{ acc.currencyCode }}）
                </option>
              </select>
            </td>
            <td><input type="number" v-model.number="line.debit" step="0.01" class="numeric table-input" /></td>
            <td><input type="number" v-model.number="line.credit" step="0.01" class="numeric table-input" /></td>
            <td>
              <button class="btn btn--ghost btn--small" @click="removeLine(idx)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div style="display: flex; justify-content: flex-end; margin-top: 8px; gap: 8px;">
        <span class="badge" :class="balanced ? 'badge--ok' : 'badge--error'">
          借方合计：{{ debitTotal.toFixed(2) }} ｜ 贷方合计：{{ creditTotal.toFixed(2) }}
        </span>
        <button class="btn btn--ghost" @click="addLine">添加一行</button>
      </div>
    </div>

    <div v-if="message" style="margin-top: 8px;">
      <span class="badge" :class="messageType === 'ok' ? 'badge--ok' : 'badge--error'">
        {{ message }}
      </span>
    </div>

    <!-- 记账历史 -->
    <div class="card card--panel fade-in hover-lift" style="margin-top: 24px;">
      <div class="card__section-head">
        <div>
          <div class="card-title" style="margin-bottom: 2px;">记账历史</div>
          <div class="card-subtitle" v-if="transactionHistory.length > 0">
            共 {{ transactionHistory.length }} 条记录
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <button class="btn btn--ghost" @click="loadHistory" :disabled="historyLoading">
            {{ historyLoading ? '加载中...' : '刷新' }}
          </button>
        </div>
      </div>
      
      <div v-if="historyLoading" class="loading-state subtle-ghost">
        加载中...
      </div>
      
      <div
        v-else-if="transactionHistory.length === 0"
        class="empty-hero"
        style="margin: 16px auto 8px;"
      >
        <div class="empty-hero__icon">🧾</div>
        <div class="empty-hero__title">暂无记账记录</div>
        <div class="empty-hero__subtitle">
          点击上方「保存/过账」完成第一笔记账后，这里会展示最近的历史凭证。
        </div>
      </div>
      
      <div v-else class="history-scroller">
        <div v-for="tx in transactionHistory" :key="tx.id" class="history-item">
          <div class="history-header">
            <div class="history-info">
              <span class="history-date">{{ formatDate(tx.date) }}</span>
              <span class="history-description">{{ tx.description || '（无摘要）' }}</span>
              <span class="badge" :class="getStatusBadgeClass(tx.status)">
                {{ getStatusText(tx.status) }}
              </span>
            </div>
            <span class="history-id">#{{ tx.id }}</span>
          </div>
          <table class="history-table sheet-table table-quiet table-compact">
            <thead>
              <tr>
                <th>账户</th>
                <th class="text-right">借方</th>
                <th class="text-right">贷方</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in tx.entries" :key="entry.id">
                <td>{{ entry.accountName }}（{{ entry.currencyCode }}）</td>
                <td class="text-right">
                  <span v-if="entry.debitCredit === 'DEBIT'" style="color: #1e8e3e; font-weight: 500;">
                    {{ formatAmount(convertAmount(entry.amount)) }}
                  </span>
                  <span v-else style="color: var(--text-muted);">-</span>
                </td>
                <td class="text-right">
                  <span v-if="entry.debitCredit === 'CREDIT'" style="color: #d93025; font-weight: 500;">
                    {{ formatAmount(convertAmount(entry.amount)) }}
                  </span>
                  <span v-else style="color: var(--text-muted);">-</span>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td><strong>合计</strong></td>
                <td class="text-right">
                  <strong style="color: #1e8e3e;">{{ formatAmount(getDebitTotal(tx)) }}</strong>
                </td>
                <td class="text-right">
                  <strong style="color: #d93025;">{{ formatAmount(getCreditTotal(tx)) }}</strong>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '../api';

interface Account {
  id: number;
  name: string;
  currencyCode: string;
}

interface Line {
  accountId?: number;
  debit: number;
  credit: number;
}

interface TransactionEntry {
  id: number;
  accountId: number;
  accountName: string;
  currencyCode: string;
  debitCredit: 'DEBIT' | 'CREDIT';
  amount: number | string | any; // 可能是BigDecimal对象
}

interface Transaction {
  id: number;
  date: string;
  description: string;
  status?: string;
  entries: TransactionEntry[];
}

const date = ref<string>(new Date().toISOString().slice(0, 10));
const description = ref('');
const accounts = ref<Account[]>([]);
const lines = ref<Line[]>([
  { debit: 0, credit: 0 },
  { debit: 0, credit: 0 }
]);

const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');
const transactionHistory = ref<Transaction[]>([]);
const historyLoading = ref(false);
const currencies = ref<Array<{ code: string; name: string }>>([]);
const displayCurrency = ref('CNY');

const debitTotal = computed(() =>
  lines.value.reduce((sum, l) => sum + (l.debit || 0), 0)
);
const creditTotal = computed(() =>
  lines.value.reduce((sum, l) => sum + (l.credit || 0), 0)
);
const balanced = computed(() => debitTotal.value === creditTotal.value && debitTotal.value > 0);

const loadAccounts = async () => {
  const resp = await api.get('/accounts');
  accounts.value = resp.data;
};

const addLine = () => {
  lines.value.push({ debit: 0, credit: 0 });
};

const removeLine = (idx: number) => {
  lines.value.splice(idx, 1);
};

const postTransaction = async () => {
  if (!balanced.value) {
    message.value = '借贷不平衡，无法保存。';
    messageType.value = 'error';
    return;
  }
  try {
    const entries = lines.value
      .flatMap((l) => {
        const entries: any[] = [];
        if (l.debit && l.debit > 0) {
          entries.push({
            accountId: l.accountId,
            debitCredit: 'DEBIT',
            amount: l.debit
          });
        }
        if (l.credit && l.credit > 0) {
          entries.push({
            accountId: l.accountId,
            debitCredit: 'CREDIT',
            amount: l.credit
          });
        }
        return entries;
      })
      .filter((e) => e.accountId);

    const payload = {
      date: date.value,
      description: description.value || '手工记账',
      entries
    };

    await api.post('/transactions', payload);
    message.value = '记账成功，账户余额已更新。';
    messageType.value = 'ok';
    
    // 清空表单
    description.value = '';
    lines.value = [
      { debit: 0, credit: 0 },
      { debit: 0, credit: 0 }
    ];
    
    // 刷新历史记录
    await loadHistory();
  } catch (e: any) {
    message.value = e?.response?.data?.message || '记账失败';
    messageType.value = 'error';
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

const onCurrencyChange = async () => {
  await loadHistory();
};

const loadHistory = async () => {
  historyLoading.value = true;
  try {
    const resp = await api.get('/transactions', {
      params: { displayCurrency: displayCurrency.value }
    });
    // 按日期倒序排列，最新的在前面
    transactionHistory.value = resp.data.sort((a: Transaction, b: Transaction) => {
      return new Date(b.date).getTime() - new Date(a.date).getTime();
    });
  } catch (e: any) {
    console.error('加载历史记录失败:', e);
  } finally {
    historyLoading.value = false;
  }
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit' 
  });
};

// 转换金额（处理BigDecimal可能返回为字符串或对象的情况）
const convertAmount = (amount: any): number => {
  if (typeof amount === 'number') {
    return amount;
  }
  if (typeof amount === 'string') {
    return parseFloat(amount);
  }
  // 如果后端返回的是BigDecimal对象，可能有valueOf或toString方法
  if (amount && typeof amount === 'object') {
    if (typeof amount.valueOf === 'function') {
      return parseFloat(amount.valueOf().toString());
    }
    if (typeof amount.toString === 'function') {
      return parseFloat(amount.toString());
    }
  }
  return 0;
};

const formatAmount = (amount: number) => {
  return amount.toFixed(2);
};

const getDebitTotal = (tx: Transaction) => {
  return tx.entries
    .filter(e => e.debitCredit === 'DEBIT')
    .reduce((sum, e) => sum + convertAmount(e.amount), 0);
};

const getCreditTotal = (tx: Transaction) => {
  return tx.entries
    .filter(e => e.debitCredit === 'CREDIT')
    .reduce((sum, e) => sum + convertAmount(e.amount), 0);
};

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'UNAUDITED': '未审核',
    'AUDITED': '已审核',
    'POSTED': '已过账'
  };
  return statusMap[status] || status;
};

const getStatusBadgeClass = (status: string) => {
  if (status === 'UNAUDITED') return 'badge--warning';
  if (status === 'AUDITED') return 'badge--info';
  if (status === 'POSTED') return 'badge--ok';
  return '';
};

onMounted(async () => {
  await loadCurrencies();
  await loadAccounts();
  await loadHistory();
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
  margin-right: 8px;
}

.currency-select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
}
</style>


