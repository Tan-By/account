<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">银行对账（US002）</div>
        <div class="page-header__subtitle">基于金额+日期自动匹配，生成对账结果与调节余额</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="doReconcile">开始对账</button>
      </div>
    </div>

    <div class="card">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">银行账户</label>
          <select v-model.number="bankAccountId">
            <option :value="undefined">选择银行账户</option>
            <option
              v-for="acc in bankAccounts"
              :key="acc.id"
              :value="acc.id"
            >
              {{ acc.name }}（{{ acc.currencyCode }}）
            </option>
          </select>
        </div>
        <div class="form-col">
          <label class="form-label">对账期间开始</label>
          <input type="date" v-model="periodStart" />
        </div>
        <div class="form-col">
          <label class="form-label">对账期间结束</label>
          <input type="date" v-model="periodEnd" />
        </div>
      </div>
    </div>

    <div v-if="result" class="card" style="margin-top: 10px;">
      <div class="card-title">对账结果摘要（任务ID：{{ result.taskId }}）</div>
      <table>
        <tbody>
          <tr>
            <td>已匹配记录数</td>
            <td>{{ result.matchedCount }}</td>
          </tr>
          <tr>
            <td>银行未匹配记录数</td>
            <td>{{ result.unmatchedBankCount }}</td>
          </tr>
          <tr>
            <td>账面未匹配记录数</td>
            <td>{{ result.unmatchedBookCount }}</td>
          </tr>
          <tr>
            <td>账面余额</td>
            <td>{{ result.bookBalance }}</td>
          </tr>
          <tr>
            <td>银行余额</td>
            <td>{{ result.bankBalance }}</td>
          </tr>
          <tr>
            <td>调节后账面余额</td>
            <td>{{ result.adjustedBookBalance }}</td>
          </tr>
          <tr>
            <td>调节后银行余额</td>
            <td>{{ result.adjustedBankBalance }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';

interface Account {
  id: number;
  name: string;
  currencyCode: string;
}

const bankAccounts = ref<Account[]>([]);
const bankAccountId = ref<number | undefined>();
const periodStart = ref<string>(new Date().toISOString().slice(0, 10));
const periodEnd = ref<string>(new Date().toISOString().slice(0, 10));

const result = ref<any>(null);

const loadBankAccounts = async () => {
  const resp = await api.get('/accounts');
  bankAccounts.value = resp.data.filter((a: any) => a.type === 'ASSET');
};

const doReconcile = async () => {
  if (!bankAccountId.value) return;
  const payload = {
    bankAccountId: bankAccountId.value,
    periodStart: periodStart.value,
    periodEnd: periodEnd.value,
    executor: 'demo-user'
  };
  const resp = await api.post('/reconciliations', payload);
  result.value = resp.data;
};

onMounted(loadBankAccounts);
</script>


