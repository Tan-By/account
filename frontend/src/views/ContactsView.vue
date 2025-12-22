<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">外部联系人（US006）</div>
        <div class="page-header__subtitle">集中维护供应商/客户信息，支撑后续业务单据</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="openCreate">新增联系人</button>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>名称</th>
            <th>类型</th>
            <th>纳税人识别号</th>
            <th>电话</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in contacts" :key="c.id">
            <td>{{ c.name }}</td>
            <td>{{ c.type }}</td>
            <td>{{ c.taxId || '-' }}</td>
            <td>{{ c.phone || '-' }}</td>
            <td>
              <span class="badge" :class="c.status === '活跃' ? 'badge--ok' : 'badge--warn'">
                {{ c.status }}
              </span>
            </td>
            <td>
              <button class="btn btn--ghost" @click="edit(c)">编辑</button>
              <button
                v-if="c.status === '活跃'"
                class="btn btn--ghost"
                @click="deactivate(c)"
              >
                停用
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editing" class="card" style="margin-top: 10px;">
      <div class="card-title">{{ editing.id ? '编辑联系人' : '新增联系人' }}</div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">名称</label>
          <input v-model="editing.name" />
        </div>
        <div class="form-col">
          <label class="form-label">类型</label>
          <select v-model="editing.type">
            <option value="供应商">供应商</option>
            <option value="客户">客户</option>
          </select>
        </div>
        <div class="form-col">
          <label class="form-label">纳税人识别号</label>
          <input v-model="editing.taxId" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">电话</label>
          <input v-model="editing.phone" />
        </div>
        <div class="form-col">
          <label class="form-label">开户行</label>
          <input v-model="editing.bankName" />
        </div>
        <div class="form-col">
          <label class="form-label">银行账号</label>
          <input v-model="editing.bankAccount" />
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

interface Contact {
  id?: number;
  name: string;
  type: string;
  taxId?: string;
  phone?: string;
  bankName?: string;
  bankAccount?: string;
  status?: string;
}

const contacts = ref<Contact[]>([]);
const editing = ref<Contact | null>(null);

const load = async () => {
  const resp = await api.get('/external-contacts');
  contacts.value = resp.data;
};

const openCreate = () => {
  editing.value = { name: '', type: '供应商', status: '活跃' };
};

const edit = (c: Contact) => {
  editing.value = { ...c };
};

const deactivate = async (c: Contact) => {
  if (!c.id) return;
  await api.post(`/external-contacts/${c.id}/deactivate`);
  await load();
};

const save = async () => {
  if (!editing.value) return;
  const payload = editing.value;
  if (editing.value.id) {
    await api.put(`/external-contacts/${editing.value.id}`, payload);
  } else {
    await api.post('/external-contacts', payload);
  }
  editing.value = null;
  await load();
};

onMounted(load);
</script>


