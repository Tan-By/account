<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">税务申报（US001）</div>
        <div class="page-header__subtitle">从系统数据生成申报草稿，模拟提交与状态流转</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="createDraft">生成申报草稿</button>
      </div>
    </div>

    <div class="card">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">税种</label>
          <select v-model="taxType">
            <option value="增值税">增值税</option>
            <option value="企业所得税">企业所得税</option>
          </select>
        </div>
        <div class="form-col">
          <label class="form-label">申报期开始</label>
          <input type="date" v-model="periodStart" />
        </div>
        <div class="form-col">
          <label class="form-label">申报期结束</label>
          <input type="date" v-model="periodEnd" />
        </div>
      </div>
    </div>

    <div v-if="current" class="card" style="margin-top: 10px;">
      <div class="card-title">申报表草稿（ID：{{ current.id }}）</div>
      <div class="card-subtitle">状态：{{ current.status }}</div>
      <table>
        <tbody>
          <tr>
            <td style="width: 200px;">计税依据</td>
            <td>{{ current.taxableAmount }}</td>
          </tr>
          <tr>
            <td>应纳税额</td>
            <td>{{ current.taxPayable }}</td>
          </tr>
          <tr>
            <td>减免税额</td>
            <td>{{ current.taxRelief }}</td>
          </tr>
          <tr>
            <td>已缴税额</td>
            <td>{{ current.taxPaid }}</td>
          </tr>
          <tr>
            <td>应补(退)税额</td>
            <td>{{ current.taxDue }}</td>
          </tr>
        </tbody>
      </table>

      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary" @click="submit" :disabled="current.status !== '草稿'">
          提交申报
        </button>
        <button
          class="btn btn--ghost"
          @click="markSuccess"
          :disabled="current.status !== '已提交'"
        >
          模拟税局确认成功
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '../api';

const taxType = ref('增值税');
const today = new Date().toISOString().slice(0, 10);
const periodStart = ref(today);
const periodEnd = ref(today);

const current = ref<any | null>(null);

const createDraft = async () => {
  const payload = {
    taxType: taxType.value,
    periodStart: periodStart.value,
    periodEnd: periodEnd.value
  };
  const resp = await api.post('/tax-declarations/draft', payload);
  current.value = resp.data;
};

const submit = async () => {
  if (!current.value) return;
  const resp = await api.post(`/tax-declarations/${current.value.id}/submit`);
  current.value = resp.data;
};

const markSuccess = async () => {
  if (!current.value) return;
  const resp = await api.post(`/tax-declarations/${current.value.id}/success`);
  current.value = resp.data;
};
</script>


