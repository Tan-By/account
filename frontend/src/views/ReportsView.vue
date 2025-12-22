<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">财务报表（US004）</div>
        <div class="page-header__subtitle">利润表与资产负债表，勾稽关系一目了然</div>
      </div>
      <div class="toolbar">
        <div class="pill-group">
          <div
            class="pill"
            :class="activeTab === 'profit' ? 'pill--active' : ''"
            @click="activeTab = 'profit'"
          >
            利润表
          </div>
          <div
            class="pill"
            :class="activeTab === 'balance' ? 'pill--active' : ''"
            @click="activeTab = 'balance'"
          >
            资产负债表
          </div>
        </div>
        <button class="btn btn--primary" @click="load">生成报表</button>
      </div>
    </div>

    <div class="card">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">期间开始</label>
          <input type="date" v-model="startDate" />
        </div>
        <div class="form-col">
          <label class="form-label">期间结束</label>
          <input type="date" v-model="endDate" />
        </div>
        <div class="form-col">
          <label class="form-label">报表币种</label>
          <input v-model="currencyCode" style="width: 80px" />
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'profit'" class="card" style="margin-top: 10px;">
      <div class="card-title">简版利润表（{{ currencyCode }}）</div>
      <table>
        <tbody>
          <tr>
            <td style="width: 180px;">营业收入合计</td>
            <td>{{ profit?.incomeTotal?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>费用合计</td>
            <td>{{ profit?.expenseTotal?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td><strong>净利润</strong></td>
            <td><strong>{{ profit?.netProfit?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="card" style="margin-top: 10px;">
      <div class="card-title">资产负债表摘要（{{ currencyCode }}）</div>
      <table>
        <tbody>
          <tr>
            <td style="width: 200px;">资产总计</td>
            <td>{{ balance?.assetTotal?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>负债总计</td>
            <td>{{ balance?.liabilityTotal?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td>所有者权益总计</td>
            <td>{{ balance?.equityTotal?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td><strong>平衡校验：资产 vs 负债+权益</strong></td>
            <td>
              <span class="badge" :class="balance?.balanced ? 'badge--ok' : 'badge--error'">
                {{ balance?.balanced ? '已平衡' : '不平衡，请检查账务' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '../api';

const startDate = ref<string>(new Date().toISOString().slice(0, 10));
const endDate = ref<string>(new Date().toISOString().slice(0, 10));
const currencyCode = ref('CNY');

const activeTab = ref<'profit' | 'balance'>('profit');

const profit = ref<any>(null);
const balance = ref<any>(null);

const load = async () => {
  const base = {
    startDate: startDate.value,
    endDate: endDate.value,
    currencyCode: currencyCode.value
  };
  const [pResp, bResp] = await Promise.all([
    api.post('/reports/profit', { ...base, type: 'PROFIT' }),
    api.post('/reports/balance-sheet', { ...base, type: 'BALANCE_SHEET' })
  ]);
  profit.value = pResp.data;
  balance.value = bResp.data;
};
</script>


