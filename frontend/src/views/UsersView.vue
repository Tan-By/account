<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">用户与权限（US005）</div>
        <div class="page-header__subtitle">管理内部人员账户与角色</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="openCreate">新增用户</button>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div class="card card--panel fade-in">
      <table class="sheet-table table-compact table-quiet">
        <thead>
          <tr>
            <th>姓名</th>
            <th>用户名</th>
            <th>部门</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.name }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.department || '-' }}</td>
            <td>{{ (u.roles || []).join(', ') }}</td>
            <td>
              <span class="badge" :class="u.status === '启用' ? 'badge--ok' : 'badge--warn'">
                {{ u.status }}
              </span>
            </td>
            <td>
              <button class="btn btn--ghost btn--pill btn--small" @click="edit(u)">编辑</button>
              <button class="btn btn--ghost btn--pill btn--small" @click="disable(u)" v-if="u.status === '启用'">
                禁用
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑用户：子窗口 -->
    <div v-if="editing" class="modal-overlay" @click.self="editing = null">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <div>
            <h3 class="modal-title">{{ editing.id ? '编辑用户' : '新增用户' }}</h3>
            <p class="modal-subtitle">设置基础信息与角色，未启用的字段保持为空</p>
          </div>
          <button class="modal-close" @click="editing = null">×</button>
        </div>
        <div class="modal-body">
          <div class="form-grid form-grid--three">
            <div class="form-item">
              <label class="form-label">姓名 *</label>
              <input v-model="editing.name" placeholder="请输入姓名" />
            </div>
            <div class="form-item">
              <label class="form-label">用户名 *</label>
              <input v-model="editing.username" placeholder="登录用户名" />
            </div>
            <div class="form-item" v-if="!editing.id">
              <label class="form-label">初始密码 *</label>
              <input v-model="editing.password" type="password" placeholder="至少 6 位" />
            </div>
          </div>
          <div class="form-grid form-grid--two">
            <div class="form-item">
              <label class="form-label">部门</label>
              <input v-model="editing.department" placeholder="如：财务部/销售部" />
            </div>
            <div class="form-item">
              <label class="form-label">角色</label>
              <select v-model="selectedRoles" multiple class="multiselect">
                <option v-for="role in availableRoles" :key="role" :value="role">
                  {{ role }}
                </option>
              </select>
              <div class="field-hint">按住 Ctrl (Windows) 或 Cmd (Mac) 键可多选</div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="modal-actions">
            <button class="btn btn--ghost" @click="editing = null">取消</button>
            <button class="btn btn--primary" @click="save">保存</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 员工信息列表 -->
    <div class="card card--panel fade-in" style="margin-top: 24px;">
      <div class="card__section-head">
        <div>
          <div class="card-title" style="margin-bottom: 2px;">员工信息</div>
          <div class="card-subtitle" v-if="users.length > 0">共 {{ users.length }} 名员工</div>
        </div>
        <button class="btn btn--ghost btn--pill btn--small" @click="showUserList = !showUserList">
          {{ showUserList ? '收起' : '展开' }}
        </button>
      </div>
      
      <div v-if="showUserList">
        <div v-if="users.length === 0" class="empty-hero">
          <div class="empty-hero__icon">👥</div>
          <div class="empty-hero__title">暂无员工信息</div>
          <div class="empty-hero__subtitle">
            点击右上角「新增用户」，为系统添加第一个内部用户。
          </div>
          <button class="btn btn--primary btn--pill empty-hero__action" @click="openCreate">
            新增用户
          </button>
        </div>

        <div v-else>
          <div v-for="u in users" :key="u.id" class="user-item card card--ghost fade-in" style="margin-bottom: 10px; box-shadow: none;">
            <div class="user-header">
              <div class="user-info">
                <span class="user-name">{{ u.name }}</span>
                <span class="user-username">{{ u.username }}</span>
                <span v-if="u.department" class="user-department">{{ u.department }}</span>
              </div>
              <div class="user-meta">
                <span class="badge" :class="u.status === '启用' ? 'badge--ok' : 'badge--warn'">
                  {{ u.status }}
                </span>
              </div>
            </div>
            <div class="user-details">
              <div class="user-detail-row">
                <span class="detail-label">角色：</span>
                <span class="detail-value">{{ (u.roles || []).join(', ') || '无' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';

interface User {
  id?: number;
  name: string;
  username: string;
  password?: string;
  department?: string;
  roles?: string[];
  status?: string;
}

const users = ref<User[]>([]);
const editing = ref<User | null>(null);
const selectedRoles = ref<string[]>([]);
const availableRoles = ref<string[]>([]);
const showUserList = ref(true);

const load = async () => {
  const resp = await api.get('/users');
  users.value = resp.data;
};

const loadRoles = async () => {
  try {
    const resp = await api.get('/users/roles');
    availableRoles.value = resp.data;
  } catch (e) {
    console.error('加载角色列表失败:', e);
  }
};

const openCreate = () => {
  editing.value = { name: '', username: '', password: '', roles: [] };
  selectedRoles.value = [];
};

const edit = (u: User) => {
  editing.value = { ...u };
  selectedRoles.value = [...(u.roles || [])];
};

const disable = async (u: User) => {
  if (!u.id) return;
  await api.post(`/users/${u.id}/disable`);
  await load();
};

const save = async () => {
  if (!editing.value) return;
  const payload = {
    ...editing.value,
    roles: selectedRoles.value
  };
  if (editing.value.id) {
    await api.put(`/users/${editing.value.id}`, payload);
  } else {
    await api.post('/users', payload);
  }
  editing.value = null;
  selectedRoles.value = [];
  await load();
};

onMounted(() => {
  load();
  loadRoles();
});
</script>

<style scoped>
.user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
}

.user-username {
  font-size: 12px;
  color: var(--text-muted);
}

.user-department {
  font-size: 12px;
  color: var(--text-muted);
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-details {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
}

.user-detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.detail-label {
  color: var(--text-muted);
}

.detail-value {
  color: var(--text-main);
}
</style>


