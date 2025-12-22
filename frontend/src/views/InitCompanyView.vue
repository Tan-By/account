<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">初始化企业（US007）</div>
        <div class="page-header__subtitle">设置账套信息与期初余额，校验“资产 = 负债 + 权益”</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary" @click="doInit">完成初始化</button>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 12px;">
      <div class="card">
        <div class="card-title">账套信息</div>
        <div class="form-row">
          <div class="form-col">
            <label class="form-label">企业名称</label>
            <input v-model="companyName" placeholder="例如：XX科技有限公司" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-col">
            <label class="form-label">默认币种</label>
            <input v-model="currencyCode" placeholder="CNY" />
          </div>
          <div class="form-col">
            <label class="form-label">启用日期</label>
            <input type="date" v-model="startDate" />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">期初科目与余额</div>
        <div class="card-subtitle">示例：资产“现金”和权益“实收资本”金额相等</div>
        <table>
          <thead>
            <tr>
              <th>编码</th>
              <th>名称</th>
              <th>类别</th>
              <th>币种</th>
              <th>期初余额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in rows" :key="idx">
              <td><input v-model="row.code" style="width: 80px" /></td>
              <td><input v-model="row.name" /></td>
              <td>
                <select v-model="row.type">
                  <option value="ASSET">资产</option>
                  <option value="LIABILITY">负债</option>
                  <option value="EQUITY">权益</option>
                  <option value="INCOME">收入</option>
                  <option value="EXPENSE">费用</option>
                </select>
              </td>
              <td><input v-model="row.currencyCode" style="width: 70px" /></td>
              <td><input v-model.number="row.initialBalance" type="number" style="width: 90px" /></td>
            </tr>
          </tbody>
        </table>
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
import { ref } from 'vue';
import { api } from '../api';

interface InitRow {
  code: string;
  name: string;
  type: string;
  currencyCode: string;
  initialBalance: number;
}

const companyName = ref('示例企业');
const currencyCode = ref('CNY');
const startDate = ref<string>(new Date().toISOString().slice(0, 10));

const rows = ref<InitRow[]>([
  { code: '1001', name: '现金', type: 'ASSET', currencyCode: 'CNY', initialBalance: 1000 },
  { code: '3001', name: '实收资本', type: 'EQUITY', currencyCode: 'CNY', initialBalance: 1000 }
]);

const message = ref('');
const messageType = ref<'ok' | 'error'>('ok');

const doInit = async () => {
  try {
    const payload = {
      companyName: companyName.value,
      defaultCurrencyCode: currencyCode.value,
      startDate: startDate.value,
      accounts: rows.value
    };
    await api.post('/init', payload);
    message.value = '初始化成功，期初余额已平衡。';
    messageType.value = 'ok';
  } catch (e: any) {
    message.value = e?.response?.data?.message || '初始化失败，请检查输入和期初平衡。';
    messageType.value = 'error';
  }
};
</script>


