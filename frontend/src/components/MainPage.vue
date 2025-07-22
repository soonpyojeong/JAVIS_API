<template>
<div class="dashboard-summary">
    <!--
    <section>
      <div class="section-title">
        <i class="pi pi-database" style="color:#1778e7;font-size:1.3em;margin-right:8px;"/>
        DBMS 관제 내역
      </div>
      <TopKPICards :stats="stats" />
    </section>
    -->
    <!-- DB 상태 그리드 (아이콘 관제) -->
    <section>
      <div class="section-title">
        <i class="pi pi-database" style="color:#1778e7;font-size:1.3em;margin-right:8px;"/>
        데이터베이스 상태
      </div>
      <DBHealthGrid :instances="instanceStatuses" />
    </section>

    <!-- 알람 요약 -->
     <section>
          <div class="section-title">
            <i class="pi pi-bell" style="color:#ef4444;font-size:1.1em;margin-right:8px;"/>
            최근 알람
          </div>
          <!--<Button @click="fetchAlerts">새로고침</Button>-->
          <AlertSummary :alerts="alertsData" :collectedAt="collectedAt" />
        </section>
  </div>
</template>

<script setup>
import DBHealthGrid from '@/components/DBHealthGrid.vue'
import AlertSummary from '@/components/AlertSummary.vue'

import { ref, onMounted } from 'vue'
import api from '@/api'
import Button from 'primevue/button'

const stats = ref({ totalDBs: 0, normal: 0, warning: 0, critical: 0 })
const summary = ref({ cpuUsage: 0, memUsage: 0, diskUsage: 0 })
const instanceStatuses = ref([])
const alertsData = ref([])      // 반드시 빈 배열로 초기화!
const collectedAt = ref('')     // 빈 문자열로 초기화

function fetchAlerts() {
  api.get('/api/alerts/summary')
    .then(res => {
      // TIME 내림차순(최신순) 정렬
      alertsData.value = res.data.alerts.sort((a, b) => {
        // 날짜 파싱 (YYYY/MM/DD HH24:MI:SS → Date)
        const ta = new Date(a.time.replace(/\//g, '-'));
        const tb = new Date(b.time.replace(/\//g, '-'));
        return tb - ta; // 최신순
      });
      collectedAt.value = res.data.collectedAt;
    })
    .catch(() => {
      alertsData.value = [];
      collectedAt.value = '';
    })
}


onMounted(() => {
  fetchAlerts()
  setInterval(fetchAlerts, 600000)
})


</script>

<style scoped>
.dashboard-summary {
  display: flex;
  flex-direction: column;
  gap: 34px;
  padding: 28px 22px;
  background-color: #f9fafb;
  min-height: 100vh;
}

/* 상단 KPI 카드 + 리소스 게이지 가로 배치 */
.top-section {
  display: flex;
  flex-direction: row;
  gap: 28px;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 2px;
}

/* 각 Section 공통 스타일 */
section {
  margin-top: 6px;
  margin-bottom: 2px;
}

/* 타이틀 */
.section-title {
  font-size: 1.07em;
  font-weight: bold;
  color: #26426a;
  margin-bottom: 9px;
  letter-spacing: 0.02em;
  display: flex;
  align-items: center;
}

/* 미디어쿼리(모바일 대응, 세로 배치) */
@media (max-width: 900px) {
  .top-section {
    flex-direction: column;
    gap: 18px;
  }
  .dashboard-summary {
    padding: 14px 5vw;
  }
}
</style>
