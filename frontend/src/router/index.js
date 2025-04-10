// index.js (router 설정)
import { createRouter, createWebHistory } from 'vue-router'
import MainPage from '../components/MainPage'
import DBList from '../components/DBList'
import SmsHistory from '../components/SmsHistory'
import ThresholdList from '../components/ThresholdList'
import TablespacesList from '../components/TablespacesList'
import DailyChkView from '../components/DailyChk'

const routes = [
  { path: '/', component: MainPage },
  { path: '/db-list', name: 'DBList', component: DBList },
  { path: '/sms-history', name: 'SmsHistory', component: SmsHistory },
  { path: '/threshold-list', name: 'ThresholdList', component: ThresholdList },
  { path: '/tablespaces', name: 'TablespacesList', component: TablespacesList },
  { path: '/dailyChk', name: 'DailyChkView', component: DailyChkView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


export default router
