<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">凭证过账（UC008）</div>
        <div class="page-header__subtitle">审核凭证并将已审核的凭证过账到总账和明细账</div>
      </div>
      <div class="toolbar">
        <button 
          v-if="activeTab === 'audited'"
          class="btn btn--primary btn--pill" 
          @click="executePosting" 
          :disabled="selectedVouchers.length === 0 || posting"
        >
          {{ posting ? '过账中...' : '执行过账' }}
        </button>
        <button 
          v-if="activeTab === 'unaudited'"
          class="btn btn--primary btn--pill" 
          @click="executeAudit" 
          :disabled="selectedVouchers.length === 0 || auditing"
        >
          {{ auditing ? '审核中...' : '批量审核' }}
        </button>
        <button class="btn btn--ghost" @click="loadVouchers" :disabled="loading">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="message" style="margin-top: 8px; margin-bottom: 16px;">
      <span class="badge" :class="messageType === 'ok' ? 'badge--ok' : 'badge--error'">
        {{ message }}
      </span>
    </div>

    <!-- 标签页 -->
    <div class="pill-switch" style="margin-bottom: 16px;">
      <button 
        class="pill-switch__btn"
        :class="{ 'is-active': activeTab === 'unaudited' }"
        @click="activeTab = 'unaudited'"
      >
        未审核凭证 ({{ unauditedVouchers.length }})
      </button>
      <button 
        class="pill-switch__btn"
        :class="{ 'is-active': activeTab === 'audited' }"
        @click="activeTab = 'audited'"
      >
        已审核凭证 ({{ pendingVouchers.length }})
      </button>
    </div>

    <div class="card card--panel fade-in hover-lift">
      <div class="card__section-head">
        <h3 style="margin: 0; font-size: 16px; font-weight: 600;">
          <span v-if="activeTab === 'unaudited'">待审核凭证列表（状态：未审核）</span>
          <span v-else>待过账凭证列表（状态：已审核）</span>
        </h3>
        <span v-if="currentVouchers.length > 0" class="card-subtitle">
          共 {{ currentVouchers.length }} 张凭证
        </span>
      </div>

      <div v-if="loading" class="loading-state">
        加载中...
      </div>

      <div
        v-else-if="currentVouchers.length === 0"
        class="empty-hero"
        style="margin: 16px auto 8px;"
      >
        <div class="empty-hero__icon">📑</div>
        <div class="empty-hero__title">
          <span v-if="activeTab === 'unaudited'">暂无待审核凭证</span>
          <span v-else>暂无待过账凭证</span>
        </div>
        <div class="empty-hero__subtitle">
          先在「记账」中录入业务凭证，并完成审核后，这里会显示需要过账的项目。
        </div>
      </div>

      <div v-else>
        <table class="sheet-table table-compact table-quiet">
          <thead>
            <tr>
              <th style="width: 40px;">
                <input 
                  type="checkbox" 
                  :checked="allSelected" 
                  @change="toggleSelectAll"
                />
              </th>
              <th>凭证号</th>
              <th>日期</th>
              <th>摘要</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="voucher in currentVouchers" :key="voucher.id">
              <td>
                <input 
                  type="checkbox" 
                  :value="voucher.id" 
                  v-model="selectedVouchers"
                />
              </td>
              <td>#{{ voucher.id }}</td>
              <td>{{ formatDate(voucher.date) }}</td>
              <td>{{ voucher.description || '（无摘要）' }}</td>
              <td>
                <span class="badge" :class="getStatusBadgeClass(voucher.status)">
                  {{ getStatusText(voucher.status) }}
                </span>
              </td>
              <td>
                <button 
                  class="btn btn--ghost btn--pill btn--small" 
                  @click="viewVoucherDetail(voucher)"
                >
                  查看详情
                </button>
                <button 
                  v-if="activeTab === 'unaudited' && voucher.status === 'UNAUDITED'"
                  class="btn btn--ghost btn--pill btn--small" 
                  @click="auditSingleVoucher(voucher.id)"
                >
                  审核
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="selectedVouchers.length > 0" class="card--ghost hover-lift" style="margin-top: 16px; padding: 12px; border-radius: 10px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span class="card-subtitle" style="font-size: 13px;">
              已选择 {{ selectedVouchers.length }} 张凭证
            </span>
            <button 
              class="btn btn--ghost btn--pill btn--small" 
              @click="selectedVouchers = []"
            >
              清空选择
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 凭证详情对话框 -->
    <div v-if="selectedVoucherDetail" class="modal-overlay" @click="selectedVoucherDetail = null">
      <div class="modal-content modal-content--md pop-in" @click.stop>
        <div class="card__section-head" style="margin-bottom: 8px;">
          <h3 style="margin: 0; font-size: 18px; font-weight: 600;">凭证详情 #{{ selectedVoucherDetail.id }}</h3>
          <button class="btn btn--ghost btn--pill btn--small" @click="selectedVoucherDetail = null" style="font-size: 14px; padding: 4px 10px;">关闭</button>
        </div>
        <div class="card-subtitle" style="margin-bottom: 12px;">
          <strong>日期：</strong>{{ formatDate(selectedVoucherDetail.date) }}
        </div>
        <div class="card-subtitle" style="margin-bottom: 12px;">
          <strong>摘要：</strong>{{ selectedVoucherDetail.description || '（无摘要）' }}
        </div>
        <div class="card-subtitle" style="margin-bottom: 12px;">
          <strong>状态：</strong>
          <span class="badge badge--info">{{ getStatusText(selectedVoucherDetail.status) }}</span>
        </div>
        <div v-if="selectedVoucherDetail.entries && selectedVoucherDetail.entries.length > 0">
          <strong style="display: block; margin-bottom: 8px;">分录：</strong>
          <table class="sheet-table table-compact table-quiet">
            <thead>
              <tr>
                <th>账户</th>
                <th class="text-right">借方</th>
                <th class="text-right">贷方</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in selectedVoucherDetail.entries" :key="entry.id">
                <td>{{ entry.accountName }}（{{ entry.currencyCode }}）</td>
                <td class="text-right numeric">
                  <span v-if="entry.debitCredit === 'DEBIT'" class="text-success" style="font-weight: 600;">
                    {{ formatAmount(convertAmount(entry.amount)) }}
                  </span>
                  <span v-else style="color: var(--text-muted);">-</span>
                </td>
                <td class="text-right numeric">
                  <span v-if="entry.debitCredit === 'CREDIT'" class="text-danger" style="font-weight: 600;">
                    {{ formatAmount(convertAmount(entry.amount)) }}
                  </span>
                  <span v-else style="color: var(--text-muted);">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '../api';

interface TransactionEntry {
  id: number;
  accountId: number;
  accountName: string;
  currencyCode: string;
  debitCredit: 'DEBIT' | 'CREDIT';
  amount: number | string | any;
}

interface Transaction {
  id: number;
  date: string;
  description: string;
  status: string;
  entries?: TransactionEntry[];
}

const pendingVouchers = ref<Transaction[]>([]);
const unauditedVouchers = ref<Transaction[]>([]);
const selectedVouchers = ref<number[]>([]);
const loading = ref(false);
const posting = ref(false);
const auditing = ref(false);
const activeTab = ref<'unaudited' | 'audited'>('unaudited');
const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');
const selectedVoucherDetail = ref<Transaction | null>(null);

const currentVouchers = computed(() => {
  return activeTab.value === 'unaudited' ? unauditedVouchers.value : pendingVouchers.value;
});

const allSelected = computed(() => {
  return currentVouchers.value.length > 0 && 
         selectedVouchers.value.length === currentVouchers.value.length;
});

const loadVouchers = async () => {
  loading.value = true;
  message.value = '';
  try {
    // 加载已审核凭证（待过账）
    const respPending = await api.get('/posting/pending');
    pendingVouchers.value = respPending.data;
    
    // 加载未审核凭证
    const respAll = await api.get('/transactions');
    unauditedVouchers.value = respAll.data.filter((t: Transaction) => t.status === 'UNAUDITED');
    
    selectedVouchers.value = [];
  } catch (e: any) {
    message.value = e?.response?.data?.message || '加载凭证失败';
    messageType.value = 'error';
  } finally {
    loading.value = false;
  }
};

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedVouchers.value = [];
  } else {
    selectedVouchers.value = currentVouchers.value.map(v => v.id);
  }
};

const executePosting = async () => {
  if (selectedVouchers.value.length === 0) {
    message.value = '请选择要过账的凭证';
    messageType.value = 'error';
    return;
  }

  posting.value = true;
  message.value = '';
  try {
    const resp = await api.post('/posting/execute', {
      voucherIds: selectedVouchers.value
    });
    
    if (resp.data.successCount > 0) {
      message.value = `成功过账 ${resp.data.successCount} 张凭证`;
      messageType.value = 'ok';
      // 刷新列表
      await loadVouchers();
    } else {
      message.value = resp.data.message || '过账失败';
      messageType.value = 'error';
    }
  } catch (e: any) {
    message.value = e?.response?.data?.message || '过账失败';
    messageType.value = 'error';
  } finally {
    posting.value = false;
  }
};

const viewVoucherDetail = async (voucher: Transaction) => {
  try {
    // 获取完整的凭证信息（包含分录）
    const resp = await api.get('/transactions');
    const allTransactions = resp.data;
    const fullVoucher = allTransactions.find((t: Transaction) => t.id === voucher.id);
    if (fullVoucher && fullVoucher.entries) {
      selectedVoucherDetail.value = fullVoucher;
    } else {
      // 如果没有找到完整信息，使用当前凭证信息
      selectedVoucherDetail.value = voucher;
    }
  } catch (e: any) {
    console.error('加载凭证详情失败:', e);
    selectedVoucherDetail.value = voucher;
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

const auditSingleVoucher = async (id: number) => {
  try {
    await api.post(`/transactions/${id}/audit`);
    message.value = '凭证审核成功';
    messageType.value = 'ok';
    await loadVouchers();
  } catch (e: any) {
    message.value = e?.response?.data?.message || '审核失败';
    messageType.value = 'error';
  }
};

const executeAudit = async () => {
  if (selectedVouchers.value.length === 0) {
    message.value = '请选择要审核的凭证';
    messageType.value = 'error';
    return;
  }

  auditing.value = true;
  message.value = '';
  try {
    let successCount = 0;
    let failCount = 0;
    
    for (const id of selectedVouchers.value) {
      try {
        await api.post(`/transactions/${id}/audit`);
        successCount++;
      } catch (e: any) {
        failCount++;
      }
    }
    
    if (successCount > 0) {
      message.value = `成功审核 ${successCount} 张凭证${failCount > 0 ? `，${failCount} 张失败` : ''}`;
      messageType.value = failCount > 0 ? 'error' : 'ok';
      await loadVouchers();
    } else {
      message.value = '审核失败';
      messageType.value = 'error';
    }
  } catch (e: any) {
    message.value = e?.response?.data?.message || '审核失败';
    messageType.value = 'error';
  } finally {
    auditing.value = false;
  }
};

const convertAmount = (amount: any): number => {
  if (typeof amount === 'number') {
    return amount;
  }
  if (typeof amount === 'string') {
    return parseFloat(amount);
  }
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

onMounted(async () => {
  await loadVouchers();
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--bg-surface);
  border-radius: 12px;
  padding: 24px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.badge--info {
  background: #1a73e8;
  color: white;
}

.tab-button {
  background: transparent;
  border: none;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--text-muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-button:hover {
  color: var(--text-main);
}

.tab-button--active {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 500;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-muted);
  font-size: 14px;
}
</style>

