<template>
  <div class="ts-usage-container">
    <!-- 월간 증가량 -->
    <div class="tabs">
      <div class="tab-title">
        <i class="pi pi-calendar" style="color:#3b82f6; margin-right:6px;" />
        월간 증가량
      </div>
      <div class="content-box">
        <div v-if="loadingMonthly" class="spinner-wrapper">
          <ProgressSpinner strokeWidth="4" style="width: 22px; height: 22px;" />
          <div class="spinner-text">로딩 중...</div>
        </div>
        <div v-else class="scrollable">
          <table class="data-table">
            <thead>
              <tr>
                <th>순위</th><th>DB 설명</th><th>DB 유형</th><th>DB 이름</th><th>TS 이름</th><th>증가량(MB)</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, idx) in monthlyList"
                :key="row.dbName + row.tsName + idx"
                :class="{ 'highlight-top': idx < 5 }"
              >
                <td>{{ idx + 1 }}</td>
                <td>{{ row.dbDescript }}</td>
                <td>{{ row.dbType }}</td>
                <td>{{ row.dbName }}</td>
                <td>{{ row.tsName }}</td>
                <td class="text-right">{{ row.increaseMb.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 주간 증가량 -->
    <div class="tabs">
      <div class="tab-title">
        <i class="pi pi-clock" style="color:#10b981; margin-right:6px;" />
        주간 증가량
      </div>
      <div class="content-box">
        <div v-if="loadingWeekly" class="spinner-wrapper">
          <ProgressSpinner strokeWidth="4" style="width: 22px; height: 22px;" />
          <div class="spinner-text">로딩 중...</div>
        </div>
        <div v-else class="scrollable">
          <table class="data-table">
            <thead>
              <tr>
                <th>순위</th><th>DB 설명</th><th>DB 유형</th><th>DB 이름</th><th>TS 이름</th><th>증가량(MB)</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, idx) in weeklyList"
                :key="row.dbName + row.tsName + idx"
                :class="{ 'highlight-top': idx < 5 }"
              >
                <td>{{ idx + 1 }}</td>
                <td>{{ row.dbDescript }}</td>
                <td>{{ row.dbType }}</td>
                <td>{{ row.dbName }}</td>
                <td>{{ row.tsName }}</td>
                <td class="text-right">{{ row.increaseMb.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api'
import ProgressSpinner from 'primevue/progressspinner'
const monthlyList = ref([])
const weeklyList = ref([])
const loadingMonthly = ref(true)
const loadingWeekly = ref(true)

function fetchMonthly() {
  loadingMonthly.value = true
  api.get('/api/tb/tablespaces/increase/monthly')
    .then(res => monthlyList.value = res.data)
    .finally(() => loadingMonthly.value = false)
}

function fetchWeekly() {
  loadingWeekly.value = true
  api.get('/api/tb/tablespaces/increase/weekly')
    .then(res => weeklyList.value = res.data)
    .finally(() => loadingWeekly.value = false)
}

onMounted(() => {
  fetchMonthly()
  fetchWeekly()
})
</script>

<style scoped>
.ts-usage-container {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  padding: 1.5rem;
}
.tabs {
  flex: 1 1 48%;
  min-width: 380px;
}
.tab-title {
  font-weight: bold;
  font-size: 1.1rem;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.content-box {
  border: 1px solid #ccc;
  border-radius: 6px;
  padding: 1rem;
  background: white;
  min-height: 300px;
  position: relative;
}
.loader {
  text-align: center;
  padding-top: 50px;
  color: gray;
}
.scrollable {
  max-height: 420px;
  overflow-y: auto;
}
.text-right {
  text-align: right;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th,
.data-table td {
  border: 1px solid #ddd;
  padding: 8px;
}
.data-table th {
  background-color: #f3f4f6;
  font-weight: 600;
}
.highlight-top {
  background-color: #e0f2fe; /* 연한 하늘색 (Tailwind 기준 sky-100) */
  font-weight: 600;
}


:deep(.p-tabview-nav) {
  border-bottom: none !important;
}
.spinner-wrapper {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
}
.spinner-text {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280; /* Tailwind 기준 gray-500 */
}

</style>
