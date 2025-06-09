import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import PrimeVue from 'primevue/config';
import Aura from '@primeuix/themes/aura'
import './main.css'
import 'primeicons/primeicons.css';
import ToastService from 'primevue/toastservice'

const token = localStorage.getItem("accessToken");
const userRaw = localStorage.getItem("user");

if (token && userRaw && userRaw !== "undefined") {
  try {
    const user = JSON.parse(userRaw);
    store.commit("setUser", user);
    store.commit("setLoggedIn", true);
  } catch (e) {
    store.dispatch("logout");
  }
}

const app = createApp(App);
app.use(store);
app.use(router);
app.use(PrimeVue, {
    // Default theme configuration
    theme: {
        preset: Aura,
        options: {
            prefix: 'p',
            darkModeSelector: 'light',
            cssLayer: false
        }
    }
 });
app.use(ToastService) ;
app.mount('#app');


