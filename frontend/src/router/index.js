import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '../components/MainPage';
import DBList from '../components/DBList';
import SmsHistory from '../components/SmsHistory';
import ThresholdList from '../components/ThresholdList';
import TablespacesList from '../components/TablespacesList';
import DailyChkView from '../components/DailyChk';
import LoginComponent from '../components/LoginComponent';
import store from '../store';

const routes = [
  { path: '/login', component: LoginComponent, meta: { title: 'Login', requiresAuth: false } },
  { path: '/', component: Dashboard, meta: { title: 'JAVIS Dashboard', requiresAuth: true } },
  { path: '/db-list', name: 'DBList', component: DBList, meta: { title: 'DB List', requiresAuth: true } },
  { path: '/sms-history', name: 'SmsHistory', component: SmsHistory, meta: { title: 'SMS History', requiresAuth: true } },
  { path: '/threshold-list', name: 'ThresholdList', component: ThresholdList, meta: { title: 'Threshold List', requiresAuth: true } },
  { path: '/tablespaces', name: 'TablespacesList', component: TablespacesList, meta: { title: 'Tablespaces List', requiresAuth: true } },
  { path: '/dailyChk', name: 'DailyChkView', component: DailyChkView, meta: { title: 'Daily Check View', requiresAuth: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const isLoggedIn = store.state.isLoggedIn || !!localStorage.getItem("accessToken");
  console.log("Router Guard - isLoggedIn:", isLoggedIn);
  console.log("Navigating to:", to.path);

  if (to.path === "/login" && isLoggedIn) {
    next("/");
  } else if (to.meta.requiresAuth && !isLoggedIn) {
    next("/login");
  } else {
    next();
  }
});

router.afterEach((to) => {
  if (to.meta.title) {
    document.title = to.meta.title;
  }
});

export default router;
