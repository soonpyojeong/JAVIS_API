<template>
  <div class="container-wrapper">
    <!-- DB 트리(PanelMenu) -->
    <div class="db-tree min-w-[220px] max-w-[260px] w-[250px] bg-white px-4 py-5 shadow rounded-tr-2xl rounded-br-2xl">
      <h3 class="mb-4 font-bold text-lg">DBMS 목록</h3>
      <PanelMenu :model="panelMenuData" v-model:expandedKeys="expandedKeys" />
    </div>
    <!-- 오른쪽 메인 (시스템 정보/로그) -->
    <div class="main-panel">
      <div class="sysinfo-area">
        <div class="hostname-section">
          <h2>{{ summary.hostname }}</h2>
        </div>
        <div class="date-picker-section">
          <CustomDatePicker
            v-model="selectedDate"
            :collectedDates="collectedDates"
            @date-select="onPrimeDateSelected"
            @month-change="onPrimeMonthChange"
          />
        </div>
        <div class="summary-cards">
          <div class="card">CPU<br /><strong>{{ summary.cpuUsage }}%</strong></div>
          <div class="card">MEM<br /><strong>{{ summary.memUsage }}%</strong></div>
          <div class="card">DISK<br /><strong>{{ summary.diskUsage }}%</strong></div>
        </div>
        <div class="disk-section">
          <h3>디스크 상세</h3>
            <canvas id="diskChart"></canvas>
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
      <div class="syslog-area">
          <div class="log-section">
            <h3>시스템 로그 요약</h3>
            <div class="log-list">
              <template v-if="isLoading">
                <ProgressSpinner style="width:50px;height:50px" />
                <div class="loading-text">로그 요약 불러오는 중...</div>
              </template>
              <template v-else>
                <template v-if="Object.keys(groupedLogSummaries).length > 0">
                  <div v-for="(entries, date) in groupedLogSummaries" :key="date" class="log-group">
                    <div class="log-group-header">📅 {{ date }}</div>
                    <div v-for="(group, index) in entries" :key="index" class="log-line">
                      <div class="log-sub-header" :class="getLogTypeClass(group.logType)">[{{ group.logType }}]</div>
                      <div class="log-message" v-for="(msg, i) in group.messages" :key="i">
                        - {{ msg.message }} ({{ msg.count }}건)
                      </div>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <div class="loading-text">로그 요약 없음</div>
                </template>
              </template>
            </div>
          </div>
        </div>

    </div>
  </div>
</template>


<script setup>
import { computed, onMounted, ref, nextTick, onBeforeUnmount } from 'vue';
import Chart from 'chart.js/auto';
import CustomDatePicker from '@/components/CustomDatePicker.vue'
import PanelMenu from 'primevue/panelmenu';
//import Panel from 'primevue/panel';
import api from '@/api';
import { useSysInfoCalendar } from '@/stores/useSysInfoCalendar';
const { collectedDates, loadCollectedDates } = useSysInfoCalendar();
import ProgressSpinner from 'primevue/progressspinner'
// 커스텀 프리셋

const summary = ref({});
const disks = ref([]);
const logSummaries = ref([]);
const hostList = ref([]);
const selectedHost = ref('');
let chartInstance = null;

const selectedDate = ref(null);
const showCalendar = ref(false);
const calendarWrapper = ref(null);
const isLoading = ref(false)

const expandedKeys = ref({});
const panelMenuData = computed(() => {
  return Object.entries(groupedHostList.value).map(([loc, items]) => ({
    key: `loc-${loc}`,
    label: loc,
    icon: 'pi pi-globe',
    items: (items || []).map(host => ({
      key: `host-${loc}-${host.label}`,
      label: host.label,
      icon: 'pi pi-database',
      command: () => selectHost(host.label)
    }))
  }));
});

//const highlightDates = computed(() =>
//  (collectedDates.value || []).map(d => {
//    if (typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
//   if (d instanceof Date) {
//      const yyyy = d.getFullYear();
//      const mm = String(d.getMonth() + 1).padStart(2, '0');
//     const dd = String(d.getDate()).padStart(2, '0');
//      return `${yyyy}-${mm}-${dd}`;
//    }
//    return '';
//  })
//)


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
    summary.value = sum || {};
    disks.value = Array.isArray(dks) ? dks : [];
    logSummaries.value = Array.isArray(logs) ? logs : [];
    selectedDate.value = selected;
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
  isLoading.value = true;
  const url = hostname ? `/api/sysinfo/latest?hostname=${hostname}` : '/api/sysinfo/latest';
  const { data } = await api.get(url);

  summary.value = data.summary;
  disks.value = data.disks;

  if (summary.value?.checkDate) {
    selectedDate.value = new Date(summary.value.checkDate);
    const year = selectedDate.value.getFullYear();
    const month = selectedDate.value.getMonth() + 1;
    await loadCollectedDates(hostname, year, month);
  }

  logSummaries.value = [];
  if (summary.value?.id) {
    const res = await api.get(`/api/sysinfo/log-summary?summaryId=${summary.value.id}`);
    logSummaries.value = res.data;
  }
  isLoading.value = false;
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

// script 부분
const renderDiskChart = () => {
  const ctx = document.getElementById('diskChart');
  if (!ctx) return;
  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }
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

//watch([collectedDates, highlightDates], ([dates, highlights]) => {
//  console.log('collectedDates:', dates);
//  console.log('highlightDates:', highlights);
//});

</script>



<style scoped>
.container-wrapper {
  display: flex;
  flex-direction: row;
  height: 100vh;
  background: #f8fafc;
  gap: 0; /* 여백 없이 딱! */
}
.db-tree {
  width: 230px;
  min-width: 210px;
  max-width: 250px;
  background: #fff;
  padding: 12px 10px 12px 18px;
  border-radius: 0 18px 18px 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  height: 100vh;
  overflow-y: auto;
  flex-shrink: 0;
}

.main-panel {
  display: flex;
  flex-direction: row;
  flex: 1 1 0;
  min-width: 0;
  background: transparent;
  padding: 0;
  gap: 0;
  align-items: stretch;
}

.sysinfo-area {
  width: 720px;
  min-width: 260px;
  max-width: 720px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 20px 20px 12px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-left: 2px;
  margin: 0;
}
.syslog-area {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.11);
  margin-left: 2px;
  padding: 20px 24px;
  overflow-y: auto;
}
.hostname-section { margin-bottom: 6px; }
.summary-cards {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.card {
  flex: 1 1 0;
  min-width: 90px;
  padding: 8px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  text-align: center;
  font-weight: 600;
  background: #f6fafd;
  font-size: 1rem;
  color: #2d64a9;
}
.disk-section { margin-bottom: 14px; }
.disk-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 7px;
  font-size: 0.96em;
}

.disk-section canvas#diskChart {
  width: 100% !important;
  height: 160px !important;
  max-height: 200px;
  display: block;
}
.disk-table th, .disk-table td {
  border: 1px solid #f3f4f7;
  padding: 5px 8px;
}
.date-picker-section {
  margin: 3px 0 9px 0;
  display: flex;
  align-items: center;
  gap: 7px;
}
.log-group-header {
  font-weight: 700;
  background: #f3f4f7;
  color: #374151;
  padding: 8px 12px;
  border-radius: 5px;
  margin-top: 12px;
}
.log-line {
  font-size: 0.97rem;
  background: #f8fafc;
  padding: 10px 16px;
  border-radius: 9px;
  border-left: 4px solid #dbeafe;
  margin: 6px 0;
}
.log-message {
  background: #e7f3ff;
  border-left: 3px solid #93c5fd;
  border-radius: 6px;
  margin: 3px 0;
  padding: 6px 10px;
}
.log-sub-header.log-error { color: #e11d48; font-weight: bold; }
.log-sub-header.log-warn { color: #f59e42; font-weight: bold; }
.log-sub-header.log-info { color: #0284c7; font-weight: bold; }
@media (max-width: 1200px) {
  .container-wrapper, .main-panel { flex-direction: column; }
  .sysinfo-area, .syslog-area { width: 100%; max-width: none; min-width: 0; margin-left: 0; }
}


.loading-text {
  margin-top: 16px;
  text-align: center;
  color: #666;
  font-size: 1.1em;
}

</style>