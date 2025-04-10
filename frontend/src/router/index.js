// index.js (router 설정)
import { createRouter, createWebHistory } from 'vue-router'
import MainPage from '../components/MainPage'
import DBList from '../components/DBList'
import SmsHistory from '../components/SmsHistory'
import ThresholdList from '../components/ThresholdList'
import TablespacesList from '../components/TablespacesList'
import DailyChkView from '../components/DailyChk'

const routes = [
  { path: '/', component: MainPage, meta: { title: 'JAVIS HOME' } },
  { path: '/db-list', name: 'DBList', component: DBList, meta: { title: 'DB List' } },
  { path: '/sms-history', name: 'SmsHistory', component: SmsHistory, meta: { title: 'SMS History' } },
  { path: '/threshold-list', name: 'ThresholdList', component: ThresholdList, meta: { title: 'Threshold List' } },
  { path: '/tablespaces', name: 'TablespacesList', component: TablespacesList, meta: { title: 'Tablespaces List' } },
  { path: '/dailyChk', name: 'DailyChkView', component: DailyChkView, meta: { title: 'Daily Check View' } }
]


const router = createRouter({
  history: createWebHistory(),
  routes
})

// 페이지 전환 시 title 업데이트
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title;
  }
  next();
});

export default router;