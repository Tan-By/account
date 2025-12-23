import axios from 'axios';

export const api = axios.create({
  baseURL: '/api'
});

// 请求拦截器：自动添加JWT token
api.interceptors.request.use(
  (config) => {
    // 登录接口不需要添加 token
    if (config.url?.includes('/auth/login')) {
      console.log('🔓 登录请求，不添加 Authorization header');
      return config;
    }
    
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('🔐 添加 Authorization header:', token.substring(0, 20) + '...');
    } else {
      console.log('⚠️ 无 token，不添加 Authorization header');
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：处理401未授权错误
api.interceptors.response.use(
  (response) => {
    // 登录成功响应，不进行任何处理
    if (response.config.url?.includes('/auth/login')) {
      console.log('✅ 登录响应成功，状态码:', response.status);
      return response;
    }
    return response;
  },
  (error) => {
    // 登录接口的错误不应该触发401处理逻辑
    if (error.config?.url?.includes('/auth/login')) {
      console.log('❌ 登录请求失败:', error.response?.status, error.response?.data);
      return Promise.reject(error);
    }
    
    if (error.response?.status === 401) {
      console.log('🔒 401 未授权错误，清除 token 并跳转到登录页');
      // token过期或无效，清除token并跳转到登录页
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('name');
      // 避免在登录页重复跳转
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// 登录函数
export const login = async (username: string, password: string) => {
  console.log('📤 发送登录请求:', { username });
  
  const response = await api.post('/auth/login', { username, password });
  console.log('📥 登录响应:', {
    status: response.status,
    data: response.data,
    headers: response.headers
  });
  
  // 确保响应数据存在
  if (!response.data) {
    console.error('❌ 登录响应数据为空');
    throw new Error('登录响应数据为空');
  }
  
  const { token, username: user, name } = response.data;
  console.log('🔑 解析响应数据:', { 
    hasToken: !!token, 
    tokenLength: token?.length,
    username: user,
    name 
  });
  
  // 验证 token 是否存在
  if (!token) {
    console.error('❌ Token 不存在于响应中:', response.data);
    throw new Error('登录失败：未收到认证令牌');
  }
  
  // 保存 token 和用户信息到 localStorage
  console.log('💾 保存 Token 到 localStorage...');
  localStorage.setItem('token', token);
  localStorage.setItem('username', user || username);
  if (name) {
    localStorage.setItem('name', name);
  }
  
  // 验证保存是否成功
  const savedToken = localStorage.getItem('token');
  if (savedToken !== token) {
    console.error('❌ Token 保存失败！');
    throw new Error('Token 保存失败');
  }
  
  console.log('✅ Token 已成功保存到 localStorage');
  console.log('✅ 用户信息已保存:', {
    username: localStorage.getItem('username'),
    name: localStorage.getItem('name')
  });
  
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


