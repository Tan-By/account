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


