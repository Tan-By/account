import axios from 'axios';

export const api = axios.create({
  baseURL: '/api'
});

// 请求拦截器：自动添加JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：处理401未授权错误
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // token过期或无效，清除token并跳转到登录页
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 登录函数
export const login = async (username: string, password: string) => {
  const response = await api.post('/auth/login', { username, password });
  const { token, username: user, name } = response.data;
  localStorage.setItem('token', token);
  localStorage.setItem('username', user);
  localStorage.setItem('name', name);
  return response.data;
};

// 登出函数
export const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('name');
};

// 订货单API
export const purchaseOrderApi = {
  list: () => api.get('/purchase-orders'),
  get: (id: number) => api.get(`/purchase-orders/${id}`),
  create: (data: any) => api.post('/purchase-orders', data),
  submit: (id: number) => api.post(`/purchase-orders/${id}/submit`),
  approve: (id: number, data: any) => api.post(`/purchase-orders/${id}/approve`, data),
  deliver: (id: number, data: any[]) => api.post(`/purchase-orders/${id}/deliver`, data),
};

// 销售单API
export const salesOrderApi = {
  list: () => api.get('/sales-orders'),
  get: (id: number) => api.get(`/sales-orders/${id}`),
  create: (data: any) => api.post('/sales-orders', data),
  submit: (id: number) => api.post(`/sales-orders/${id}/submit`),
  audit: (id: number, data: any) => api.post(`/sales-orders/${id}/audit`, data),
  ship: (id: number, data: any[]) => api.post(`/sales-orders/${id}/ship`, data),
  payment: (id: number, data: any) => api.post(`/sales-orders/${id}/payment`, data),
};

// 商品API
export const productApi = {
  list: (enabled?: boolean) => api.get('/products', { params: { enabled } }),
  get: (id: number) => api.get(`/products/${id}`),
  create: (data: any) => api.post('/products', data),
  update: (id: number, data: any) => api.put(`/products/${id}`, data),
};

// 仓库API
export const warehouseApi = {
  list: (enabled?: boolean) => api.get('/warehouses', { params: { enabled } }),
  get: (id: number) => api.get(`/warehouses/${id}`),
  create: (data: any) => api.post('/warehouses', data),
  update: (id: number, data: any) => api.put(`/warehouses/${id}`, data),
};


