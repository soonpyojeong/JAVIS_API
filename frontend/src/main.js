// main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios';
import store from "./store";

axios.defaults.baseURL = process.env.VUE_APP_BASE_URL;

const app = createApp(App);
app.use(store);
app.use(router);

// 라우터 인스턴스에 스토어 주입
router.store = store;

app.mount('#app');