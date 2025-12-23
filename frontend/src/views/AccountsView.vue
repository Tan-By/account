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
    <div v-for="group in groupedAccounts" :key="group.typeKey" class="account-group fade-in">
      <div class="account-group__header">
        <span class="account-group__title">{{ getTypeLabel(group.typeKey) }}</span>
        <span class="account-group__count">({{ countAccounts(group.rows) }})</span>
      </div>
      <div class="card card--panel">
        <table class="account-table sheet-table table-compact">
          <thead>
            <tr>
              <th style="width: 100px;">编码</th>
              <th>名称</th>
              <th style="width: 120px; text-align: right;">余额</th>
              <th style="width: 220px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="acc in group.rows"
              :key="acc.id ?? acc._localId"
            >
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
                <div
                  class="account-name"
                  :class="{ 'account-name--child': (acc as any).level > 0 }"
                  :style="{ paddingLeft: `${((acc as any).level || 0) * 18}px` }"
                >
                  <div v-if="(acc as any).level > 0" class="account-parent-hint">
                    <span class="tree-line"></span>
                    <span class="parent-indicator">└</span>
                  </div>
                  <div class="account-name__text">
                    <span>{{ acc.name }}</span>
                    <span
                      class="type-badge"
                      :class="`type-badge--${(acc.type || '').toLowerCase()}`"
                    >
                      {{ getTypeLabel(acc.type) }}
                    </span>
                  </div>
                </div>
              </td>
              <td style="text-align: right;">
                <span :class="getBalanceClass(acc.balance)">
                  {{ formatBalance(acc.balance) }}
                </span>
              </td>
              <td>
                <div class="action-buttons" style="flex-wrap: wrap; gap: 6px;">
                  <div style="display: flex; gap: 4px;">
                  <button
                    class="btn btn--ghost btn--small btn--pill"
                      @click="openEditModal(acc)"
                  >
                    编辑
                  </button>
                    <button
                      v-if="acc.id"
                      class="btn btn--ghost btn--small btn--pill"
                      @click="openAddSubAccount(acc)"
                      style="color: var(--accent);"
                    >
                      添加子科目
                    </button>
                  <button
                    v-if="acc.id"
                    class="btn btn--danger btn--small btn--pill"
                    @click="remove(acc)"
                  >
                    删除
                  </button>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 兜底：如果分组渲染为空但确实有数据，则用简易表格直接展示，避免空白 -->
    <div v-if="groupedAccounts.length === 0 && accounts.length > 0" class="card card--panel fade-in">
      <div class="card-title">科目列表（未分组兜底展示）</div>
      <table class="account-table sheet-table table-compact">
        <thead>
          <tr>
            <th style="width: 100px;">编码</th>
            <th>名称</th>
            <th style="width: 120px;">类型</th>
            <th style="width: 120px; text-align: right;">余额</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="acc in accounts" :key="acc.id ?? acc._localId">
            <td>{{ acc.code }}</td>
            <td>{{ acc.name }}</td>
            <td>{{ getTypeLabel(acc.type) }}</td>
            <td class="text-right">{{ formatBalance(acc.balance) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 空状态 -->
    <div
      v-if="filteredAccounts.length === 0"
      class="empty-hero"
      style="margin-top: 16px;"
    >
      <div class="empty-hero__icon">📚</div>
      <div class="empty-hero__title">暂无科目数据</div>
      <div class="empty-hero__subtitle">
        点击右上角「新增账户」，为账套创建第一批资产、负债和权益科目。
      </div>
      <button class="btn btn--primary btn--pill empty-hero__action" @click="openAddModal">
        新增账户
      </button>
    </div>

    <!-- 新增账户模态框 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
      <div class="modal-content modal-content--md pop-in">
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
          <div class="form-group">
            <label class="form-label">父科目（可选）</label>
            <select v-model="newAccount.parentId" class="form-input">
              <option :value="undefined">无（顶级科目）</option>
              <option
                v-for="acc in accounts.filter(a => a.id && a.type === newAccount.type && a.id !== newAccount.parentId)"
                :key="acc.id"
                :value="acc.id"
              >
                {{ acc.code }} - {{ acc.name }}
              </option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn--ghost" @click="closeAddModal">取消</button>
          <button class="btn btn--primary" @click="saveNewAccount">保存</button>
        </div>
      </div>
    </div>

    <!-- 编辑账户模态框 -->
    <div v-if="showEditModal && editAccount" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <h3 class="modal-title">编辑账户</h3>
          <button class="modal-close" @click="closeEditModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">科目名称 <span class="required">*</span></label>
            <input
              v-model="editAccount.name"
              type="text"
              class="form-input"
              placeholder="请输入科目名称"
              @keyup.enter="saveEditAccount"
            />
          </div>
          <div class="form-group">
            <label class="form-label">科目编码 <span class="required">*</span></label>
            <input
              v-model="editAccount.code"
              type="text"
              class="form-input"
              placeholder="请输入科目编码"
              @keyup.enter="saveEditAccount"
            />
          </div>
          <div class="form-group">
            <label class="form-label">科目类型</label>
            <input
              type="text"
              class="form-input"
              :value="getTypeLabel(editAccount.type)"
              disabled
            />
          </div>
          <div class="form-group">
            <label class="form-label">父科目（可选）</label>
            <select v-model="editAccount.parentId" class="form-input">
              <option :value="undefined">无（顶级科目）</option>
              <option
                v-for="acc in accounts.filter(a => a.id && a.type === editAccount.type && a.id !== editAccount.id && !isDescendant(a.id, editAccount.id || undefined))"
                :key="acc.id"
                :value="acc.id"
              >
                {{ acc.code }} - {{ acc.name }}
              </option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn--ghost" @click="closeEditModal">取消</button>
          <button class="btn btn--primary" @click="saveEditAccount">保存</button>
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
}

const accounts = ref<AccountRow[]>([]);
const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');
const searchText = ref('');
const showAddModal = ref(false);
const showEditModal = ref(false);
const newAccount = ref({
  name: '',
  code: '',
  type: 'ASSET',
  parentId: undefined as number | undefined
});

const editAccount = ref<{
  id: number | null;
  name: string;
  code: string;
  type: string;
  parentId: number | undefined;
} | null>(null);

const load = async () => {
  try {
    const resp = await api.get('/accounts');
    const raw = resp.data;
    console.log('账户接口原始响应 /accounts =', raw);
    // 兼容后端返回 List 或分页 { content: [] }
    const list = Array.isArray(raw) ? raw : Array.isArray((raw as any)?.content) ? (raw as any).content : [];
    console.log('解析后的账户列表长度 =', list.length);
    // 规范化字段，避免前端因格式差异不渲染
    accounts.value = list.map((acc: any) => ({
      id: acc.id,
      _localId: acc._localId,
      name: acc.name,
      code: acc.code,
      type: acc.type,
      currencyCode: acc.currencyCode,
      balance: typeof acc.balance === 'number' ? acc.balance : Number(acc.balance ?? 0),
      parentId: acc.parentId ?? undefined,
      enabled: acc.enabled !== false
    }));
    message.value = `已加载 ${accounts.value.length} 条科目`;
    messageType.value = 'ok';
  } catch (e: any) {
    message.value = '加载失败: ' + (e?.response?.data?.message || e.message);
    messageType.value = 'error';
    console.error('加载科目失败', e);
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

// 构建扁平化的树状列表（带层级信息）
const buildFlatTree = (accounts: AccountRow[]): (AccountRow & { level: number; hasChildren?: boolean })[] => {
  const accountMap = new Map<number, AccountRow>();
  const childrenMap = new Map<number, AccountRow[]>();
  const result: (AccountRow & { level: number; hasChildren?: boolean })[] = [];

  // 第一遍：建立映射
  accounts.forEach(acc => {
    if (acc.id) {
      accountMap.set(acc.id, acc);
    }
  });

  // 第二遍：建立父子关系
  accounts.forEach(acc => {
    if (acc.parentId && accountMap.has(acc.parentId)) {
      if (!childrenMap.has(acc.parentId)) {
        childrenMap.set(acc.parentId, []);
      }
      childrenMap.get(acc.parentId)!.push(acc);
    }
  });

  // 递归函数：按层级顺序添加账户
  const addAccount = (acc: AccountRow, level: number) => {
    const hasChildren = childrenMap.has(acc.id || 0) && (childrenMap.get(acc.id || 0)?.length || 0) > 0;
    result.push({ ...acc, level, hasChildren });
    
    // 添加子账户
    if (hasChildren && acc.id) {
      const children = childrenMap.get(acc.id) || [];
      children.forEach(child => addAccount(child, level + 1));
    }
  };

  // 添加根节点
  accounts.forEach(acc => {
    if (!acc.parentId) {
      addAccount(acc, 0);
    }
  });

  return result;
};

const groupedAccounts = computed(() => {
  const typeOrder = ['ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE'];
  const result: { typeKey: string; rows: (AccountRow & { level: number; hasChildren?: boolean })[] }[] = [];

  const handledTypes = new Set<string>();

  typeOrder.forEach((typeKey) => {
    const sameType = filteredAccounts.value.filter(acc => acc.type === typeKey);
    if (sameType.length > 0) {
      result.push({
        typeKey,
        rows: buildFlatTree(sameType),
      });
      handledTypes.add(typeKey);
    }
  });

  // 兜底：如果后端返回了暂未识别的科目类型，也分一组展示，避免页面空白
  const others = filteredAccounts.value.filter(acc => !handledTypes.has(acc.type));
  if (others.length > 0) {
    result.push({
      typeKey: 'OTHER',
      rows: buildFlatTree(others),
    });
  }

  return result;
});

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    ASSET: '资产',
    LIABILITY: '负债',
    EQUITY: '权益',
    INCOME: '收入',
    EXPENSE: '费用',
    OTHER: '其他'
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

// 判断当前行是否处于编辑状态
const isEditing = (acc: AccountRow) => {
  return !!editAccount.value && editAccount.value.id === acc.id;
};

const openAddModal = () => {
  newAccount.value = {
    name: '',
    code: '',
    type: 'ASSET',
    parentId: undefined
  };
  showAddModal.value = true;
};

const closeAddModal = () => {
  showAddModal.value = false;
  newAccount.value = {
    name: '',
    code: '',
    type: 'ASSET',
    parentId: undefined
  };
};

const openEditModal = (acc: AccountRow) => {
  editAccount.value = {
    id: acc.id || null,
    name: acc.name,
    code: acc.code,
    type: acc.type,
    parentId: acc.parentId
  };
  showEditModal.value = true;
};

const closeEditModal = () => {
  showEditModal.value = false;
  editAccount.value = null;
};

const saveNewAccount = async () => {
  if (!newAccount.value.name?.trim() || !newAccount.value.code?.trim()) {
    message.value = '请填写科目名称和编码';
    messageType.value = 'error';
    return;
  }

  try {
    const payload: any = {
      name: newAccount.value.name.trim(),
      code: newAccount.value.code.trim(),
      type: newAccount.value.type,
      currencyCode: 'CNY', // 默认使用CNY
      enabled: true
    };
    if (newAccount.value.parentId) {
      payload.parentId = newAccount.value.parentId;
    }
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

const saveEditAccount = async () => {
  if (!editAccount.value) return;
  if (!editAccount.value.name?.trim() || !editAccount.value.code?.trim()) {
    message.value = '请填写科目名称和编码';
    messageType.value = 'error';
    return;
  }

  try {
    const payload: any = {
      name: editAccount.value.name.trim(),
      code: editAccount.value.code.trim(),
      type: editAccount.value.type,
      currencyCode: 'CNY', // 默认使用CNY
      enabled: true
    };
    if (editAccount.value.parentId) {
      payload.parentId = editAccount.value.parentId;
    }
    if (editAccount.value.id) {
      await api.put(`/accounts/${editAccount.value.id}`, payload);
      message.value = '保存成功';
      messageType.value = 'ok';
      closeEditModal();
    await load();
    setTimeout(() => {
      message.value = '';
    }, 3000);
    }
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

// 打开添加子科目模态框
const openAddSubAccount = (parent: AccountRow) => {
  newAccount.value = {
    name: '',
    code: '',
    type: parent.type,
    parentId: parent.id
  };
  showAddModal.value = true;
};

// 计算账户总数（扁平列表长度）
const countAccounts = (accounts: AccountRow[]): number => accounts.length;

// 检查是否是后代节点（防止循环引用）
const isDescendant = (potentialAncestorId: number | undefined, potentialDescendantId: number | undefined): boolean => {
  if (!potentialAncestorId || !potentialDescendantId) return false;
  if (potentialAncestorId === potentialDescendantId) return true;
  
  const findAccount = (id: number): AccountRow | undefined => {
    return accounts.value.find(a => a.id === id);
  };
  
  let current = findAccount(potentialDescendantId);
  while (current?.parentId) {
    if (current.parentId === potentialAncestorId) {
      return true;
    }
    current = findAccount(current.parentId);
  }
  return false;
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

.tree-line {
  position: relative;
  display: inline-block;
  width: 10px;
  height: 18px;
  margin-right: 2px;
}

.tree-line::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -2px;
  bottom: 2px;
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}

.account-name {
  display: flex;
  align-items: center;
  gap: 4px;
}

.account-name__text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.account-name--child {
  font-size: 13px;
  color: var(--text-main);
}

.account-child-row {
  background: #f8f9fa;
}

.account-child-row:hover {
  background: #e9ecef;
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


