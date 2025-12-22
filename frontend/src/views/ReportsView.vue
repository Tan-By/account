<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">财务报表（US004）</div>
        <div class="page-header__subtitle">利润表、资产负债表与现金流量表，勾稽关系一目了然</div>
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
          <div
            class="pill"
            :class="activeTab === 'cashflow' ? 'pill--active' : ''"
            @click="activeTab = 'cashflow'"
          >
            现金流量表
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
          <select v-model="currencyCode" class="currency-select">
            <option v-for="curr in currencies" :key="curr.code" :value="curr.code">
              {{ curr.code }} - {{ curr.name }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'profit'" class="card" style="margin-top: 10px;">
      <div class="card-title">利润表（{{ currencyCode }}）</div>
      <table>
        <tbody>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">一、营业收入</td>
          </tr>
          <template v-if="profit?.revenueItems && profit.revenueItems.length > 0">
            <tr v-for="item in profit.revenueItems" :key="item.accountName">
              <td style="width: 200px; padding-left: 20px;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #666; font-size: 12px;"> ({{ item.accountCode }})</span>
              </td>
              <td>{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr v-else>
            <td style="padding-left: 20px;">营业收入</td>
            <td>{{ profit?.operatingRevenue?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>营业收入合计</strong></td>
            <td><strong>{{ profit?.operatingRevenue?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">二、营业成本</td>
          </tr>
          <template v-if="profit?.costItems && profit.costItems.length > 0">
            <tr v-for="item in profit.costItems" :key="item.accountName">
              <td style="padding-left: 20px;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #666; font-size: 12px;"> ({{ item.accountCode }})</span>
              </td>
              <td>{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr v-else>
            <td style="padding-left: 20px;">营业成本</td>
            <td>{{ profit?.operatingCost?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>营业成本合计</strong></td>
            <td><strong>{{ profit?.operatingCost?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">三、营业费用</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">销售费用</td>
            <td>{{ profit?.sellingExpenses?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">管理费用</td>
            <td>{{ profit?.administrativeExpenses?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">财务费用</td>
            <td>{{ profit?.financialExpenses?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <template v-if="profit?.expenseItems && profit.expenseItems.length > 0">
            <tr v-for="item in profit.expenseItems" :key="item.accountName">
              <td style="padding-left: 40px; font-size: 12px; color: #666;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #999;"> ({{ item.accountCode }})</span>
              </td>
              <td style="font-size: 12px; color: #666;">{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr>
            <td style="padding-left: 20px;"><strong>营业费用合计</strong></td>
            <td><strong>{{ profit?.operatingExpenses?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">四、营业利润</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>营业利润</strong></td>
            <td><strong>{{ profit?.operatingProfit?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">五、利润总额</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>利润总额</strong></td>
            <td><strong>{{ profit?.totalProfit?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">六、净利润</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>净利润</strong></td>
            <td><strong>{{ profit?.netProfit?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="activeTab === 'balance'" class="card" style="margin-top: 10px;">
      <div class="card-title">资产负债表（{{ currencyCode }}）</div>
      <table>
        <tbody>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">资产</td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-left: 10px; padding-top: 5px;">流动资产</td>
          </tr>
          <tr>
            <td style="width: 200px; padding-left: 20px;">现金及现金等价物</td>
            <td>{{ balance?.cashAndEquivalents?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">应收账款</td>
            <td>{{ balance?.accountsReceivable?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">存货</td>
            <td>{{ balance?.inventory?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">预付账款</td>
            <td>{{ balance?.prepaidExpenses?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <template v-if="balance?.currentAssetItems && balance.currentAssetItems.length > 0">
            <tr v-for="item in balance.currentAssetItems" :key="item.accountName">
              <td style="padding-left: 40px; font-size: 12px; color: #666;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #999;"> ({{ item.accountCode }})</span>
              </td>
              <td style="font-size: 12px; color: #666;">{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr>
            <td style="padding-left: 20px;"><strong>流动资产合计</strong></td>
            <td><strong>{{ balance?.currentAssets?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-left: 10px; padding-top: 5px;">非流动资产</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">固定资产</td>
            <td>{{ balance?.fixedAssets?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">长期投资</td>
            <td>{{ balance?.longTermInvestments?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <template v-if="balance?.nonCurrentAssetItems && balance.nonCurrentAssetItems.length > 0">
            <tr v-for="item in balance.nonCurrentAssetItems" :key="item.accountName">
              <td style="padding-left: 40px; font-size: 12px; color: #666;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #999;"> ({{ item.accountCode }})</span>
              </td>
              <td style="font-size: 12px; color: #666;">{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr>
            <td style="padding-left: 20px;"><strong>非流动资产合计</strong></td>
            <td><strong>{{ balance?.nonCurrentAssets?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td style="padding-left: 10px;"><strong>资产总计</strong></td>
            <td><strong>{{ balance?.assetTotal?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">负债和所有者权益</td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-left: 10px; padding-top: 5px;">负债</td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-left: 20px; padding-top: 3px;">流动负债</td>
          </tr>
          <tr>
            <td style="padding-left: 30px;">应付账款</td>
            <td>{{ balance?.accountsPayable?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 30px;">短期借款</td>
            <td>{{ balance?.shortTermDebt?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 30px;">应付费用</td>
            <td>{{ balance?.accruedExpenses?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <template v-if="balance?.currentLiabilityItems && balance.currentLiabilityItems.length > 0">
            <tr v-for="item in balance.currentLiabilityItems" :key="item.accountName">
              <td style="padding-left: 50px; font-size: 12px; color: #666;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #999;"> ({{ item.accountCode }})</span>
              </td>
              <td style="font-size: 12px; color: #666;">{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr>
            <td style="padding-left: 30px;"><strong>流动负债合计</strong></td>
            <td><strong>{{ balance?.currentLiabilities?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-left: 20px; padding-top: 3px;">非流动负债</td>
          </tr>
          <tr>
            <td style="padding-left: 30px;">长期借款</td>
            <td>{{ balance?.longTermDebt?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <template v-if="balance?.nonCurrentLiabilityItems && balance.nonCurrentLiabilityItems.length > 0">
            <tr v-for="item in balance.nonCurrentLiabilityItems" :key="item.accountName">
              <td style="padding-left: 50px; font-size: 12px; color: #666;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #999;"> ({{ item.accountCode }})</span>
              </td>
              <td style="font-size: 12px; color: #666;">{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr>
            <td style="padding-left: 30px;"><strong>非流动负债合计</strong></td>
            <td><strong>{{ balance?.nonCurrentLiabilities?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>负债总计</strong></td>
            <td><strong>{{ balance?.liabilityTotal?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-left: 10px; padding-top: 5px;">所有者权益</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">实收资本</td>
            <td>{{ balance?.paidInCapital?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">留存收益</td>
            <td>{{ balance?.retainedEarnings?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <template v-if="balance?.equityItems && balance.equityItems.length > 0">
            <tr v-for="item in balance.equityItems" :key="item.accountName">
              <td style="padding-left: 40px; font-size: 12px; color: #666;">
                {{ item.accountName }}<span v-if="item.accountCode" style="color: #999;"> ({{ item.accountCode }})</span>
              </td>
              <td style="font-size: 12px; color: #666;">{{ item.amount?.toFixed?.(2) ?? '-' }}</td>
            </tr>
          </template>
          <tr>
            <td style="padding-left: 20px;"><strong>所有者权益总计</strong></td>
            <td><strong>{{ balance?.equityTotal?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td style="padding-left: 10px;"><strong>负债和所有者权益总计</strong></td>
            <td><strong>{{ ((balance?.liabilityTotal ?? 0) + (balance?.equityTotal ?? 0))?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="padding-top: 10px;">
              <span class="badge" :class="balance?.balanced ? 'badge--ok' : 'badge--error'">
                {{ balance?.balanced ? '✓ 已平衡：资产 = 负债 + 所有者权益' : '✗ 不平衡，请检查账务' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="card" style="margin-top: 10px;">
      <div class="card-title">现金流量表（{{ currencyCode }}）</div>
      <table>
        <tbody>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">一、经营活动产生的现金流量</td>
          </tr>
          <tr>
            <td style="width: 200px; padding-left: 20px;">经营活动现金流入</td>
            <td>{{ cashFlow?.operatingCashInflow?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">经营活动现金流出</td>
            <td>{{ cashFlow?.operatingCashOutflow?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>经营活动产生的现金流量净额</strong></td>
            <td><strong>{{ cashFlow?.operatingNetCashFlow?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">二、投资活动产生的现金流量</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">投资活动现金流入</td>
            <td>{{ cashFlow?.investingCashInflow?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">投资活动现金流出</td>
            <td>{{ cashFlow?.investingCashOutflow?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>投资活动产生的现金流量净额</strong></td>
            <td><strong>{{ cashFlow?.investingNetCashFlow?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">三、筹资活动产生的现金流量</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">筹资活动现金流入</td>
            <td>{{ cashFlow?.financingCashInflow?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">筹资活动现金流出</td>
            <td>{{ cashFlow?.financingCashOutflow?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>筹资活动产生的现金流量净额</strong></td>
            <td><strong>{{ cashFlow?.financingNetCashFlow?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">四、现金及现金等价物净增加额</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>现金及现金等价物净增加额</strong></td>
            <td><strong>{{ cashFlow?.netIncreaseInCash?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">五、期初现金及现金等价物余额</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;">期初现金及现金等价物余额</td>
            <td>{{ cashFlow?.beginningCashBalance?.toFixed?.(2) ?? '-' }}</td>
          </tr>
          <tr>
            <td colspan="2" style="font-weight: bold; padding-top: 10px;">六、期末现金及现金等价物余额</td>
          </tr>
          <tr>
            <td style="padding-left: 20px;"><strong>期末现金及现金等价物余额</strong></td>
            <td><strong>{{ cashFlow?.endingCashBalance?.toFixed?.(2) ?? '-' }}</strong></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '../api';

const startDate = ref<string>(new Date().toISOString().slice(0, 10));
const endDate = ref<string>(new Date().toISOString().slice(0, 10));
const currencyCode = ref('CNY');
const currencies = ref<Array<{ code: string; name: string }>>([]);

const activeTab = ref<'profit' | 'balance' | 'cashflow'>('profit');

const profit = ref<any>(null);
const balance = ref<any>(null);
const cashFlow = ref<any>(null);

const loadCurrencies = async () => {
  try {
    const resp = await api.get('/currencies');
    currencies.value = resp.data.map((curr: any) => ({
      code: curr.code,
      name: curr.name
    }));
    if (currencies.value.length > 0 && !currencies.value.find(c => c.code === currencyCode.value)) {
      currencyCode.value = currencies.value[0].code;
    }
  } catch (e: any) {
    console.error('加载币种失败:', e);
  }
};

const load = async () => {
  const base = {
    startDate: startDate.value,
    endDate: endDate.value,
    currencyCode: currencyCode.value
  };
  const [pResp, bResp, cfResp] = await Promise.all([
    api.post('/reports/profit', { ...base, type: 'PROFIT' }),
    api.post('/reports/balance-sheet', { ...base, type: 'BALANCE_SHEET' }),
    api.post('/reports/cash-flow', { ...base, type: 'CASH_FLOW' })
  ]);
  profit.value = pResp.data;
  balance.value = bResp.data;
  cashFlow.value = cfResp.data;
};

onMounted(loadCurrencies);
</script>

<style scoped>
.currency-select {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s ease-out, box-shadow 0.15s ease-out;
  width: 100%;
  box-sizing: border-box;
}

.currency-select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
}
</style>


