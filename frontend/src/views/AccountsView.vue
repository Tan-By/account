<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">科目管理</div>
        <div class="page-header__subtitle">管理树状账户结构</div>
      </div>
      <div class="toolbar">
        <input
          v-model="searchText"
          type="text"
          placeholder="搜索科目名称或编码..."
          class="search-input"
          style="width: 200px;"
        />
        <button class="btn btn--primary" @click="openAddModal">新增账户</button>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div v-if="message" style="margin-bottom: 12px;">
      <span class="badge" :class="messageType === 'ok' ? 'badge--ok' : 'badge--error'">
        {{ message }}
      </span>
    </div>

    <!-- 按类型分组的科目列表 -->
    <div v-for="(group, typeKey) in groupedAccounts" :key="typeKey" class="account-group">
      <div class="account-group__header">
        <span class="account-group__title">{{ getTypeLabel(typeKey) }}</span>
        <span class="account-group__count">({{ group.length }})</span>
      </div>
      <div class="card">
        <table class="account-table">
          <thead>
            <tr>
              <th style="width: 100px;">编码</th>
              <th>名称</th>
              <th style="width: 120px; text-align: right;">余额</th>
              <th style="width: 150px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="acc in group" :key="acc.id ?? acc._localId" :class="{ 'row-editing': isEditing(acc) }">
              <td>
                <input
                  v-if="!acc.id || isEditing(acc)"
                  v-model="acc.code"
                  class="table-input"
                  style="width: 90px;"
                  placeholder="编码"
                />
                <span v-else class="account-code">{{ acc.code }}</span>
              </td>
              <td>
                <div v-if="acc.parentId" class="account-parent-hint">
                  <span class="parent-indicator">↳</span>
                </div>
                <input
                  v-if="!acc.id || isEditing(acc)"
                  v-model="acc.name"
                  class="table-input"
                  placeholder="科目名称"
                />
                <span v-else>{{ acc.name }}</span>
                <span v-if="!isEditing(acc)" class="type-badge" :class="`type-badge--${acc.type.toLowerCase()}`" style="margin-left: 8px;">
                  {{ getTypeLabel(acc.type) }}
                </span>
              </td>
              <td style="text-align: right;">
                <span :class="getBalanceClass(acc.balance)">
                  {{ formatBalance(acc.balance) }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <button
                    v-if="!acc.id || isEditing(acc)"
                    class="btn btn--ghost btn--small"
                    @click="save(acc)"
                  >
                    保存
                  </button>
                  <button
                    v-else
                    class="btn btn--ghost btn--small"
                    @click="startEdit(acc)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="acc.id"
                    class="btn btn--ghost btn--small btn--danger"
                    @click="remove(acc)"
                  >
                    删除
                  </button>
                  <button
                    v-if="isEditing(acc)"
                    class="btn btn--ghost btn--small"
                    @click="cancelEdit(acc)"
                  >
                    取消
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredAccounts.length === 0" class="empty-state">
      暂无科目数据
    </div>

    <!-- 新增账户模态框 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">新增账户</h3>
          <button class="modal-close" @click="closeAddModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">科目名称 <span class="required">*</span></label>
            <input
              v-model="newAccount.name"
              type="text"
              class="form-input"
              placeholder="请输入科目名称"
              @keyup.enter="saveNewAccount"
            />
          </div>
          <div class="form-group">
            <label class="form-label">科目编码 <span class="required">*</span></label>
            <input
              v-model="newAccount.code"
              type="text"
              class="form-input"
              placeholder="请输入科目编码"
              @keyup.enter="saveNewAccount"
            />
          </div>
          <div class="form-group">
            <label class="form-label">科目类型 <span class="required">*</span></label>
            <select v-model="newAccount.type" class="form-input">
              <option value="ASSET">资产</option>
              <option value="LIABILITY">负债</option>
              <option value="EQUITY">权益</option>
              <option value="INCOME">收入</option>
              <option value="EXPENSE">费用</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn--ghost" @click="closeAddModal">取消</button>
          <button class="btn btn--primary" @click="saveNewAccount">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { api } from '../api';

interface AccountRow {
  id?: number;
  _localId?: number;
  name: string;
  code: string;
  type: string;
  currencyCode: string;
  balance?: number;
  parentId?: number;
  enabled?: boolean;
  _original?: AccountRow; // 用于取消编辑
  _editing?: boolean;
}

const accounts = ref<AccountRow[]>([]);
const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');
const searchText = ref('');
const showAddModal = ref(false);
const newAccount = ref({
  name: '',
  code: '',
  type: 'ASSET'
});

const load = async () => {
  try {
    const resp = await api.get('/accounts');
    accounts.value = resp.data.map((acc: any) => ({
      ...acc,
      _editing: false
    }));
    message.value = '';
  } catch (e: any) {
    message.value = '加载失败: ' + (e?.response?.data?.message || e.message);
    messageType.value = 'error';
  }
};

const filteredAccounts = computed(() => {
  if (!searchText.value.trim()) {
    return accounts.value;
  }
  const keyword = searchText.value.toLowerCase();
  return accounts.value.filter(
    (acc) =>
      acc.name?.toLowerCase().includes(keyword) ||
      acc.code?.toLowerCase().includes(keyword)
  );
});

const groupedAccounts = computed(() => {
  const groups: Record<string, AccountRow[]> = {
    ASSET: [],
    LIABILITY: [],
    EQUITY: [],
    INCOME: [],
    EXPENSE: []
  };

  filteredAccounts.value.forEach((acc) => {
    if (groups[acc.type]) {
      groups[acc.type].push(acc);
    }
  });

  // 移除空组
  Object.keys(groups).forEach((key) => {
    if (groups[key].length === 0) {
      delete groups[key];
    }
  });

  return groups;
});

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    ASSET: '资产',
    LIABILITY: '负债',
    EQUITY: '权益',
    INCOME: '收入',
    EXPENSE: '费用'
  };
  return labels[type] || type;
};

const formatBalance = (balance?: number) => {
  if (balance === undefined || balance === null) return '-';
  return balance.toFixed(2);
};

const getBalanceClass = (balance?: number) => {
  if (balance === undefined || balance === null) return '';
  if (balance > 0) return 'balance-positive';
  if (balance < 0) return 'balance-negative';
  return '';
};

const isEditing = (acc: AccountRow) => {
  return acc._editing || !acc.id;
};

const startEdit = (acc: AccountRow) => {
  acc._original = { ...acc };
  acc._editing = true;
};

const cancelEdit = (acc: AccountRow) => {
  if (acc._original) {
    Object.assign(acc, acc._original);
    delete acc._original;
  }
  acc._editing = false;
  if (!acc.id) {
    // 如果是新行，删除它
    const index = accounts.value.indexOf(acc);
    if (index > -1) {
      accounts.value.splice(index, 1);
    }
  }
};

const openAddModal = () => {
  newAccount.value = {
    name: '',
    code: '',
    type: 'ASSET'
  };
  showAddModal.value = true;
};

const closeAddModal = () => {
  showAddModal.value = false;
  newAccount.value = {
    name: '',
    code: '',
    type: 'ASSET'
  };
};

const saveNewAccount = async () => {
  if (!newAccount.value.name?.trim() || !newAccount.value.code?.trim()) {
    message.value = '请填写科目名称和编码';
    messageType.value = 'error';
    return;
  }

  try {
    const payload = {
      name: newAccount.value.name.trim(),
      code: newAccount.value.code.trim(),
      type: newAccount.value.type,
      currencyCode: 'CNY', // 默认使用CNY
      enabled: true
    };
    await api.post('/accounts', payload);
    message.value = '创建成功';
    messageType.value = 'ok';
    closeAddModal();
    await load();
    setTimeout(() => {
      message.value = '';
    }, 3000);
  } catch (e: any) {
    message.value = e?.response?.data?.message || '保存失败';
    messageType.value = 'error';
  }
};

const save = async (row: AccountRow) => {
  if (!row.name?.trim() || !row.code?.trim()) {
    message.value = '请填写科目名称和编码';
    messageType.value = 'error';
    return;
  }

  try {
    const payload = {
      name: row.name.trim(),
      code: row.code.trim(),
      type: row.type,
      currencyCode: 'CNY', // 默认使用CNY
      enabled: row.enabled !== false
    };
    if (row.id) {
      await api.put(`/accounts/${row.id}`, payload);
      message.value = '保存成功';
      messageType.value = 'ok';
    } else {
      const resp = await api.post('/accounts', payload);
      Object.assign(row, resp.data);
      message.value = '创建成功';
      messageType.value = 'ok';
    }
    row._editing = false;
    delete row._original;
    await load();
    setTimeout(() => {
      message.value = '';
    }, 3000);
  } catch (e: any) {
    message.value = e?.response?.data?.message || '保存失败';
    messageType.value = 'error';
  }
};

const remove = async (row: AccountRow) => {
  if (!row.id) return;
  if (!confirm(`确定要删除科目 "${row.name}" 吗？`)) {
    return;
  }
  try {
    await api.delete(`/accounts/${row.id}`);
    message.value = '删除成功';
    messageType.value = 'ok';
    await load();
    setTimeout(() => {
      message.value = '';
    }, 3000);
  } catch (e: any) {
    message.value = e?.response?.data?.message || '删除失败';
    messageType.value = 'error';
  }
};

onMounted(load);
</script>

<style scoped>
.search-input {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s ease-out, box-shadow 0.15s ease-out;
}

.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
}

.account-group {
  margin-bottom: 24px;
}

.account-group__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 0 4px;
}

.account-group__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.account-group__count {
  font-size: 12px;
  color: var(--text-muted);
}

.account-table {
  width: 100%;
}

.account-table th {
  background: #f8f9fa;
  font-weight: 500;
  padding: 10px 12px;
}

.account-table td {
  padding: 10px 12px;
  vertical-align: middle;
}

.row-editing {
  background: rgba(26, 115, 232, 0.04);
}

.account-code {
  font-family: 'Courier New', monospace;
  font-weight: 500;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.account-parent-hint {
  display: inline-flex;
  align-items: center;
  margin-right: 4px;
}

.parent-indicator {
  color: var(--text-muted);
  font-size: 12px;
  margin-right: 4px;
}

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

.type-badge--asset {
  background: rgba(26, 115, 232, 0.1);
  color: #1a73e8;
}

.type-badge--liability {
  background: rgba(234, 67, 53, 0.1);
  color: #ea4335;
}

.type-badge--equity {
  background: rgba(251, 188, 4, 0.1);
  color: #fbbc04;
}

.type-badge--income {
  background: rgba(52, 168, 83, 0.1);
  color: #34a853;
}

.type-badge--expense {
  background: rgba(255, 152, 0, 0.1);
  color: #ff9800;
}

.table-input {
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s ease-out;
  width: 100%;
}

.table-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
}

.balance-positive {
  color: #34a853;
  font-weight: 500;
}

.balance-negative {
  color: #ea4335;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 4px;
  align-items: center;
}

.btn--small {
  padding: 4px 10px;
  font-size: 12px;
}

.btn--danger {
  color: #ea4335;
}

.btn--danger:hover {
  background: rgba(234, 67, 53, 0.1);
}

.type-select {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s ease-out, box-shadow 0.15s ease-out;
}

/* 模态框样式 */
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
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-subtle);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-muted);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.15s ease-out, color 0.15s ease-out;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  background: rgba(60, 64, 67, 0.1);
  color: var(--text-main);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  margin-bottom: 8px;
}

.required {
  color: #ea4335;
}

.form-input {
  width: 100%;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s ease-out, box-shadow 0.15s ease-out;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-subtle);
  background: #f8f9fa;
}
</style>


