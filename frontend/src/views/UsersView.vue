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

    <div class="card">
      <table>
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
              <button class="btn btn--ghost" @click="edit(u)">编辑</button>
              <button class="btn btn--ghost" @click="disable(u)" v-if="u.status === '启用'">
                禁用
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editing" class="card" style="margin-top: 10px;">
      <div class="card-title">{{ editing.id ? '编辑用户' : '新增用户' }}</div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">姓名</label>
          <input v-model="editing.name" />
        </div>
        <div class="form-col">
          <label class="form-label">用户名</label>
          <input v-model="editing.username" />
        </div>
        <div class="form-col" v-if="!editing.id">
          <label class="form-label">初始密码</label>
          <input v-model="editing.password" type="password" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">部门</label>
          <input v-model="editing.department" />
        </div>
        <div class="form-col">
          <label class="form-label">角色</label>
          <select v-model="selectedRoles" multiple style="min-height: 80px;">
            <option v-for="role in availableRoles" :key="role" :value="role">
              {{ role }}
            </option>
          </select>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            按住 Ctrl (Windows) 或 Cmd (Mac) 键可多选
          </div>
        </div>
      </div>
      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary" @click="save">保存</button>
        <button class="btn btn--ghost" @click="editing = null">取消</button>
      </div>
    </div>

    <!-- 员工信息列表 -->
    <div class="card" style="margin-top: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3 style="margin: 0; font-size: 16px; font-weight: 500;">员工信息</h3>
        <div style="display: flex; align-items: center; gap: 8px;">
          <span v-if="users.length > 0" style="font-size: 12px; color: var(--text-muted);">
            共 {{ users.length }} 名员工
          </span>
          <button class="btn btn--ghost" @click="showUserList = !showUserList">
            {{ showUserList ? '收起' : '展开' }}
          </button>
        </div>
      </div>
      
      <div v-if="showUserList">
        <div v-if="users.length === 0" class="empty-state">
          暂无员工信息
        </div>
        
        <div v-else>
          <div v-for="u in users" :key="u.id" class="user-item">
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
.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.user-item {
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
  background: var(--bg-secondary);
}

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
  font-weight: 500;
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
  background: var(--bg-tertiary);
  border-radius: 3px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-details {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
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
  color: var(--text-primary);
}
</style>


