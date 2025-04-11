import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')

import axios from 'axios';

axios.defaults.baseURL = process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080/api';


