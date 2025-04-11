import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios';

axios.defaults.baseURL = 'http://10.90.4.60:8813'; // Spring Boot 서버 URL

createApp(App).use(router).mount('#app')


