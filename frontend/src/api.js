import axios from "axios";

const api = axios.create({
  baseURL: process.env.VUE_APP_API_URL, // 환경 변수에서 API URL 읽기
  timeout: 5000, // 요청 제한 시간 설정 (옵션)
  headers: {
    "Content-Type": "application/json", // 공통 헤더 설정
  },
});
console.log("Axios baseURL 확인:", api.defaults.baseURL);
export default api;