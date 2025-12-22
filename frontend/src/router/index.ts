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

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'dashboard', component: Dashboard },
  { path: '/init', name: 'init', component: InitCompany },
  { path: '/accounts', name: 'accounts', component: AccountsView },
  { path: '/transactions', name: 'transactions', component: TransactionsView },
  { path: '/reports', name: 'reports', component: ReportsView },
  { path: '/reconciliation', name: 'reconciliation', component: ReconciliationView },
  { path: '/tax', name: 'tax', component: TaxView },
  { path: '/users', name: 'users', component: UsersView },
  { path: '/contacts', name: 'contacts', component: ContactsView }
];

export default routes;


