import axios from "axios";
import store from "@/store";
import router from "@/router";

const api = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_URL, // ✅ Vite 환경변수
  timeout: 600000,
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

// ✅ 응답 인터셉터 - accessToken 만료 시 자동 재발급 & reset-password 예외처리
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // ✅ reset-password 화면/요청 예외처리 (리다이렉트 금지)
    const isResetPassword =
      originalRequest.url?.includes("/reset-password") ||
      window.location.pathname.startsWith("/reset-password");

    if (
      error.response &&
      error.response.status === 401 &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;
      try {
        // 토큰 재발급 요청
        const res = await api.post('/api/auth/refresh', {
          refreshToken: localStorage.getItem('refreshToken'),
        });
        const newAccessToken = res.data.accessToken;
        localStorage.setItem('accessToken', newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        store.dispatch("logout");
        if (!isResetPassword) {
          // SPA 방식 라우터 이동 (새로고침 X)
          router.push("/login");
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
