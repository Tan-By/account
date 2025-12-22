<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">账户/科目（US003 部分）</div>
        <div class="page-header__subtitle">管理树状账户结构与多币种属性</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="addRow">新增账户</button>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>名称</th>
            <th>编码</th>
            <th>类型</th>
            <th>币种</th>
            <th>余额</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="acc in accounts" :key="acc.id ?? acc._localId">
            <td><input v-model="acc.name" /></td>
            <td><input v-model="acc.code" style="width: 80px" /></td>
            <td>
              <select v-model="acc.type">
                <option value="ASSET">资产</option>
                <option value="LIABILITY">负债</option>
                <option value="EQUITY">权益</option>
                <option value="INCOME">收入</option>
                <option value="EXPENSE">费用</option>
              </select>
            </td>
            <td><input v-model="acc.currencyCode" style="width: 70px" /></td>
            <td>{{ acc.balance?.toFixed?.(2) ?? '-' }}</td>
            <td>
              <button class="btn btn--ghost" @click="save(acc)">保存</button>
              <button
                v-if="acc.id"
                class="btn btn--ghost"
                style="color: #fecaca;"
                @click="remove(acc)"
              >
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="message" style="margin-top: 8px;">
      <span class="badge" :class="messageType === 'ok' ? 'badge--ok' : 'badge--error'">
        {{ message }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';

interface AccountRow {
  id?: number;
  _localId?: number;
  name: string;
  code: string;
  type: string;
  currencyCode: string;
  balance?: number;
}

const accounts = ref<AccountRow[]>([]);
const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');
let localId = 1;

const load = async () => {
  const resp = await api.get('/accounts');
  accounts.value = resp.data;
};

const addRow = () => {
  accounts.value.push({
    _localId: localId++,
    name: '',
    code: '',
    type: 'ASSET',
    currencyCode: 'CNY'
  });
};

const save = async (row: AccountRow) => {
  try {
    const payload = {
      name: row.name,
      code: row.code,
      type: row.type,
      currencyCode: row.currencyCode,
      enabled: true
    };
    if (row.id) {
      await api.put(`/accounts/${row.id}`, payload);
    } else {
      const resp = await api.post('/accounts', payload);
      Object.assign(row, resp.data);
    }
    message.value = '保存成功';
    messageType.value = 'ok';
    await load();
  } catch (e: any) {
    message.value = e?.response?.data?.message || '保存失败';
    messageType.value = 'error';
  }
};

const remove = async (row: AccountRow) => {
  if (!row.id) return;
  await api.delete(`/accounts/${row.id}`);
  await load();
};

onMounted(load);
</script>


