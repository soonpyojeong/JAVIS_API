<template>
  <div class="dashboard-summary">
    <!-- KPI 카드 -->
    <TopKPICards :stats="stats" />

    <!-- 리소스 게이지 -->
    <ResourceGauges :cpu="summary.cpuUsage" :memory="summary.memUsage" :disk="summary.diskUsage" />

    <!-- DB 상태 그리드 -->
    <DBHealthGrid :instances="instanceStatuses" />

    <!-- 알람 요약 -->
    <AlertSummary :alerts="alerts" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopKPICards from '@/components/TopKPICards.vue'
import ResourceGauges from '@/components/ResourceGauges.vue'
import DBHealthGrid from '@/components/DBHealthGrid.vue'
import AlertSummary from '@/components/AlertSummary.vue'
//import api from '@/api'

const stats = ref({ totalDBs: 0, normal: 0, warning: 0, critical: 0 })
const summary = ref({ cpuUsage: 0, memUsage: 0, diskUsage: 0 })
const instanceStatuses = ref([])
const alerts = ref([])

onMounted(async () => {
  //const { data } = await api.get('/dashboard/summary')
  //stats.value = data.stats
  //summary.value = data.summary
  //instanceStatuses.value = data.instances
  //alerts.value = data.alerts
})
</script>

<style scoped>
.dashboard-summary {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px;
  background-color: #f9fafb;
}
</style>