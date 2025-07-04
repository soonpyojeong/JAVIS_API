// router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '../components/MainPage.vue';
import DBList from '../components/PrimevueDBList.vue';
import SmsHistory from '../components/SmsHistory.vue';
import ThresholdList from '../components/ThresholdList.vue';
import TablespacesList from '../components/TablespacesList.vue';
import DailyChkView from '../components/PrimevueDailyChk.vue';
import LoginComponent from '../components/LoginComponent.vue';
import SysInfoDetail from '../components/PrimevueSysInfoDetail.vue';
import PWMng from '../components/PassWordMng.vue';
import PrimevueTest from '../components/PrimevueTest.vue';
import EtlDbConnection from '../components/EtlDbConnection.vue';
import ETLJobList from '../components/ETLJobList.vue';
import MappingSimulate from '../components/MappingSimulate.vue';
import EtlWorkflow from '../components/EtlWorkflow.vue';
import MonitorModuleManage from '../components/MonitorModuleManage.vue';
import EtlJobScheduler from '../components/EtlJobScheduler.vue';
import EtlSchedulerLog from '../components/EtlSchedulerLog.vue';
import EtlSchedulerDialog from '../components/EtlSchedulerDialog.vue';
import ManagerMenuRole from '../components/ManagerMenuRole.vue';


import store from '../store';

const routes = [
  { path: '/login', component: LoginComponent, meta: { title: 'Login', requiresAuth: false } },
  { path: '/', component: Dashboard, meta: { title: 'JAVIS Dashboard', requiresAuth: true } },
  { path: '/db-list', name: 'DBList', component: DBList, meta: { title: 'DB List', requiresAuth: true } },
  { path: '/sms-history', name: 'SmsHistory', component: SmsHistory, meta: { title: 'SMS History', requiresAuth: true } },
  { path: '/threshold-list', name: 'ThresholdList', component: ThresholdList, meta: { title: 'Threshold List', requiresAuth: true } },
  { path: '/tablespaces', name: 'TablespacesList', component: TablespacesList, meta: { title: 'Tablespaces List', requiresAuth: true } },
  { path: '/dailyChk', name: 'DailyChkView', component: DailyChkView, meta: { title: 'Daily Check View', requiresAuth: true } },
  { path: '/SysInfoDetail', name: 'SysInfoDetail', component: SysInfoDetail, meta: { title: 'SysInfoDetail', requiresAuth: true } },
  { path: '/Manager', name: 'PWMng', component: PWMng, meta: { title: 'Manager', requiresAuth: true } },
  { path: '/TEST', name: 'PrimevueTest', component: PrimevueTest, meta: { title: 'Manager', requiresAuth: true } },
  { path: '/EtlDbConnection', name: 'EtlDbConnection', component: EtlDbConnection, meta: { title: 'EtlDbConnection', requiresAuth: true } },
  { path: '/ETLJobList', name: 'ETLJobList', component: ETLJobList, meta: { title: 'ETLJobList', requiresAuth: true } },
  { path: '/MappingSimulate', name: 'MappingSimulate', component: MappingSimulate, meta: { title: 'MappingSimulate', requiresAuth: true } },
  { path: '/EtlWorkflow', name: 'EtlWorkflow', component: EtlWorkflow, meta: { title: 'EtlWorkflow', requiresAuth: true } },
  { path: '/ETLjob-Scheduler', name: 'EtlJobScheduler', component: EtlJobScheduler, meta: { title: 'EtlJobScheduler', requiresAuth: true } },
  { path: '/MonitorModuleManage', name: 'MonitorModuleManage', component: MonitorModuleManage, meta: { title: 'MonitorModuleManage', requiresAuth: true } },
  { path: '/ManagerMenuRole', name: 'ManagerMenuRole', component: ManagerMenuRole, meta: { title: 'ManagerMenuRole', requiresAuth: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const accessToken = localStorage.getItem("accessToken");
  //const refreshToken = localStorage.getItem("refreshToken");
  const userRaw = localStorage.getItem("user");

  //console.groupCollapsed(`[🔁 라우터 이동] ${from.path} → ${to.path}`);
  //console.log("🪪 accessToken:", accessToken);
  //console.log("🪪 refreshToken:", refreshToken);
  //console.log("🧑 userRaw:", userRaw);
  //console.log("📦 Vuex isLoggedIn:", store.state.isLoggedIn);
  console.groupEnd();

  if (!store.state.isLoggedIn && accessToken && userRaw && userRaw !== "undefined") {
    try {
      const user = JSON.parse(userRaw);
      store.commit("setUser", user);
      store.commit("setLoggedIn", true);
      //console.info("✅ 상태 복원 완료 (user, token)");
    } catch (e) {
      //console.warn("❌ user 복원 실패", e);
      store.dispatch("logout");
    }
  }

  const isLoggedIn = store.state.isLoggedIn;

  if (to.path === "/login" && isLoggedIn) {
    //console.warn("⚠️ 로그인된 사용자가 로그인 페이지 접근 → / 리다이렉트");
    next("/");
  } else if (to.meta.requiresAuth && !isLoggedIn) {
   // console.error("❌ 인증 필요 페이지 접근 → 로그인 페이지로 이동");
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
