// main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios';
import store from "./store";

axios.defaults.baseURL = process.env.VUE_APP_BASE_URL;
// 🔥 새로고침 시 상태 복원
const token = localStorage.getItem("accessToken");
const userRaw = localStorage.getItem("user");

if (token && userRaw && userRaw !== "undefined") {
  try {
    const user = JSON.parse(userRaw);
    store.commit("setUser", user);
    store.commit("setLoggedIn", true);
  } catch (e) {
    console.warn("⛔ user JSON 파싱 실패", e);
    store.dispatch("logout"); // 잘못된 값이면 세션 클리어
  }
}
const app = createApp(App);
app.use(store);
app.use(router);
app.mount('#app');
