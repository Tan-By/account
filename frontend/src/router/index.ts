import type { RouteRecordRaw } from 'vue-router';
import Dashboard from '../views/DashboardView.vue';
import InitCompany from '../views/InitCompanyView.vue';
import AccountsView from '../views/AccountsView.vue';
import TransactionsView from '../views/TransactionsView.vue';
import ReportsView from '../views/ReportsView.vue';
import ReconciliationView from '../views/ReconciliationView.vue';
import TaxView from '../views/TaxView.vue';
import UsersView from '../views/UsersView.vue';
import ContactsView from '../views/ContactsView.vue';
import PostingView from '../views/PostingView.vue';
import LoginView from '../views/LoginView.vue';
import PurchaseOrdersView from '../views/PurchaseOrdersView.vue';
import SalesOrdersView from '../views/SalesOrdersView.vue';

const routes: RouteRecordRaw[] = [
  { 
    path: '/login', 
    name: 'login', 
    component: LoginView,
    meta: { requiresAuth: false }
  },
  { 
    path: '/', 
    name: 'dashboard', 
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  { 
    path: '/init', 
    name: 'init', 
    component: InitCompany,
    meta: { requiresAuth: true }
  },
  { 
    path: '/accounts', 
    name: 'accounts', 
    component: AccountsView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/transactions', 
    name: 'transactions', 
    component: TransactionsView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/posting', 
    name: 'posting', 
    component: PostingView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/reports', 
    name: 'reports', 
    component: ReportsView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/reconciliation', 
    name: 'reconciliation', 
    component: ReconciliationView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/tax', 
    name: 'tax', 
    component: TaxView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/users', 
    name: 'users', 
    component: UsersView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/contacts', 
    name: 'contacts', 
    component: ContactsView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/purchase-orders', 
    name: 'purchase-orders', 
    component: PurchaseOrdersView,
    meta: { requiresAuth: true }
  },
  { 
    path: '/sales-orders', 
    name: 'sales-orders', 
    component: SalesOrdersView,
    meta: { requiresAuth: true }
  }
];

export default routes;


