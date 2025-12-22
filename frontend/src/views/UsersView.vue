<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">用户与权限（US005）</div>
        <div class="page-header__subtitle">管理内部人员账户与角色（仅做演示，不接真实登录）</div>
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
          <label class="form-label">角色（逗号分隔）</label>
          <input v-model="rolesText" placeholder="如：FINANCE_CLERK,SYSTEM_ADMIN" />
        </div>
      </div>
      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary" @click="save">保存</button>
        <button class="btn btn--ghost" @click="editing = null">取消</button>
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
const rolesText = ref('');

const load = async () => {
  const resp = await api.get('/users');
  users.value = resp.data;
};

const openCreate = () => {
  editing.value = { name: '', username: '', password: '', roles: ['SYSTEM_ADMIN'] };
  rolesText.value = (editing.value.roles || []).join(',');
};

const edit = (u: User) => {
  editing.value = { ...u };
  rolesText.value = (u.roles || []).join(',');
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
    roles: rolesText.value
      .split(',')
      .map((r) => r.trim())
      .filter((r) => r)
  };
  if (editing.value.id) {
    await api.put(`/users/${editing.value.id}`, payload);
  } else {
    await api.post('/users', payload);
  }
  editing.value = null;
  await load();
};

onMounted(load);
</script>


