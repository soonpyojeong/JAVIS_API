// main.js

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// 모든 axios 요청에 기본 URL 설정
axios.defaults.baseURL = 'http://10.90.4.60:8813';

createApp(App).use(router).mount('#app');