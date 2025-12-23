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

    <div class="card card--panel fade-in hover-lift">
      <template v-if="contacts.length === 0">
        <div class="empty-hero">
          <div class="empty-hero__icon">🤝</div>
          <div class="empty-hero__title">暂无外部联系人</div>
          <div class="empty-hero__subtitle">
            点击右上角「新增联系人」，维护供应商与客户信息，方便后续业务单据选择。
          </div>
          <button class="btn btn--primary btn--pill empty-hero__action" @click="openCreate">
            新增联系人
          </button>
        </div>
      </template>
      <template v-else>
        <table class="sheet-table table-compact table-quiet">
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
              <td class="text-right">{{ c.taxId || '-' }}</td>
              <td class="text-right">{{ c.phone || '-' }}</td>
              <td>
                <span class="badge" :class="c.status === '活跃' ? 'badge--ok' : 'badge--warn'">
                  {{ c.status }}
                </span>
              </td>
              <td>
                <button class="btn btn--ghost btn--pill btn--small" @click="edit(c)">编辑</button>
                <button
                  v-if="c.status === '活跃'"
                  class="btn btn--ghost btn--pill btn--small"
                  @click="deactivate(c)"
                >
                  停用
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </div>

    <!-- 编辑/新增联系人子窗口 -->
    <div v-if="editing" class="modal-overlay" @click.self="editing = null">
      <div class="modal-content modal-content--md pop-in">
        <div class="modal-header">
          <div>
            <h3 class="modal-title">{{ editing.id ? '编辑联系人' : '新增联系人' }}</h3>
            <p class="modal-subtitle">完善联系人基础信息，便于后续单据选择</p>
          </div>
          <button class="modal-close" @click="editing = null">×</button>
        </div>
        <div class="modal-body">
          <div class="form-grid form-grid--three">
            <div class="form-item">
              <label class="form-label">名称 *</label>
              <input v-model="editing.name" placeholder="请输入联系人名称" />
            </div>
            <div class="form-item">
              <label class="form-label">类型 *</label>
              <select v-model="editing.type">
                <option value="供应商">供应商</option>
                <option value="客户">客户</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">纳税人识别号</label>
              <input v-model="editing.taxId" placeholder="选填" />
            </div>
          </div>
          <div class="form-grid form-grid--three">
            <div class="form-item">
              <label class="form-label">电话</label>
              <input v-model="editing.phone" placeholder="联系人电话" />
            </div>
            <div class="form-item">
              <label class="form-label">开户行</label>
              <input v-model="editing.bankName" placeholder="如：招商银行" />
            </div>
            <div class="form-item">
              <label class="form-label">银行账号</label>
              <input v-model="editing.bankAccount" placeholder="银行卡号" />
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


