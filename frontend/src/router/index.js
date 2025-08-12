import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '../components/MainPage.vue';
import DBList from '../components/PrimevueDBList.vue';
import SmsHistory from '../components/SmsHistory.vue';
import TablespacesList from '../components/TablespacesList.vue';
import TablespaceStats from '../components/TablespaceStats.vue';
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
import ResetPasswordPage from '../components/ResetPasswordPage.vue';

import store from '../store';

const routes = [
  { path: '/login', component: LoginComponent, meta: { title: 'Login', requiresAuth: false, public: true } },
  { path: '/', component: Dashboard, meta: { title: 'JAVIS Dashboard', requiresAuth: true } },
  { path: '/db-list', name: 'DBList', component: DBList, meta: { title: 'DB List', requiresAuth: true } },
  { path: '/sms-history', name: 'SmsHistory', component: SmsHistory, meta: { title: 'SMS History', requiresAuth: true } },
  { path: '/tablespaces', name: 'TablespacesList', component: TablespacesList, meta: { title: 'Tablespaces List', requiresAuth: true } },
  { path: '/TablespaceStats', name: 'TablespaceStats', component: TablespaceStats, meta: { title: 'Tablespace Stats', requiresAuth: true } },
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
  { path: '/ManagerMenuRole', name: 'ManagerMenuRole', component: ManagerMenuRole, meta: { title: 'ManagerMenuRole', requiresAuth: true } },
  // 필요 시 :token 파라미터 방식도 추가 가능
  { path: '/reset-password', name: 'ResetPasswordPage', component: ResetPasswordPage, meta: { title: 'ResetPasswordPage', requiresAuth: false, public: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// ✨ 전역 라우터 가드
router.beforeEach((to, from, next) => {
  // 🚩 1. /reset-password 경로(파라미터/쿼리스트링 포함) 무조건 통과!
  if (to.path.startsWith('/reset-password')) {
    next();
    return;
  }

  // 🚩 2. 세션 상태 복원 (SPA 새로고침 시)
  const accessToken = localStorage.getItem("accessToken");
  const userRaw = localStorage.getItem("user");
  if (!store.state.isLoggedIn && accessToken && userRaw && userRaw !== "undefined") {
    try {
      const user = JSON.parse(userRaw);
      store.commit("setUser", user);
      store.commit("setLoggedIn", true);
      // 상태 복원 로그(선택)
      // console.log('[상태복원] 로컬스토어에서 user/accessToken');
    } catch {
      store.dispatch("logout");
    }
  }

  // 🚩 3. 인증 분기
  const isLoggedIn = store.state.isLoggedIn;

  // 로그인 된 사용자가 /login에 접근하면 홈으로 이동
  if (to.path === "/login" && isLoggedIn) {
    next("/");
    return;
  }

  // 공개 라우트는 무조건 통과(meta.public)
  if (to.meta.public) {
    next();
    return;
  }

  // 인증 필요한데 로그인 안 됐으면 로그인으로 이동
  if (to.meta.requiresAuth && !isLoggedIn) {
    next("/login");
    return;
  }

  // 나머지는 통과
  next();
});

// ✨ 페이지 타이틀 자동 세팅
router.afterEach((to) => {
  if (to.meta.title) {
    document.title = to.meta.title;
  }
});

export default router;
