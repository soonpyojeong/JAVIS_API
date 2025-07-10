<!-- PrimevueDailyChk.vue -->
<template>
  <div class="container">
    <!-- PanelMenu 트리 -->
    <div class="db-tree">
      <h3 class="mb-4 font-bold text-lg">DBMS 목록</h3>
      <PanelMenu :model="panelMenuData" v-model:expandedKeys="expandedKeys" />
    </div>
    <div class="chart-container">
      <div class="charts-wrapper">
        <h3 class="mb-4 font-bold text-lg">
          일일점검 DB :
          <span v-if="selectedDB">{{ selectedDB.instanceName }}</span>
        </h3>
        <template v-if="selectedDB && activeMetrics.length > 0">
          <div
            v-for="(metric, index) in activeMetrics"
            :key="metric.key"
            class="metric-chart"
          >
            <canvas :ref="setCanvasRef(index)"></canvas>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue';
import PanelMenu from 'primevue/panelmenu';
import Chart from 'chart.js/auto';
import api from "@/api";

const expandedKeys = ref({});
const dbTreeData = ref({});
const selectedDB = ref(null);       // { instanceName, dbType, key }
const selectedDBType = ref(null);
const selectedKey = ref('');        // 선택된 PanelMenu key
const canvasRefs = [];
const dbData = ref([]);
const tbdbData = ref([]);

// -------- 메트릭 정의 --------
const metrics = ref([
  { key: 'transaTions', label: 'Transactions' },
  { key: 'totalSess', label: 'Total Sessions' },
  { key: 'activeSess', label: 'Active Sessions' },
  { key: 'bufferpct', label: 'Buffer Nowait Pct' },
  { key: 'reDoPct', label: 'Redo Nowait Pct' },
  { key: 'buffHit', label: 'Buffer Hit Pct' },
  { key: 'latchHitPct', label: 'Latch Hit Pct' },
  { key: 'libHitPct', label: 'Library Hit Pct' },
  { key: 'softPct', label: 'Soft Parse Pct' },
  { key: 'executTopct', label: 'Execute to Parse Pct' },
  { key: 'parseCpuElapsd', label: 'Parse CPU to Elapsed' },
  { key: 'nonParseCpu', label: 'Non Parse CPU' },
  { key: 'inMemorySort', label: 'In Memory Sort' },
  { key: 'dailyArchCht', label: 'Daily Archive Count' }
]);
const tbmetrics = ref([
  { key: 'maxlSess', label: 'Max Sessions' },
  { key: 'totalSess', label: 'Total Sessions' },
  { key: 'runSess', label: 'Active Sessions' },
  { key: 'recSess', label: 'Recover Session' },
  { key: 'tsm', label: 'TSM' },
  { key: 'wpm', label: 'WPM' },
  { key: 'pgaSize', label: 'PGA Size' },
  { key: 'wpmPgadiff', label: 'WPM PGA DIFF' },
  { key: 'shardMem', label: 'SHARED POOL MEMORY' },
  { key: 'phyRead', label: 'PHYSICAL READ' },
  { key: 'logicRead', label: 'LOGICAL READ' },
  { key: 'buffHit', label: 'BUFFER CACHE HIT' },
  { key: 'libHit', label: 'LIBRARY CACHE HIT' },
  { key: 'dictHit', label: 'DICTIONARY CACHE HIT' },
  { key: 'dailyArchCht', label: 'Daily Archive Count' }
]);
const activeMetrics = computed(() =>
  selectedDBType.value === 'oracle' ? metrics.value : tbmetrics.value
);

function setCanvasRef(index) {
  return (el) => {
    if (el) canvasRefs[index] = el;
  }
}

// --- 선택 DB 저장 및 강조 ---
const selectDB = async (instanceName, dbType, key) => {
  selectedDB.value = { instanceName, dbType, key };
  selectedDBType.value = dbType.toLowerCase();
  selectedKey.value = key;      // 강조를 위한 key 저장
  canvasRefs.length = 0;
  await fetchData(instanceName, selectedDBType.value);
  await renderCharts();
};

// -------- DB 트리 불러오기 --------
const fetchDbList = async () => {
  try {
    const res = await api.get('/api/dailychk/db-list');
    const rawData = res.data || {};
    // 가나다순 정렬
    const sorted = Object.keys(rawData)
      .sort((a, b) => a.localeCompare(b, 'ko'))
      .reduce((acc, loc) => { acc[loc] = rawData[loc]; return acc; }, {});
    dbTreeData.value = sorted;
  } catch (err) {
    console.error('DB 목록 조회 실패', err);
  }
};

// -------- DB별 데이터 불러오기 --------
const fetchData = async (name, type) => {
  const endpoint = type === 'oracle' ? 'oradata' : 'tbdata';
  const res = await api.get(`/api/dailychk/${name}/${endpoint}`);
  if (type === 'oracle') dbData.value = res.data;
  else tbdbData.value = res.data;
};

// -------- PanelMenu 트리구조 생성 --------
const panelMenuData = computed(() => {
  // dbTreeData = { 당진: { TIBERO: ['MEDP', 'MDAC'] }, 부산: ... }
  return Object.entries(dbTreeData.value).map(([loc, dbTypes]) => ({
    key: `loc-${loc}`,
    label: loc,
    icon: 'pi pi-globe',
    items: Object.entries(dbTypes || {}).map(([dbType, instances]) => ({
      key: `dbtype-${loc}-${dbType}`,
      label: dbType,
      icon: dbTypeIcon(dbType),
      items: (instances || []).map(instance => {
        const key = `inst-${loc}-${dbType}-${instance}`;
        return {
          key,
          label: instance,
          icon: 'pi pi-database',
          class: selectedKey.value === key ? 'active-db' : '', // 활성화 상태 강조
          command: () => selectDB(instance, dbType, key)
        }
      })
    }))
  }));
});

function dbTypeIcon(type) {
  switch (type.toLowerCase()) {
    case 'oracle': return 'pi pi-server text-orange-400';
    case 'tibero': return 'pi pi-database text-blue-400';
    default: return 'pi pi-server text-slate-400';
  }
}

// === 차트 부분: 기존과 동일 ===
const chartInstances = [];
const hours = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, '0'));
function getRandomHue() { return Math.floor(Math.random() * 360); }
function pastelColorByHue(hue, alpha = 1) {
  return `hsla(${hue}, 70%, 60%, ${alpha})`;
}
const syncCharts = (event) => {
  if (!chartInstances.length) return;
  const baseChart = chartInstances[0];
  const elements = baseChart.getElementsAtEventForMode(event, 'index', { intersect: false }, false);
  if (!elements.length) return;
  const { index } = elements[0];
  chartInstances.forEach(chart => {
    chart.tooltip.setActiveElements(
      chart.data.datasets.map((_, datasetIndex) => ({ datasetIndex, index })),
      { x: 0, y: 0 }
    );
    chart.update();
  });
};
const renderCharts = async () => {
  chartInstances.forEach((chart) => chart.destroy());
  chartInstances.length = 0;
  const targetMetrics = activeMetrics.value;
  const targetData = selectedDBType.value === 'oracle' ? dbData.value : tbdbData.value;
  const fixed100Keys = ['buffHit', 'libHit', 'dictHit', 'bufferpct', 'reDoPct', 'buffHitPct', 'latchHitPct', 'libHitPct', 'softPct', 'executTopct', 'parseCpuElapsd'];
  await nextTick();
  targetMetrics.forEach((metric, index) => {
    const canvas = canvasRefs[index];
    if (!canvas || !canvas.getContext) return;
    const existing = Chart.getChart(canvas);
    if (existing) existing.destroy();
    const { yesterdayData, todayData } = splitYesterdayTodayData(targetData, metric.key);
    const combinedData = [...todayData, ...yesterdayData].filter(val => val !== null && val !== undefined);
    const minValue = combinedData.length > 0 ? Math.min(...combinedData) : 0;
    const maxValue = combinedData.length > 0 ? Math.max(...combinedData) : 100;
    const diff = maxValue - minValue;
    let suggestedMin;
    let suggestedMax;
    let stepSize;
    let beginAtZero;
    if (fixed100Keys.includes(metric.key)) {
      suggestedMin = 0;
      suggestedMax = 100;
      stepSize = 10;
      beginAtZero = true;
    } else {
      suggestedMin = diff >= 5 ? Math.floor(minValue) - 5 : Math.floor(minValue);
      suggestedMax = diff >= 5 ? Math.ceil(maxValue) + 5 : Math.ceil(maxValue);
      beginAtZero = false;
    }
    const hue = getRandomHue();
    const todayFillColor = pastelColorByHue(hue, 0.8);
    const yesterdayFillColor = pastelColorByHue(hue, 0.3);
    const solidColor = pastelColorByHue(hue, 1);
    const chart = new Chart(canvas.getContext('2d'), {
      type: 'line',
      data: {
        labels: hours,
        datasets: [
          {
            label: `${metric.label} (오늘)`,
            data: todayData,
            backgroundColor: todayFillColor,
            borderColor: solidColor,
            fill: true,
            tension: 0.4,
            pointRadius: 2,
          },
          {
            label: `${metric.label} (어제)`,
            data: yesterdayData,
            backgroundColor: yesterdayFillColor,
            borderColor: 'transparent',
            fill: true,
            tension: 0.4,
            pointRadius: 0,
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        height: 200,
        interaction: { mode: 'index', intersect: false, axis: 'x' },
        layout: { padding: 0 },
        scales: {
          x: { title: { display: true, text: '시간 (00~23)' } },
          y: {
            title: { display: true, text: '값' },
            suggestedMin,
            suggestedMax,
            ticks: {
              stepSize,
              beginAtZero,
              callback: (value) => Math.floor(value)
            }
          }
        },
        plugins: {
          legend: { position: 'top' },
        }
      }
    });
    chart.canvas.addEventListener('mousemove', syncCharts);
    chartInstances.push(chart);
  });
};

function splitYesterdayTodayData(dataList, key) {
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  const todayStr = today.toISOString().slice(0, 10).replace(/-/g, '/');
  const yesterdayStr = yesterday.toISOString().slice(0, 10).replace(/-/g, '/');
  const todayData = new Array(24).fill(null);
  const yesterdayData = new Array(24).fill(null);
  dataList.forEach(item => {
    const chkDateStr = item.id.chkDate; // '2025/04/29 09:00:00'
    if (!chkDateStr) return;
    const datePart = chkDateStr.slice(0, 10);
    const hourPart = parseInt(chkDateStr.slice(11, 13), 10);
    if (datePart === todayStr) {
      todayData[hourPart] = item[key] ?? null;
    } else if (datePart === yesterdayStr) {
      yesterdayData[hourPart] = item[key] ?? null;
    }
  });
  const nowHour = new Date().getHours();
  for (let i = nowHour + 1; i < 24; i++) {
    todayData[i] = null;
  }
  return { yesterdayData, todayData };
}

onMounted(() => { fetchDbList(); });
</script>


<style scoped>
/* ====== 전체 레이아웃 ====== */
.container {
  display: flex;
  height: 100%;
  background-color: #f4f4f4;
}

/* ====== DB 트리(왼쪽) ====== */
.db-tree {
  position: sticky;
  top: 15px; /* 스크롤 시 상단에서 15px 떨어져 고정 */
  align-self: flex-start;
  width: 250px;
  max-height: 90vh;
  height: fit-content;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  overflow-y: auto;
  flex-shrink: 0;
  z-index: 10;
}

/* ====== 차트 컨테이너(오른쪽) ====== */
.chart-container {
  flex-grow: 1;
  width: 900px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  min-height: 20vh;
  overflow: hidden;
}

/* ====== 차트 래퍼 & 개별 차트 ====== */
.charts-wrapper {
  flex-grow: 1;
  overflow-y: auto;
  padding-right: 10px;
}
.metric-chart {
  width: 100%;
  height: 200px;
  margin-bottom: 20px;
}

/* ====== 선택 DB 강조 스타일 ====== */
.active-db,
.active-db:hover,
.active-db:focus {
  background: #6366f1 !important;   /* 진한 보라색 */
  color: #fff !important;
  font-weight: bold !important;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(99,102,241,0.15);
}

/* 기본 hover 제거(선택된 DB 제외) */
.p-panelmenu .p-menuitem-link:not(.active-db):hover {
  background: transparent !important;
  color: inherit !important;
  font-weight: normal !important;
  box-shadow: none !important;
}

/* ====== 반응형(모바일) ====== */
@media (max-width: 768px) {
  .container {
    flex-direction: column;
    margin-left: 0;
  }
  .db-tree {
    position: static !important;
    width: 100%;
    max-height: none;
    height: auto;
    border-radius: 8px;
    margin-bottom: 10px;
    z-index: 1;
    top: unset;
    left: unset;
    box-shadow: 0 2px 6px rgba(0,0,0,0.04);
  }
  .chart-container {
    width: 100%;
    padding: 10px;
    min-width: 0;
  }

}
</style>
