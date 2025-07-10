import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import PrimeVue from 'primevue/config';
import Aura from '@primeuix/themes/aura';
import './main.css';
import 'primeicons/primeicons.css';
import ToastService from 'primevue/toastservice';
import Toast from 'primevue/toast';
import './assets/vue-flow.css';

// ✅ [1] 토큰/유저 상태 복구
const accessToken = localStorage.getItem('accessToken');
const userRaw = localStorage.getItem('user');
if (accessToken && userRaw && userRaw !== "undefined") {
  try {
    const user = JSON.parse(userRaw);
    store.commit('setUser', user);
    store.commit('setLoggedIn', true);
  } catch (e) {
    store.dispatch('logout');
  }
}

// ✅ [2] 앱 생성 및 플러그인 등록
const app = createApp(App);
app.use(store);
app.use(router);
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      prefix: 'p',
      darkModeSelector: 'light',
      cssLayer: false
    }
  }
});
app.use(ToastService);
app.component('Toast', Toast);

app.mount('#app');
