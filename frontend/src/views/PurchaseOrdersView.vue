<template>
  <div>
    <div class="page-header">
      <div>
        <div class="page-header__title">订货单管理（UC009）</div>
        <div class="page-header__subtitle">创建和管理采购订货单，跟踪订单执行状态</div>
      </div>
      <div class="toolbar">
        <button class="btn btn--primary btn--strong" @click="openCreate">
          ＋ 新建订货单
        </button>
        <button class="btn btn--ghost" @click="load">刷新</button>
      </div>
    </div>

    <div class="card card--panel fade-in hover-lift">
      <div class="card-subtitle" style="margin-bottom: 8px;">
        说明：本页进行<b>业务审批</b>和收货操作；<b>会计审核与过账</b>请在「凭证过账」功能中完成。
      </div>
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="orders.length === 0" class="empty-hero">
        <div class="empty-hero__icon">📦</div>
        <div class="empty-hero__title">暂无订货单数据</div>
        <div class="empty-hero__subtitle">
          点击右上角「＋ 新建订货单」开始创建第一张采购订单。
        </div>
        <button class="btn btn--primary btn--pill empty-hero__action" @click="openCreate">
          新建订货单
        </button>
      </div>
      <table v-else class="sheet-table table-compact table-quiet">
        <thead>
          <tr>
            <th>订单编号</th>
            <th>供货商</th>
            <th>订单日期</th>
            <th>交货日期</th>
            <th>订单金额</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>{{ order.orderNumber }}</td>
            <td>{{ order.supplierName }}</td>
            <td>{{ formatDate(order.orderDate) }}</td>
            <td>{{ formatDate(order.deliveryDate) }}</td>
            <td class="text-right numeric">{{ formatAmount(order.totalAmount) }}</td>
            <td>
              <span class="badge" :class="getStatusBadgeClass(order.status)">
                {{ getStatusText(order.status) }}
              </span>
            </td>
            <td>
              <div style="display: flex; gap: 4px; flex-wrap: wrap;">
                <button class="btn btn--ghost btn--pill btn--small" @click="view(order)">查看</button>
                <button
                  v-if="order.status === 'DRAFT'"
                  class="btn btn--ghost btn--pill btn--small"
                  @click="edit(order)"
                >
                  编辑
                </button>
                <button
                  v-if="order.status === 'DRAFT'"
                  class="btn btn--ghost btn--pill btn--small"
                  @click="submit(order)"
                >
                  提交审批
                </button>
                <button
                  v-if="order.status === 'PENDING_APPROVAL'"
                  class="btn btn--ghost btn--pill btn--small"
                  @click="openApprove(order)"
                >
                  审批
                </button>
                <button
                  v-if="order.status === 'APPROVED' || order.status === 'PARTIAL_DELIVERY'"
                  class="btn btn--ghost btn--pill btn--small"
                  @click="openDelivery(order)"
                >
                  收货
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="card card--panel fade-in" style="margin-top: 10px; background: #fff5f4; border-color: var(--negative); color: var(--negative);">
      <div style="font-size: 13px;">{{ errorMessage }}</div>
    </div>

    <!-- 创建/编辑订货单：子窗口 -->
    <div v-if="editing" class="modal-overlay" @click.self="editing = null">
      <div class="modal-content modal-content--xl pop-in">
        <div class="modal-header">
          <h3 class="modal-title">{{ editing.id ? '编辑订货单' : '新建订货单' }}</h3>
          <button class="modal-close" @click="editing = null">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-col">
              <label class="form-label">供货商 *</label>
              <select v-model.number="editing.supplierId">
                <option :value="undefined">选择供货商</option>
                <option v-for="s in suppliers" :key="s.id" :value="s.id">
                  {{ s.name }}
                </option>
              </select>
            </div>
            <div class="form-col">
              <label class="form-label">订单日期 *</label>
              <input type="date" v-model="editing.orderDate" />
            </div>
            <div class="form-col">
              <label class="form-label">交货日期 *</label>
              <input type="date" v-model="editing.deliveryDate" />
            </div>
          </div>

          <div style="margin-top: 16px;">
            <div class="card__section-head" style="margin-bottom: 8px;">
              <strong>订单明细</strong>
              <button class="btn btn--ghost btn--pill btn--small" @click="addItem">添加商品</button>
            </div>
            <table class="sheet-table table-compact table-quiet">
              <thead>
                <tr>
                  <th>商品名称</th>
                  <th>单位</th>
                  <th>数量</th>
                  <th>单价</th>
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
                      style="width: 80px;"
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
                  <td class="text-right numeric">{{ formatAmount(Number(item.quantity || 0) * Number(item.unitPrice || 0)) }}</td>
                  <td>
                    <button class="btn btn--ghost" @click="removeItem(idx)">删除</button>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="3"><strong>订单总金额</strong></td>
                  <td class="text-right numeric"><strong>{{ formatAmount(calculateTotal()) }}</strong></td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn--ghost" @click="editing = null">取消</button>
          <button class="btn btn--primary" @click="save">保存</button>
        </div>
      </div>
    </div>

    <!-- 审批对话框 -->
    <div v-if="approvingOrder" class="modal-overlay" @click.self="approvingOrder = null">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <h3 class="modal-title">审批订货单：{{ approvingOrder.orderNumber }}</h3>
          <button class="modal-close" @click="approvingOrder = null">×</button>
        </div>
        <div class="modal-body">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">审批意见</label>
              <textarea v-model="approvalComment" rows="3" class="form-input"></textarea>
            </div>
          </div>
        </div>
        <div class="modal-footer">
        <button class="btn btn--primary" @click="doApprove(true)">批准</button>
        <button class="btn btn--danger" @click="doApprove(false)">拒绝</button>
        <button class="btn btn--ghost" @click="approvingOrder = null">取消</button>
        </div>
      </div>
    </div>

    <!-- 收货对话框 -->
    <div v-if="deliveringOrder" class="modal-overlay" @click.self="deliveringOrder = null">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <h3 class="modal-title">收货操作：{{ deliveringOrder.orderNumber }}</h3>
          <button class="modal-close" @click="deliveringOrder = null">×</button>
        </div>
        <div class="modal-body">
      <table>
        <thead>
          <tr>
            <th>商品</th>
            <th>订购数量</th>
            <th>已交货</th>
            <th>未交货</th>
            <th>本次交货数量</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in deliveringOrder.items" :key="idx">
            <td>{{ item.productName }}</td>
            <td class="text-right">{{ item.quantity }}</td>
            <td class="text-right">{{ item.deliveredQuantity }}</td>
            <td class="text-right">{{ item.undeliveredQuantity }}</td>
            <td>
              <input
                type="number"
                v-model.number="deliveryQuantities[idx]"
                :max="item.undeliveredQuantity"
                step="0.01"
                    class="form-input"
              />
            </td>
          </tr>
        </tbody>
      </table>
        </div>
        <div class="modal-footer">
        <button class="btn btn--primary" @click="doDelivery">确认收货</button>
        <button class="btn btn--ghost" @click="deliveringOrder = null">取消</button>
        </div>
      </div>
    </div>

    <!-- 查看详情对话框 -->
    <div v-if="viewingOrder" class="modal-overlay" @click.self="viewingOrder = null">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <h3 class="modal-title">订货单详情：{{ viewingOrder.orderNumber }}</h3>
          <button class="modal-close" @click="viewingOrder = null">×</button>
        </div>
        <div class="modal-body">
      <div class="form-row">
        <div class="form-col">
          <label class="form-label">供货商</label>
          <div>{{ viewingOrder.supplierName }}</div>
        </div>
        <div class="form-col">
          <label class="form-label">订单日期</label>
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
            <th>金额</th>
            <th>已交货</th>
            <th>未交货</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in viewingOrder.items" :key="item.id">
            <td>{{ item.productCode }}</td>
            <td>{{ item.productName }}</td>
            <td class="text-right">{{ item.quantity }}</td>
            <td class="text-right">{{ formatAmount(item.unitPrice) }}</td>
            <td class="text-right">{{ formatAmount(item.amount) }}</td>
            <td class="text-right">{{ item.deliveredQuantity }}</td>
            <td class="text-right">{{ item.undeliveredQuantity }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="4"><strong>订单总金额</strong></td>
            <td class="text-right"><strong>{{ formatAmount(viewingOrder.totalAmount) }}</strong></td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>
        </div>
        <div class="modal-footer">
        <button class="btn btn--ghost" @click="viewingOrder = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { purchaseOrderApi, productApi, warehouseApi } from '../api';
import { api } from '../api';

interface PurchaseOrder {
  id?: number;
  orderNumber?: string;
  supplierId?: number;
  supplierName?: string;
  orderDate?: string;
  deliveryDate?: string;
  warehouseId?: number;
  warehouseName?: string;
  totalAmount?: number;
  status?: string;
  items?: PurchaseOrderItem[];
}

interface PurchaseOrderItem {
  productId?: number;
  quantity?: number;
  unitPrice?: number;
}

interface Supplier {
  id: number;
  name: string;
}

interface Product {
  id: number;
  code: string;
  name: string;
  lastPurchasePrice?: number;
}

const orders = ref<PurchaseOrder[]>([]);
const suppliers = ref<Supplier[]>([]);
const products = ref<Product[]>([]);
const editing = ref<PurchaseOrder | null>(null);
const approvingOrder = ref<PurchaseOrder | null>(null);
const deliveringOrder = ref<PurchaseOrder | null>(null);
const viewingOrder = ref<PurchaseOrder | null>(null);
const approvalComment = ref('');
const deliveryQuantities = ref<number[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const load = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await purchaseOrderApi.list();
    orders.value = response.data;
  } catch (error: any) {
    console.error('加载订货单失败:', error);
    errorMessage.value = error.response?.data?.message || '加载订货单失败';
    alert(errorMessage.value);
  } finally {
    loading.value = false;
  }
};

const loadSuppliers = async () => {
  try {
    const response = await api.get('/external-contacts', { params: { type: '供应商', status: '活跃' } });
    suppliers.value = response.data;
  } catch (error) {
    console.error('加载供货商失败:', error);
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

// 为后续扩展预留：加载可用仓库，避免挂载时调用未定义函数导致页面报错
const loadWarehouses = async () => {
  try {
    // 当前界面暂未直接使用仓库列表，仅为后续“收货入库”等扩展预留
    await warehouseApi.list(true);
  } catch (error) {
    console.error('加载仓库失败:', error);
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
    unitPrice: 0
  });
};

const removeItem = (idx: number) => {
  if (!editing.value?.items) return;
  editing.value.items.splice(idx, 1);
};

// 移除onProductChange方法，因为不再需要选择商品

const calculateItemAmount = (idx: number) => {
  // 金额自动计算，这里只是触发响应式更新
};

const calculateTotal = (): number => {
  if (!editing.value?.items) return 0;
  return editing.value.items.reduce((sum, item) => {
    return sum + Number(item.quantity || 0) * Number(item.unitPrice || 0);
  }, 0);
};

const save = async () => {
  if (!editing.value) return;
  
  // 验证必填项
  if (!editing.value.supplierId || !editing.value.orderDate || !editing.value.deliveryDate) {
    alert('请填写必填项（供货商、订单日期、交货日期）');
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
      alert(`第${i + 1}行：请输入有效的订购数量（整数）`);
      return;
    }
    if (!item.unitPrice || item.unitPrice <= 0) {
      alert(`第${i + 1}行：请输入有效的单价`);
      return;
    }
  }

  try {
    const data = {
      supplierId: editing.value.supplierId,
      orderDate: editing.value.orderDate,
      deliveryDate: editing.value.deliveryDate,
      items: editing.value.items.map(item => ({
        productName: item.productName.trim(),
        unit: item.unit.trim(),
        quantity: item.quantity,
        unitPrice: item.unitPrice
      }))
    };

    if (editing.value.id) {
      // 更新逻辑（如果需要）
      alert('编辑功能待实现');
    } else {
      await purchaseOrderApi.create(data);
      alert('订货单创建成功');
      editing.value = null;
      await load();
    }
  } catch (error: any) {
    console.error('保存失败:', error);
    const errorMsg = error.response?.data?.message || error.message || '保存失败';
    alert(errorMsg);
  }
};

const edit = (order: PurchaseOrder) => {
  editing.value = { ...order };
};

const submit = async (order: PurchaseOrder) => {
  if (!confirm('确认提交审批？')) return;
  try {
    await purchaseOrderApi.submit(order.id!);
    alert('提交成功');
    load();
  } catch (error: any) {
    console.error('提交失败:', error);
    alert(error.response?.data?.message || '提交失败');
  }
};

const openApprove = (order: PurchaseOrder) => {
  approvingOrder.value = order;
  approvalComment.value = '';
};

const doApprove = async (approved: boolean) => {
  if (!approvingOrder.value) return;
  try {
    await purchaseOrderApi.approve(approvingOrder.value.id!, {
      approved,
      comment: approvalComment.value
    });
    alert(approved ? '审批通过' : '审批拒绝');
    approvingOrder.value = null;
    load();
  } catch (error: any) {
    console.error('审批失败:', error);
    alert(error.response?.data?.message || '审批失败');
  }
};

const openDelivery = (order: PurchaseOrder) => {
  deliveringOrder.value = order;
  deliveryQuantities.value = order.items?.map(() => 0) || [];
};

const doDelivery = async () => {
  if (!deliveringOrder.value) return;
  
  // 验证交货数量
  const deliveries = deliveringOrder.value.items!.map((item, idx) => ({
    itemId: item.id,
    quantity: deliveryQuantities.value[idx] || 0
  })).filter(d => d.quantity > 0);

  if (deliveries.length === 0) {
    alert('请至少输入一个交货数量');
    return;
  }

  // 验证交货数量不超过未交货数量
  for (let i = 0; i < deliveringOrder.value.items!.length; i++) {
    const item = deliveringOrder.value.items![i];
    const qty = deliveryQuantities.value[i] || 0;
    if (qty > 0 && qty > item.undeliveredQuantity!) {
      alert(`${item.productName} 的交货数量不能超过未交货数量 ${item.undeliveredQuantity}`);
      return;
    }
  }

  try {
    await purchaseOrderApi.deliver(deliveringOrder.value.id!, deliveries);
    alert('收货成功');
    deliveringOrder.value = null;
    await load();
  } catch (error: any) {
    console.error('收货失败:', error);
    const errorMsg = error.response?.data?.message || error.message || '收货失败';
    alert(errorMsg);
  }
};

const view = (order: PurchaseOrder) => {
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
    PENDING_APPROVAL: '待审批',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
    PARTIAL_DELIVERY: '部分交货',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  };
  return statusMap[status || ''] || status || '-';
};

const getStatusBadgeClass = (status?: string) => {
  const classMap: Record<string, string> = {
    DRAFT: 'badge--warn',
    PENDING_APPROVAL: 'badge--info',
    APPROVED: 'badge--ok',
    REJECTED: 'badge--error',
    PARTIAL_DELIVERY: 'badge--info',
    COMPLETED: 'badge--ok',
    CANCELLED: 'badge--warn'
  };
  return classMap[status || ''] || '';
};

onMounted(() => {
  load();
  loadSuppliers();
  loadProducts();
  loadWarehouses();
});
</script>

<style scoped>
.text-right {
  text-align: right;
}

/* 统一子窗口模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  width: 95%;
  max-width: 720px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-subtle);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-muted);
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.15s ease-out, color 0.15s ease-out;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  background: rgba(60, 64, 67, 0.1);
  color: var(--text-main);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-subtle);
  background: #f8f9fa;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.1);
}
</style>

