<template>
  <div class="ts-usage-container">
    <!-- 30일 요약 -->
    <div class="tabs">
      <div class="tab-title">
        <i class="pi pi-calendar" style="color:#3b82f6; margin-right:6px;" />
        최근 30일 아카이브 & 세션 Top
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
                <th>순위</th>
                <th>수집일시</th>
                <th>DB 설명</th>
                <th>DB 유형</th>
                <th>DB 이름</th>
                <th @click="toggleSort('activeSession', true)">
                  세션수
                  <span v-if="sortKeyMonthly === 'activeSession'">
                    {{ sortOrderMonthly === 'asc' ? '▲' : '▼' }}
                  </span>
                </th>
                <th @click="toggleSort('dailyArchCnt', true)">
                  아카이브수
                  <span v-if="sortKeyMonthly === 'dailyArchCnt'">
                    {{ sortOrderMonthly === 'asc' ? '▲' : '▼' }}
                  </span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, idx) in sortedMonthly"
                :key="row.dbName + row.chkDate + idx"
                :class="{ 'highlight-top': idx < 5 }"
              >
                <td>{{ idx + 1 }}</td>
                <td>{{ row.chkDate }}</td>
                <td>{{ row.dbDescript }}</td>
                <td>{{ row.dbType }}</td>
                <td>{{ row.dbName }}</td>
                <td class="text-right">{{ row.activeSession.toLocaleString() }}</td>
                <td class="text-right">{{ row.dailyArchCnt.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 7일 요약 -->
    <div class="tabs">
      <div class="tab-title">
        <i class="pi pi-clock" style="color:#10b981; margin-right:6px;" />
        최근 7일 아카이브 & 세션 Top
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
                <th>순위</th>
                <th>수집일시</th>
                <th>DB 설명</th>
                <th>DB 유형</th>
                <th>DB 이름</th>
                <th @click="toggleSort('activeSession', false)">
                  세션수
                  <span v-if="sortKeyWeekly === 'activeSession'">
                    {{ sortOrderWeekly === 'asc' ? '▲' : '▼' }}
                  </span>
                </th>
                <th @click="toggleSort('dailyArchCnt', false)">
                  아카이브수
                  <span v-if="sortKeyWeekly === 'dailyArchCnt'">
                    {{ sortOrderWeekly === 'asc' ? '▲' : '▼' }}
                  </span>
                </th>

              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, idx) in sortedWeekly"
                :key="row.dbName + row.chkDate + idx"
                :class="{ 'highlight-top': idx < 5 }"
              >
                <td>{{ idx + 1 }}</td>
                <td>{{ row.chkDate }}</td>
                <td>{{ row.dbDescript }}</td>
                <td>{{ row.dbType }}</td>
                <td>{{ row.dbName }}</td>
                <td class="text-right">{{ row.activeSession.toLocaleString() }}</td>
                <td class="text-right">{{ row.dailyArchCnt.toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/api'
import ProgressSpinner from 'primevue/progressspinner'

const monthlyList = ref([])
const weeklyList = ref([])
const loadingMonthly = ref(true)
const loadingWeekly = ref(true)

// 정렬 상태
const sortKeyMonthly = ref('dailyArchCnt')
const sortOrderMonthly = ref('desc')

const sortKeyWeekly = ref('dailyArchCnt')
const sortOrderWeekly = ref('desc')

// 정렬 함수
function toggleSort(key, isMonthly = true) {
  if (isMonthly) {
    if (sortKeyMonthly.value === key) {
      sortOrderMonthly.value = sortOrderMonthly.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortKeyMonthly.value = key
      sortOrderMonthly.value = 'desc'
    }
  } else {
    if (sortKeyWeekly.value === key) {
      sortOrderWeekly.value = sortOrderWeekly.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortKeyWeekly.value = key
      sortOrderWeekly.value = 'desc'
    }
  }
}

// 정렬된 리스트 반환
const sortedMonthly = computed(() => {
  return [...monthlyList.value].sort((a, b) => {
    const key = sortKeyMonthly.value
    const order = sortOrderMonthly.value === 'asc' ? 1 : -1
    return ((a[key] ?? 0) - (b[key] ?? 0)) * order
  })
})

const sortedWeekly = computed(() => {
  return [...weeklyList.value].sort((a, b) => {
    const key = sortKeyWeekly.value
    const order = sortOrderWeekly.value === 'asc' ? 1 : -1
    return ((a[key] ?? 0) - (b[key] ?? 0)) * order
  })
})

// 데이터 요청
function fetchMonthly() {
  loadingMonthly.value = true
  api.get('/api/dailychk/summary/monthly')
    .then(res => monthlyList.value = res.data)
    .finally(() => loadingMonthly.value = false)
}

function fetchWeekly() {
  loadingWeekly.value = true
  api.get('/api/dailychk/summary/weekly')
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
  cursor: pointer;
}
.highlight-top {
  background-color: #e0f2fe;
  font-weight: 600;
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
  color: #6b7280;
}
</style>
