import axios from "axios";
import store from "@/store";
import router from "@/router";

const api = axios.create({
  baseURL: process.env.VUE_APP_API_URL, // 환경 변수에서 API URL 읽기
  timeout: 5000, // 요청 제한 시간 설정 (옵션)
  withCredentials: true,
  headers: {
    "Content-Type": "application/json", // 공통 헤더 설정
  },
});

// 요청 인터셉터
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터 (토큰 만료시 재발급)
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 403 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const res = await api.post("/auth/refresh", { refreshToken });

          const newAccessToken = res.data.accessToken;
          localStorage.setItem("accessToken", newAccessToken);

          store.commit("setLoggedIn", true);

          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return api(originalRequest);
        } catch (refreshError) {
          store.dispatch("logout");
          router.push("/login");
        }
      }
    }

    return Promise.reject(error);
  }
);
console.log("Axios baseURL 확인:", api.defaults.baseURL);
export default api;

