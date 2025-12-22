<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">记账（US003）</div>
        <div class="page-header__subtitle">类 GnuCash 分录表：有借必有贷，借贷必相等</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="postTransaction">保存/过账</button>
      </div>
    </div>

    <div class="card">
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

      <table>
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
            <td><input type="number" v-model.number="line.debit" /></td>
            <td><input type="number" v-model.number="line.credit" /></td>
            <td>
              <button class="btn btn--ghost" @click="removeLine(idx)">删除</button>
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

const date = ref<string>(new Date().toISOString().slice(0, 10));
const description = ref('');
const accounts = ref<Account[]>([]);
const lines = ref<Line[]>([
  { debit: 0, credit: 0 },
  { debit: 0, credit: 0 }
]);

const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');

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
  } catch (e: any) {
    message.value = e?.response?.data?.message || '记账失败';
    messageType.value = 'error';
  }
};

onMounted(loadAccounts);
</script>


