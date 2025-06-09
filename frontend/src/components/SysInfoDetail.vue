<template>
  <div class="container-wrapper">
    <!-- HOSTNAME 리스트 -->
    <div class="db-tree">
      <h3>DBMS 목록</h3>
      <ul>
        <li v-for="(items, loc) in groupedHostList" :key="loc">
          <span class="tree-node" @click="expandedLoc[loc] = !expandedLoc[loc]">
            <span class="toggle-icon">{{ expandedLoc[loc] ? '-' : '+' }}</span> {{ loc }}
          </span>
          <ul v-if="expandedLoc[loc]" class="sub-tree">
            <li
              v-for="item in items"
              :key="item.id"
              @click="selectHost(item.label)"
              :class="['host-item', { selected: selectedHost === item.label }]"
            >
              {{ item.label }}
            </li>
          </ul>
        </li>
      </ul>
    </div>

    <!-- 시스템 정보 영역 -->
    <div class="sysinfo-detail">
      <div class="top-grid">
        <!-- 요약 정보 -->
        <div class="left-summary">
          <div class="hostname-section">
            <h2>{{ summary.hostname }}</h2>
          </div>
          <div class="text-sm text-gray-600">
            <i class="pi pi-clock text-blue-500 mr-1" /> 가동 시간: {{ summary.uptime }}가동시간 왜 안나와

          </div>
            <!-- 날짜 달력 -->
             <div class="date-picker-section">
                    <CustomDatePicker
                       v-model="selectedDate"
                       :collectedDates="collectedDates"
                       @date-select="onPrimeDateSelected"
                       @month-change="onPrimeMonthChange"
                     />
                 </div>

        <div class="summary-cards">
            <div class="card">CPU 사용률<br /><strong>{{ summary.cpuUsage }}%</strong></div>
            <div class="card">메모리 사용률<br /><strong>{{ summary.memUsage }}%</strong></div>
            <div class="card">디스크 사용률<br /><strong>{{ summary.diskUsage }}%</strong></div>
          </div>

          <div class="disk-section">
            <h3>디스크 상세</h3>
            <canvas :id="`diskChart-${chartKey}`"></canvas>
            <table class="disk-table">
              <thead>
                <tr>
                  <th>Filesystem</th>
                  <th>Size</th>
                  <th>Used</th>
                  <th>Avail</th>
                  <th>Use%</th>
                  <th>Mounted on</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="disk in disks || []" :key="disk.id">
                  <td>{{ disk.filesystem }}</td>
                  <td>{{ disk.diskSize }}</td>
                  <td>{{ disk.used }}</td>
                  <td>{{ disk.avail }}</td>
                  <td>{{ disk.usePercent }}</td>
                  <td>{{ disk.mountedOn }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 로그 요약 -->
        <div class="right-log">
          <div class="log-section">
            <h3>시스템 로그 요약</h3>
            <div class="log-list">
              <div v-for="(entries, date) in groupedLogSummaries" :key="date" class="log-group">
                <div class="log-group-header">📅 {{ date }}</div>
                <div v-for="(group, index) in entries" :key="index" class="log-line">
                  <div class="log-sub-header" :class="getLogTypeClass(group.logType)">[{{ group.logType }}]</div>
                  <div class="log-message" v-for="(msg, i) in group.messages" :key="i">
                    - {{ msg.message }} ({{ msg.count }}건)
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>



<script setup>
import { computed, onMounted, ref, nextTick, onBeforeUnmount ,watch} from 'vue';
import Chart from 'chart.js/auto';
import CustomDatePicker from '@/components/CustomDatePicker.vue'
import api from '@/api';
import { useSysInfoCalendar } from '@/stores/useSysInfoCalendar';
const { collectedDates, loadCollectedDates } = useSysInfoCalendar();
// 커스텀 프리셋


const summary = ref({});
const disks = ref([]);
const logSummaries = ref([]);
const hostList = ref([]);
const selectedHost = ref('');
const chartKey = ref(0);
let chartInstance = null;
const expandedLoc = ref({});

const selectedDate = ref(null);
const showCalendar = ref(false);
const calendarWrapper = ref(null);


const highlightDates = computed(() =>
  (collectedDates.value || []).map(d => {
    if (typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
    if (d instanceof Date) {
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd}`;
    }
    return '';
  })
)


// 날짜 클릭 → 해당 일자 데이터 조회
const onPrimeDateSelected = async (event) => {
  showCalendar.value = false;
  if (!selectedHost.value || !event || isNaN(event.getTime())) return;
  selectedDate.value = event;
  await handleDateChange(event);
};

const onPrimeMonthChange = async ({ month, year }) => {
  // month-change에서 month는 0~11, year 그대로
  // selectedDate(현재 달력 v-model)의 getMonth()도 0~11
  // 둘이 다르면 selectedDate로 맞추거나, 둘 중 큰 쪽(최신 달력 값) 사용
  let trueMonth = month + 1;
  if (selectedDate.value instanceof Date && selectedDate.value.getFullYear() === year) {
    trueMonth = selectedDate.value.getMonth() + 1; // Date는 0-based
  }
  await loadCollectedDates(selectedHost.value, year, trueMonth);
 // console.log("🔄 month-change 이후 collectedDates:", collectedDates.value);
};

const handleDateChange = async (value) => {
  const selected = value instanceof Date ? value : new Date(value);
  if (!selectedHost.value || !selected || isNaN(selected.getTime())) {
    summary.value = {};
    disks.value = [];
    logSummaries.value = [];
    return;
  }
  const yyyy = selected.getFullYear();
  const mm = selected.getMonth() + 1;
  const dateStr = selected.toISOString().slice(0, 10);

  await loadCollectedDates(selectedHost.value, yyyy, mm);

  try {
    const res = await api.get('/api/sysinfo/by-host-and-date', {
      params: { hostname: selectedHost.value, date: dateStr }
    });
    const { summary: sum, disks: dks, logs: logs } = res.data || {};
    summary.value = { ...summary.value, ...sum };
    disks.value = Array.isArray(dks) ? dks : [];
    logSummaries.value = Array.isArray(logs) ? logs : [];
    selectedDate.value = selected;
    chartKey.value += 1;
    await nextTick(() => renderDiskChart());
  } catch (e) {
    summary.value = {};
    disks.value = [];
    logSummaries.value = [];
    selectedDate.value = null;
    //console.error('❌ 수집 데이터 요청 실패:', e);
  }
};


const selectHost = async (hostname) => {
  selectedHost.value = hostname;
  await fetchSysInfo(hostname);

  if (selectedDate.value) {
    const year = selectedDate.value.getFullYear();
    const month = selectedDate.value.getMonth() + 1;
    await loadCollectedDates(hostname, year, month);
    // 여기서 최신 값 출력!
    //console.log("🔄 selectHost 이후 collectedDates:", collectedDates.value);
  }
  await nextTick();
};


const fetchSysInfo = async (hostname = null) => {
  const url = hostname ? `/api/sysinfo/latest?hostname=${hostname}` : '/api/sysinfo/latest';
  const { data } = await api.get(url);

  summary.value = data.summary;
  disks.value = data.disks;

  if (summary.value?.checkDate) {
    selectedDate.value = new Date(summary.value.checkDate);
    const year = selectedDate.value.getFullYear();
    const month = selectedDate.value.getMonth() + 1;
    console.log('호스트:', hostname, '수집일:', collectedDates.value);
    await loadCollectedDates(hostname, year, month);
  }

  logSummaries.value = [];
  if (summary.value?.id) {
    const res = await api.get(`/api/sysinfo/log-summary?summaryId=${summary.value.id}`);
    logSummaries.value = res.data;
  }

  chartKey.value += 1;
  nextTick(() => renderDiskChart());
};

const groupedHostList = computed(() => {
  if (!Array.isArray(hostList.value)) return {};
  return hostList.value
    .filter(item => item.hostname && item.loc)
    .sort((a, b) => a.loc.localeCompare(b.loc, 'ko') || a.hostname.localeCompare(b.hostname, 'ko'))
    .reduce((acc, item) => {
      if (!acc[item.loc]) acc[item.loc] = [];
      acc[item.loc].push({ label: item.hostname, id: item.hostname });
      return acc;
    }, {});
});

const groupedLogSummaries = computed(() => {
  const groups = {};
  for (const entry of logSummaries.value) {
    const date = entry.logDate;
    if (!groups[date]) groups[date] = [];
    let group = groups[date].find(g => g.logType === entry.logType);
    if (!group) {
      group = { logType: entry.logType, messages: [] };
      groups[date].push(group);
    }
    group.messages.push({ message: entry.message, count: entry.count });
  }
  return Object.fromEntries(
    Object.entries(groups).sort(([a], [b]) => b.localeCompare(a))
  );
});

const getLogTypeClass = (type) => {
  switch (type) {
    case 'ERROR':
    case 'DB_ERROR': return 'log-error';
    case 'WARN': return 'log-warn';
    case 'INFO': return 'log-info';
    default: return '';
  }
};
// ❌ 달력 외부 클릭 시 닫기
const handleClickOutside = (e) => {
  if (calendarWrapper.value && !calendarWrapper.value.contains(e.target)) {
    showCalendar.value = false;
  }
};
const fetchHostList = async () => {
  const res = await api.get('/api/sysinfo/hostnames');
  hostList.value = res.data;
};

const renderDiskChart = () => {
  const canvasId = `diskChart-${chartKey.value}`;
  const ctx = document.getElementById(canvasId);
  if (!ctx) return;
  if (chartInstance) chartInstance.destroy();

  chartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: disks.value.map(d => d.mountedOn),
      datasets: [{
        label: '디스크 사용률(%)',
        data: disks.value.map(d => parseInt(d.usePercent)),
        backgroundColor: 'rgba(75, 192, 192, 0.6)'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: 'y',
      plugins: { legend: { labels: { color: '#333', font: { size: 14, weight: 'bold' } } } },
      scales: {
        x: {
          max: 100,
          ticks: { color: '#000', font: { size: 13, weight: 'bold' } },
          title: { display: true, text: '사용률 (%)', color: '#000', font: { weight: 'bold', size: 14 } }
        },
        y: {
          ticks: { color: '#000', font: { size: 13, weight: 'bold' } }
        }
      }
    }
  });
};

onMounted(() => {
  fetchHostList();
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }
});
watch([collectedDates, highlightDates], ([dates, highlights]) => {
  console.log('collectedDates:', dates);
  console.log('highlightDates:', highlights);
});
</script>



<style scoped>
.container-wrapper {
  display: flex;
  height: 100%;
  background-color: #f4f4f4;
}

/* DB 트리 스타일 */
.db-tree {
  width: 250px; /* DB 트리의 너비를 조정 (기본값은 200px) */
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  height: 100vh; /* DB 트리의 높이를 화면에 맞게 설정 */
  overflow-y: auto;
  flex-shrink: 0;
}

/* 트리 노드 스타일 */
.tree-node {
  font-weight: bold;
  cursor: pointer;
  padding: 8px;
  background-color: #f0f0f0;
  border-radius: 5px;
  margin: 5px 0;
  display: flex;
  align-items: center;
  transition: background-color 0.3s ease;
}


.db-tree ul {
  list-style: none;
  padding: 0;
}

.db-tree .host-item {
  padding: 8px 12px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  color: #333;
  transition: background 0.2s;
}

.db-tree .host-item:hover {
  background-color: #e9f1f7;
}

.db-tree .host-item.selected {
  background-color: #d1e7fd;
  color: #1d70b8;
  font-weight: bold;
}
.sysinfo-detail {
  flex-grow: 1;
  width: 100%;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 100vh;
}
.uptime-display {
  font-size: 0.9rem;
  color: #666;
  margin-top: 5px;
}

.top-grid {
  display: flex;
  gap: 20px;
}

.left-summary {
  flex: 1;
}

.right-log {
  flex: 1;
  max-height: 100vh;
  overflow-y: auto;
  background-color: #f9f9f9;
  border-left: 1px solid #ddd;
  padding-left: 15px;
}

.hostname-section {
  margin-bottom: 10px;
}

.summary-cards {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.card {
  flex: 1;
  min-width: 150px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  text-align: center;
  font-weight: bold;
  background: #f9f9f9;
}

.disk-section {
  margin-bottom: 30px;
  width: 100%;
}

.disk-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

.disk-table th,
.disk-table td {
  border: 1px solid #ddd;
  padding: 8px;
}

.log-list {
  margin-top: 10px;
}

.log-line {
  font-size: 0.85rem;
  line-height: 1.4;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  background: #fff;
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 8px;
  border-left: 4px solid #bbb;
}

.log-message {
  padding: 6px 10px;
  margin: 4px 0;
  border-radius: 6px;
  background-color: #f6f8fa;
  border-left: 4px solid #ccc;
  font-size: 0.85rem;
  line-height: 1.5;
  word-break: break-word;
}

canvas#diskChart {
  width: 100% !important;
  height: 250px !important;
  max-height: 300px;
}

.log-group-header {
  font-weight: bold;
  background: #eee;
  padding: 6px 8px;
  margin-top: 15px;
  border-radius: 4px;
}
.disk-chart-wrapper {
  width: 100%;
  height: auto;
  max-height: 400px;
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-start; /* ✅ 왼쪽 정렬 */
}

canvas[id^="diskChart-"] {
  width: 100% !important;
  height: auto !important;
  max-height: 400px !important;
}

.log-sub-header.log-error {
  color: red;
  font-weight: bold;
}

.log-sub-header.log-warn {
  color: orange;
  font-weight: bold;
}

.log-sub-header.log-info {
  color: teal;
  font-weight: bold;
}

.date-picker-section {
  margin: 10px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  display: inline-block;


}

</style>
