import axios from "axios";
import store from "@/store";

const api = axios.create({
  baseURL: process.env.VUE_APP_BASE_URL, // 예: http://10.90.4.60:8813
  timeout: 5000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ 요청 인터셉터 - accessToken 자동 포함
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ✅ 응답 인터셉터 - accessToken 만료 시 자동 재발급
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (
      error.response &&
      error.response.status === 401 &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;
      console.warn("⚠️ accessToken 만료, refresh 시도 중...");
      try {
        const res = await api.post('/api/auth/refresh', {
          refreshToken: localStorage.getItem('refreshToken'),
        });
        const newAccessToken = res.data.accessToken;
        localStorage.setItem('accessToken', newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        //console.info("✅ accessToken 재발급 성공");
        return axios(originalRequest);
      } catch (refreshError) {
        //console.error("❌ refreshToken 재발급 실패", refreshError);
        store.dispatch("logout");
        window.location.href = "/login";
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);


export default api;
