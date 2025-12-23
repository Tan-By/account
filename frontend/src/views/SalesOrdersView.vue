<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">销售单管理（UC010）</div>
        <div class="page-header__subtitle">创建和管理销售订单，跟踪发货和收款状态</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary btn--strong" @click="openCreate">
          ＋ 新建销售单
        </button>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div class="card">
      <div class="card-subtitle" style="margin-bottom: 8px; font-size: 12px; color: var(--text-muted);">
        说明：本页进行<b>业务审核</b>、发货与收款；<b>会计审核与过账</b>请在「凭证过账」功能中完成。
      </div>
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="orders.length === 0" class="empty-state">暂无销售单数据</div>
      <table v-else>
        <thead>
          <tr>
            <th>销售单号</th>
            <th>客户</th>
            <th>下单日期</th>
            <th>交货日期</th>
            <th>应收总额</th>
            <th>已收金额</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.orderNumber }}</td>
            <td>{{ order.customerName }}</td>
            <td>{{ formatDate(order.orderDate) }}</td>
            <td>{{ formatDate(order.deliveryDate) }}</td>
            <td class="text-right">{{ formatAmount(order.receivableAmount) }}</td>
            <td class="text-right">{{ formatAmount(order.receivedAmount) }}</td>
            <td>
              <span class="badge" :class="getStatusBadgeClass(order.status)">
                {{ getStatusText(order.status) }}
              </span>
            </td>
            <td>
              <div style="display: flex; gap: 4px; flex-wrap: wrap;">
                <button class="btn btn--ghost" @click="view(order)">查看</button>
                <button
                  v-if="order.status === 'DRAFT'"
                  class="btn btn--ghost"
                  @click="edit(order)"
                >
                  编辑
                </button>
                <button
                  v-if="order.status === 'DRAFT'"
                  class="btn btn--ghost"
                  @click="submit(order)"
                >
                  提交审核
                </button>
                <button
                  v-if="order.status === 'PENDING_AUDIT'"
                  class="btn btn--ghost"
                  @click="openAudit(order)"
                >
                  审核
                </button>
                <button
                  v-if="order.status === 'AUDITED' || order.status === 'PARTIAL_SHIPMENT'"
                  class="btn btn--ghost"
                  @click="openShipment(order)"
                >
                  发货
                </button>
                <button
                  v-if="order.status === 'SHIPPED' || order.status === 'PARTIAL_PAYMENT' || order.status === 'AUDITED'"
                  class="btn btn--ghost"
                  @click="openPayment(order)"
                >
                  收款
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="card" style="margin-top: 10px; background: #fce8e6; border-color: #d93025;">
      <div style="color: #d93025; font-size: 13px;">{{ errorMessage }}</div>
    </div>

    <!-- 创建/编辑销售单：子窗口 -->
    <div v-if="editing" class="modal-overlay" @click.self="editing = null">
      <div class="modal-content modal-content--xl">
        <div class="modal-header">
          <div>
            <h3 class="modal-title">{{ editing.id ? '编辑销售单' : '新建销售单' }}</h3>
            <p class="modal-subtitle">填写客户、日期与地址信息，添加商品明细后保存</p>
          </div>
          <button class="modal-close" @click="editing = null">×</button>
        </div>
        <div class="modal-body">
          <div class="form-grid form-grid--four">
            <div class="form-item">
              <label class="form-label">客户 *</label>
              <select v-model.number="editing.customerId">
                <option :value="undefined">选择客户</option>
                <option v-for="c in customers" :key="c.id" :value="c.id">
                  {{ c.name }}
                </option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">下单日期 *</label>
              <input type="date" v-model="editing.orderDate" />
            </div>
            <div class="form-item">
              <label class="form-label">交货日期 *</label>
              <input type="date" v-model="editing.deliveryDate" />
            </div>
            <div class="form-item">
              <label class="form-label">发货地址</label>
              <input v-model="editing.shippingAddress" placeholder="如：XX省XX市XX路" />
            </div>
          </div>
          <div class="form-grid form-grid--two">
            <div class="form-item">
              <label class="form-label">收款方式</label>
              <select v-model="editing.paymentMethod">
                <option value="">选择收款方式</option>
                <option value="现金">现金</option>
                <option value="银行转账">银行转账</option>
                <option value="支票">支票</option>
                <option value="月结">月结</option>
              </select>
            </div>
          </div>

          <div class="modal-section">
            <div class="modal-section__header">
              <div>
                <div class="modal-section__title">订单明细</div>
                <div class="modal-section__desc">填写商品、数量、单价与折扣</div>
              </div>
              <button class="btn btn--ghost" @click="addItem">添加商品</button>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>商品名称</th>
                    <th>单位</th>
                    <th>数量</th>
                    <th>单价</th>
                    <th>折扣率</th>
                    <th>金额</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in editing.items" :key="idx">
                    <td>
                      <input 
                        type="text" 
                        v-model="item.productName" 
                        @input="calculateItemAmount(idx)" 
                        placeholder="输入商品名称"
                        style="width: 100%;"
                      />
                    </td>
                    <td>
                      <input 
                        type="text" 
                        v-model="item.unit" 
                        @input="calculateItemAmount(idx)" 
                        placeholder="单位"
                        style="width: 100%;"
                      />
                    </td>
                    <td>
                      <input 
                        type="number" 
                        v-model.number="item.quantity" 
                        step="1" 
                        min="1"
                        @input="calculateItemAmount(idx)" 
                        placeholder="数量"
                      />
                    </td>
                    <td>
                      <input 
                        type="number" 
                        v-model.number="item.unitPrice" 
                        step="0.01" 
                        min="0.01"
                        @input="calculateItemAmount(idx)" 
                        placeholder="单价"
                      />
                    </td>
                    <td>
                      <input 
                        type="number" 
                        v-model.number="item.discountRate" 
                        step="0.01" 
                        min="0" 
                        max="1" 
                        @input="calculateItemAmount(idx)" 
                        placeholder="0-1"
                      />
                      <div class="field-hint">
                        {{ item.discountRate ? `折扣: ${(item.discountRate * 100).toFixed(1)}%` : '' }}
                      </div>
                    </td>
                    <td class="text-right">{{ formatAmount(calculateItemAmount(idx)) }}</td>
                    <td>
                      <button class="btn btn--ghost" @click="removeItem(idx)">删除</button>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="5"><strong>订单总金额</strong></td>
                    <td class="text-right"><strong>{{ formatAmount(calculateTotal()) }}</strong></td>
                    <td></td>
                  </tr>
                  <tr>
                    <td colspan="5"><strong>折扣总额</strong></td>
                    <td class="text-right"><strong>{{ formatAmount(calculateDiscountTotal()) }}</strong></td>
                    <td></td>
                  </tr>
                  <tr>
                    <td colspan="5"><strong>应收总额</strong></td>
                    <td class="text-right"><strong>{{ formatAmount(calculateReceivableTotal()) }}</strong></td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <div class="modal-actions">
            <button class="btn btn--ghost" @click="editing = null">取消</button>
            <button class="btn btn--primary" @click="save">保存</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 审核对话框 -->
    <div v-if="auditingOrder" class="card" style="margin-top: 10px;">
      <div class="card-title">审核销售单：{{ auditingOrder.orderNumber }}</div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">审核意见</label>
          <textarea v-model="auditComment" rows="3"></textarea>
        </div>
      </div>
      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary" @click="doAudit(true)">通过</button>
        <button class="btn btn--danger" @click="doAudit(false)">拒绝</button>
        <button class="btn btn--ghost" @click="auditingOrder = null">取消</button>
      </div>
    </div>

    <!-- 发货对话框 -->
    <div v-if="shippingOrder" class="card" style="margin-top: 10px;">
      <div class="card-title">发货操作：{{ shippingOrder.orderNumber }}</div>
      <table>
        <thead>
          <tr>
            <th>商品</th>
            <th>销售数量</th>
            <th>已发货</th>
            <th>未发货</th>
            <th>本次发货数量</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in shippingOrder.items" :key="idx">
            <td>{{ item.productName }}</td>
            <td class="text-right">{{ item.quantity }}</td>
            <td class="text-right">{{ item.shippedQuantity }}</td>
            <td class="text-right">{{ item.unshippedQuantity }}</td>
            <td>
              <input
                type="number"
                v-model.number="shipmentQuantities[idx]"
                :max="item.unshippedQuantity"
                step="0.01"
              />
            </td>
          </tr>
        </tbody>
      </table>
      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary" @click="doShipment">确认发货</button>
        <button class="btn btn--ghost" @click="shippingOrder = null">取消</button>
      </div>
    </div>

    <!-- 收款对话框 -->
    <div v-if="payingOrder" class="card" style="margin-top: 10px;">
      <div class="card-title">收款操作：{{ payingOrder.orderNumber }}</div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">应收总额</label>
          <div>{{ formatAmount(payingOrder.receivableAmount) }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">已收金额</label>
          <div>{{ formatAmount(payingOrder.receivedAmount) }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">未收金额</label>
          <div>{{ formatAmount(payingOrder.unreceivedAmount) }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">本次收款金额 *</label>
          <input type="number" v-model.number="paymentAmount" step="0.01" />
        </div>
      </div>
      <div style="margin-top: 8px; display: flex; gap: 8px;">
        <button class="btn btn--primary" @click="doPayment">确认收款</button>
        <button class="btn btn--ghost" @click="payingOrder = null">取消</button>
      </div>
    </div>

    <!-- 查看详情对话框 -->
    <div v-if="viewingOrder" class="card" style="margin-top: 10px;">
      <div class="card-title">销售单详情：{{ viewingOrder.orderNumber }}</div>
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">客户</label>
          <div>{{ viewingOrder.customerName }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">下单日期</label>
          <div>{{ formatDate(viewingOrder.orderDate) }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">交货日期</label>
          <div>{{ formatDate(viewingOrder.deliveryDate) }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">状态</label>
          <div>
            <span class="badge" :class="getStatusBadgeClass(viewingOrder.status)">
              {{ getStatusText(viewingOrder.status) }}
            </span>
          </div>
        </div>
      </div>
      <table style="margin-top: 16px;">
        <thead>
          <tr>
            <th>商品编码</th>
            <th>商品名称</th>
            <th>数量</th>
            <th>单价</th>
            <th>折扣率</th>
            <th>金额</th>
            <th>已发货</th>
            <th>未发货</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in viewingOrder.items" :key="item.id">
            <td>{{ item.productCode }}</td>
            <td>{{ item.productName }}</td>
            <td class="text-right">{{ item.quantity }}</td>
            <td class="text-right">{{ formatAmount(item.unitPrice) }}</td>
            <td class="text-right">{{ (item.discountRate * 100).toFixed(2) }}%</td>
            <td class="text-right">{{ formatAmount(item.amount) }}</td>
            <td class="text-right">{{ item.shippedQuantity }}</td>
            <td class="text-right">{{ item.unshippedQuantity }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="5"><strong>订单总金额</strong></td>
            <td class="text-right"><strong>{{ formatAmount(viewingOrder.totalAmount) }}</strong></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td colspan="5"><strong>折扣总额</strong></td>
            <td class="text-right"><strong>{{ formatAmount(viewingOrder.discountAmount) }}</strong></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td colspan="5"><strong>应收总额</strong></td>
            <td class="text-right"><strong>{{ formatAmount(viewingOrder.receivableAmount) }}</strong></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td colspan="5"><strong>已收金额</strong></td>
            <td class="text-right"><strong>{{ formatAmount(viewingOrder.receivedAmount) }}</strong></td>
            <td colspan="2"></td>
          </tr>
          <tr>
            <td colspan="5"><strong>未收金额</strong></td>
            <td class="text-right"><strong>{{ formatAmount(viewingOrder.unreceivedAmount) }}</strong></td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>
      <div style="margin-top: 8px;">
        <button class="btn btn--ghost" @click="viewingOrder = null">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { salesOrderApi, productApi } from '../api';
import { api } from '../api';

interface SalesOrder {
  id?: number;
  orderNumber?: string;
  customerId?: number;
  customerName?: string;
  orderDate?: string;
  deliveryDate?: string;
  shippingAddress?: string;
  paymentMethod?: string;
  totalAmount?: number;
  discountAmount?: number;
  receivableAmount?: number;
  receivedAmount?: number;
  unreceivedAmount?: number;
  status?: string;
  items?: SalesOrderItem[];
}

interface SalesOrderItem {
  productId?: number;
  quantity?: number;
  unitPrice?: number;
  discountRate?: number;
}

interface Customer {
  id: number;
  name: string;
}

interface Product {
  id: number;
  code: string;
  name: string;
}

const orders = ref<SalesOrder[]>([]);
const customers = ref<Customer[]>([]);
const products = ref<Product[]>([]);
const editing = ref<SalesOrder | null>(null);
const auditingOrder = ref<SalesOrder | null>(null);
const shippingOrder = ref<SalesOrder | null>(null);
const payingOrder = ref<SalesOrder | null>(null);
const viewingOrder = ref<SalesOrder | null>(null);
const auditComment = ref('');
const shipmentQuantities = ref<number[]>([]);
const paymentAmount = ref<number>(0);
const loading = ref(false);
const errorMessage = ref('');

const load = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await salesOrderApi.list();
    orders.value = response.data;
  } catch (error: any) {
    console.error('加载销售单失败:', error);
    errorMessage.value = error.response?.data?.message || '加载销售单失败';
    alert(errorMessage.value);
  } finally {
    loading.value = false;
  }
};

const loadCustomers = async () => {
  try {
    const response = await api.get('/external-contacts', { params: { type: '客户', status: '活跃' } });
    customers.value = response.data;
  } catch (error) {
    console.error('加载客户失败:', error);
  }
};

const loadProducts = async () => {
  try {
    const response = await productApi.list(true);
    products.value = response.data;
  } catch (error) {
    console.error('加载商品失败:', error);
  }
};

const openCreate = () => {
  editing.value = {
    orderDate: new Date().toISOString().split('T')[0],
    deliveryDate: new Date().toISOString().split('T')[0],
    items: []
  };
};

const addItem = () => {
  if (!editing.value) return;
  if (!editing.value.items) {
    editing.value.items = [];
  }
  editing.value.items.push({
    productName: '',
    unit: '',
    quantity: 1,
    unitPrice: 0,
    discountRate: 0
  });
};

const removeItem = (idx: number) => {
  if (!editing.value?.items) return;
  editing.value.items.splice(idx, 1);
};

const calculateItemAmount = (idx: number): number => {
  if (!editing.value?.items) return 0;
  const item = editing.value.items[idx];
  if (!item) return 0;
  const quantity = item.quantity || 0;
  const unitPrice = item.unitPrice || 0;
  const discountRate = item.discountRate || 0;
  return Number(quantity) * Number(unitPrice) * (1 - Number(discountRate));
};

const calculateTotal = (): number => {
  if (!editing.value?.items) return 0;
  return editing.value.items.reduce((sum, item) => {
    return sum + Number(item.quantity || 0) * Number(item.unitPrice || 0);
  }, 0);
};

const calculateDiscountTotal = (): number => {
  if (!editing.value?.items) return 0;
  return editing.value.items.reduce((sum, item) => {
    const quantity = Number(item.quantity || 0);
    const unitPrice = Number(item.unitPrice || 0);
    const discountRate = Number(item.discountRate || 0);
    return sum + quantity * unitPrice * discountRate;
  }, 0);
};

const calculateReceivableTotal = (): number => {
  return calculateTotal() - calculateDiscountTotal();
};

const save = async () => {
  if (!editing.value) return;
  
  // 验证必填项
  if (!editing.value.customerId || !editing.value.orderDate || !editing.value.deliveryDate) {
    alert('请填写必填项（客户、下单日期、交货日期）');
    return;
  }
  
  // 验证订单明细
  if (!editing.value.items || editing.value.items.length === 0) {
    alert('请至少添加一个商品明细');
    return;
  }
  
  // 验证每个明细项
  for (let i = 0; i < editing.value.items.length; i++) {
    const item = editing.value.items[i];
    if (!item.productName || !item.productName.trim()) {
      alert(`第${i + 1}行：请输入商品名称`);
      return;
    }
    if (!item.unit || !item.unit.trim()) {
      alert(`第${i + 1}行：请输入商品单位`);
      return;
    }
    if (!item.quantity || item.quantity <= 0 || !Number.isInteger(item.quantity)) {
      alert(`第${i + 1}行：请输入有效的销售数量（整数）`);
      return;
    }
    if (!item.unitPrice || item.unitPrice <= 0) {
      alert(`第${i + 1}行：请输入有效的销售单价`);
      return;
    }
    if (item.discountRate && (item.discountRate < 0 || item.discountRate > 1)) {
      alert(`第${i + 1}行：折扣率必须在0-1之间（如0.1表示10%折扣）`);
      return;
    }
  }

  try {
    const data = {
      customerId: editing.value.customerId,
      orderDate: editing.value.orderDate,
      deliveryDate: editing.value.deliveryDate,
      shippingAddress: editing.value.shippingAddress,
      paymentMethod: editing.value.paymentMethod,
      items: editing.value.items.map(item => ({
        productName: item.productName.trim(),
        unit: item.unit.trim(),
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discountRate: item.discountRate || 0
      }))
    };

    if (editing.value.id) {
      // 更新逻辑（如果需要）
      alert('编辑功能待实现');
    } else {
      await salesOrderApi.create(data);
      alert('销售单创建成功');
      editing.value = null;
      await load();
    }
  } catch (error: any) {
    console.error('保存失败:', error);
    const errorMsg = error.response?.data?.message || error.message || '保存失败';
    alert(errorMsg);
  }
};

const edit = (order: SalesOrder) => {
  editing.value = { ...order };
};

const submit = async (order: SalesOrder) => {
  if (!confirm('确认提交审核？')) return;
  try {
    await salesOrderApi.submit(order.id!);
    alert('提交成功');
    load();
  } catch (error: any) {
    console.error('提交失败:', error);
    alert(error.response?.data?.message || '提交失败');
  }
};

const openAudit = (order: SalesOrder) => {
  auditingOrder.value = order;
  auditComment.value = '';
};

const doAudit = async (approved: boolean) => {
  if (!auditingOrder.value) return;
  try {
    await salesOrderApi.audit(auditingOrder.value.id!, {
      approved,
      comment: auditComment.value
    });
    alert(approved ? '审核通过' : '审核拒绝');
    auditingOrder.value = null;
    load();
  } catch (error: any) {
    console.error('审核失败:', error);
    alert(error.response?.data?.message || '审核失败');
  }
};

const openShipment = (order: SalesOrder) => {
  shippingOrder.value = order;
  shipmentQuantities.value = order.items?.map(() => 0) || [];
};

const doShipment = async () => {
  if (!shippingOrder.value) return;
  
  // 验证发货数量
  const shipments = shippingOrder.value.items!.map((item, idx) => ({
    itemId: item.id,
    quantity: shipmentQuantities.value[idx] || 0
  })).filter(s => s.quantity > 0);

  if (shipments.length === 0) {
    alert('请至少输入一个发货数量');
    return;
  }

  // 验证发货数量不超过未发货数量
  for (let i = 0; i < shippingOrder.value.items!.length; i++) {
    const item = shippingOrder.value.items![i];
    const qty = shipmentQuantities.value[i] || 0;
    if (qty > 0 && qty > item.unshippedQuantity!) {
      alert(`${item.productName} 的发货数量不能超过未发货数量 ${item.unshippedQuantity}`);
      return;
    }
  }

  try {
    await salesOrderApi.ship(shippingOrder.value.id!, shipments);
    alert('发货成功');
    shippingOrder.value = null;
    await load();
  } catch (error: any) {
    console.error('发货失败:', error);
    const errorMsg = error.response?.data?.message || error.message || '发货失败';
    alert(errorMsg);
  }
};

const openPayment = (order: SalesOrder) => {
  payingOrder.value = order;
  paymentAmount.value = 0;
};

const doPayment = async () => {
  if (!payingOrder.value) return;
  
  if (!paymentAmount.value || paymentAmount.value <= 0) {
    alert('请输入有效的收款金额');
    return;
  }
  
  if (paymentAmount.value > payingOrder.value.unreceivedAmount!) {
    alert(`收款金额不能超过未收金额 ${formatAmount(payingOrder.value.unreceivedAmount)}`);
    return;
  }
  
  try {
    await salesOrderApi.payment(payingOrder.value.id!, {
      amount: paymentAmount.value
    });
    alert('收款成功');
    payingOrder.value = null;
    await load();
  } catch (error: any) {
    console.error('收款失败:', error);
    const errorMsg = error.response?.data?.message || error.message || '收款失败';
    alert(errorMsg);
  }
};

const view = (order: SalesOrder) => {
  viewingOrder.value = order;
};

const formatDate = (date?: string) => {
  if (!date) return '-';
  return new Date(date).toLocaleDateString('zh-CN');
};

const formatAmount = (amount?: number) => {
  if (amount === undefined || amount === null) return '0.00';
  return amount.toFixed(2);
};

const getStatusText = (status?: string) => {
  const statusMap: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_AUDIT: '待审核',
    AUDITED: '已审核',
    REJECTED: '已拒绝',
    PARTIAL_SHIPMENT: '部分发货',
    SHIPPED: '已发货',
    PARTIAL_PAYMENT: '部分收款',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  };
  return statusMap[status || ''] || status || '-';
};

const getStatusBadgeClass = (status?: string) => {
  const classMap: Record<string, string> = {
    DRAFT: 'badge--warn',
    PENDING_AUDIT: 'badge--info',
    AUDITED: 'badge--ok',
    REJECTED: 'badge--error',
    PARTIAL_SHIPMENT: 'badge--info',
    SHIPPED: 'badge--ok',
    PARTIAL_PAYMENT: 'badge--info',
    COMPLETED: 'badge--ok',
    CANCELLED: 'badge--warn'
  };
  return classMap[status || ''] || '';
};

onMounted(() => {
  load();
  loadCustomers();
  loadProducts();
});
</script>

<style scoped>
.text-right {
  text-align: right;
}
</style>

