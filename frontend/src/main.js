// main.js
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
const token = localStorage.getItem("accessToken");
const userRaw = localStorage.getItem("user");

//console.log("🔄 main.js 상태 복원 시도");
//console.log("accessToken:", token);
//console.log("userRaw:", userRaw);

if (token && userRaw && userRaw !== "undefined") {
  try {
    const user = JSON.parse(userRaw);
    store.commit("setUser", user);
    store.commit("setLoggedIn", true);
    //console.log("✅ main.js 상태 복원 성공");
  } catch (e) {
    //console.error("❌ user JSON 파싱 실패", e);
    store.dispatch("logout");
  }
}


const app = createApp(App);
app.use(store);
app.use(router);
app.mount('#app');
