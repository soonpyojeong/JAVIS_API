<template>
  <div class="container">
    <!-- 왼쪽 DB 트리 구조 -->
    <div class="db-tree">
      <ul>
        <li>
          <span @click="toggleDBType('oracle')" class="tree-node oracle">
            <span class="toggle-icon">{{ expandedDBType.oracle ? '-' : '+' }}</span> ORACLE
          </span>
          <ul v-if="expandedDBType.oracle" class="sub-tree">
            <li
              v-for="instance in oracleDBList"
              :key="instance"
              class="db-item"
              :class="{ selected: selectedDB === instance }"
            >
              <span @click="selectDB(instance, 'oracle')">
                <span class="leaf-icon"></span> {{ instance }}
              </span>
            </li>
          </ul>
        </li>
        <li>
          <span @click="toggleDBType('tibero')" class="tree-node tibero">
            <span class="toggle-icon">{{ expandedDBType.tibero ? '-' : '+' }}</span> TIBERO
          </span>
          <ul v-if="expandedDBType.tibero" class="sub-tree">
            <li
              v-for="instance in tiberoDBList"
              :key="instance"
              class="db-item"
              :class="{ selected: selectedDB === instance }"
            >
              <span @click="selectDB(instance, 'tibero')">
                <span class="leaf-icon"></span> {{ instance }}
              </span>
            </li>
          </ul>
        </li>
      </ul>
    </div>

    <!-- 차트 영역 -->
    <div class="chart-container">
      <div class="charts-wrapper">
        <template v-if="selectedDB && selectedDBType === 'tibero'">
          <!-- TIBERO 고정 지표 차트 -->
          <div class="metric-chart">
            <canvas id="tiberoFixedChart"></canvas>
          </div>
          <!-- TIBERO 랜덤 지표 차트 -->
          <div
            v-for="(metric, index) in randomMetrics"
            :key="index"
            class="metric-chart"
          >
            <canvas :id="'chartRef_' + index"></canvas>
          </div>
        </template>

        <template v-if="selectedDB && selectedDBType === 'oracle'">
          <!-- ORACLE 고정 지표 차트 -->
          <div class="metric-chart">
            <canvas id="oracleFixedChart"></canvas>
          </div>
          <!-- ORACLE 개별 지표 차트 -->
          <div
            v-for="(metric, index) in oracleMetrics"
            :key="'oracle_' + index"
            class="metric-chart"
          >
            <canvas :id="'oracleChart_' + index"></canvas>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>


<script>
import { ref, nextTick, onMounted } from 'vue';
import axios from 'axios';
import Chart from 'chart.js/auto';

export default {
  setup() {
    const expandedDBType = ref({ oracle: false, tibero: false });
    const selectedDB = ref(null);
    const selectedDBType = ref(null);

    const oracleDBList = ref([]);
    const tiberoDBList = ref([]);

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
      { key: 'dictHit', label: 'DICTIONARY CACHE HIT' }
    ]);

    const dbData = ref([]);
    const tbdbData = ref([]);
    const metricsToUse = ref(metrics.value);

    const fixedMetrics = ['maxlSess', 'totalSess', 'runSess'];
    const orafixedMetrics = ['totalSess', 'activeSess', 'transaTions'];

    const randomMetrics = ref([]);
    const oracleMetrics = ref([]);

    const fetchDbList = async () => {
      try {
        const response = await axios.get('/api/dailychk/db-list');
        const dbList = response.data;
        oracleDBList.value = dbList.ORACLE || [];
        tiberoDBList.value = dbList.TIBERO || [];
      } catch (error) {
        console.error("DB 목록을 불러오는 데 실패했습니다.", error);
      }
    };

    const toggleDBType = (type) => {
      expandedDBType.value[type] = !expandedDBType.value[type];
    };

    const selectDB = async (instanceName, dbType) => {
      selectedDB.value = instanceName;
      selectedDBType.value = dbType;

      if (dbType === 'oracle') {
        await fetchDbData(instanceName);
        metricsToUse.value = metrics.value;
        oracleMetrics.value = metrics.value;
        randomMetrics.value = [];
      } else if (dbType === 'tibero') {
        await fetchTbDbData(instanceName);
        metricsToUse.value = tbmetrics.value;
        randomMetrics.value = tbmetrics.value.filter(metric => !fixedMetrics.includes(metric.key));
        oracleMetrics.value = [];
      }

      nextTick(() => {
        renderCharts();
      });
    };

    const fetchDbData = async (instanceName) => {
      try {
        const response = await axios.get(`/api/dailychk/${instanceName}/oradata`);
        dbData.value = response.data;
      } catch (error) {
        console.error("ORACLE DB 데이터를 불러오는 데 실패했습니다.", error);
      }
    };

    const fetchTbDbData = async (instanceName) => {
      try {
        const response = await axios.get(`/api/dailychk/${instanceName}/tbdata`);
        tbdbData.value = response.data;
      } catch (error) {
        console.error("TIBERO DB 데이터를 불러오는 데 실패했습니다.", error);
      }
    };

    const getRandomPastelColor = () => {
      const hue = Math.floor(Math.random() * 360);
      const saturation = 70 + Math.random() * 30;
      const lightness = 70 + Math.random() * 10;
      return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    };



    const renderRandomOraCharts = () => {
      if (!dbData.value || dbData.value.length === 0) return;

      dbData.value.sort((a, b) => new Date(a.id.chkDate) - new Date(b.id.chkDate));
      const labels = dbData.value.map(item => item.id.chkDate);

      // 고정 metric을 제외한 metric만 대상으로 필터링
      const dynamicOracleMetrics = oracleMetrics.value.filter(metric => !orafixedMetrics.includes(metric.key));

      dynamicOracleMetrics.forEach((metric, index) => {
        const data = dbData.value.map(item => {
          const value = item[metric.key] ?? 0;

          // Buffer-related metrics에 대해 max값을 100으로 고정
          if (['bufferpct', 'reDoPct', 'buffHit', 'latchHitPct', 'libHitPct', 'softPct','inMemorySort','nonParseCpu'].includes(metric.key)) {
            return Math.min(value, 100);  // 100을 초과하는 값은 100으로 설정
          }

          return value;  // 나머지 지표는 정상적으로 표시
        });

        const canvas = document.getElementById(`oracleChart_${index}`);

        if (canvas) {
          if (canvas.chart) canvas.chart.destroy();

          const chart = new Chart(canvas, {
            type: 'line',
            data: {
              labels,
              datasets: [{
                label: metric.label,
                data,
                borderColor: getRandomPastelColor(),
                backgroundColor: getRandomPastelColor() + '50',
                fill: false,
                tension: 0.3,
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                x: { title: { display: true, text: 'Date' } },
                y: {
                  title: { display: true, text: 'Value' },
                  // bufferpct 관련 지표들만 Y축 max 값을 100으로 설정하고, 나머지 지표는 자동으로 설정
                  stepsize:5,
                  max: ['bufferpct', 'reDoPct', 'buffHit', 'latchHitPct', 'libHitPct', 'softPct','inMemorySort','nonParseCpu'].includes(metric.key) ? 100 : undefined,
                }
              }
            }
          });

          canvas.chart = chart;
        }
      });
    };




    const renderOracleFixedChart = () => {
      if (!dbData.value || dbData.value.length === 0) return;

      dbData.value.sort((a, b) => new Date(a.id.chkDate) - new Date(b.id.chkDate));
      const labels = dbData.value.map(item => item.id.chkDate);

      const datasets = orafixedMetrics.map((key) => {
        const data = dbData.value.map(item => item[key] ?? 0);

        return {
          label: key,
          data,
          borderColor: getRandomPastelColor(),
          backgroundColor: getRandomPastelColor() + '50',
          fill: false,
          tension: 0.3,
        };
      });

      const canvas = document.getElementById('oracleFixedChart');
      if (canvas) {
        if (canvas.chart) canvas.chart.destroy();

        const chart = new Chart(canvas, {
          type: 'line',
          data: { labels, datasets },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              x: { title: { display: true, text: 'Date' } },
              y: { title: { display: true, text: 'Value' } }
            }
          }
        });

        canvas.chart = chart;
      }
    };


    const renderTiberoFixedChart = () => {
      if (!tbdbData.value || tbdbData.value.length === 0) return;

      tbdbData.value.sort((a, b) => new Date(a.id.chkDate) - new Date(b.id.chkDate));
      const labels = tbdbData.value.map(item => item.id.chkDate);

      const datasets = fixedMetrics.map((key) => {
        const data = tbdbData.value.map(item => item[key] ?? 0);
        return {
          label: key,
          data,
          borderColor: getRandomPastelColor(),
          backgroundColor: getRandomPastelColor() + '50',
          fill: false,
          tension: 0.3,
        };
      });

      const canvas = document.getElementById('tiberoFixedChart');
      if (canvas) {
        if (canvas.chart) canvas.chart.destroy();

        const chart = new Chart(canvas, {
          type: 'line',
          data: { labels, datasets },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              x: { title: { display: true, text: 'Date' } },
              y: { title: { display: true, text: 'Value' } }
            }
          }
        });

        canvas.chart = chart;
      }
    };

    const renderRandomTbCharts = () => {
      randomMetrics.value.forEach((metric, index) => {
        const canvas = document.getElementById(`chartRef_${index}`);
        if (canvas) {
          if (canvas.chart) canvas.chart.destroy();

          const data = tbdbData.value.map(item => {
            const value = item[metric.key] ?? 0;

            // bufferHit, libHit, dictHit 지표에 대해 max값을 100으로 고정
            if (['buffHit', 'libHit', 'dictHit'].includes(metric.key)) {
              return Math.min(value, 100);  // 100을 초과하는 값은 100으로 설정
            }

            return value;  // 나머지 지표는 제한 없이 정상적으로 표시
          });

          const chartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              x: { title: { display: true, text: 'Date' } },
              y: {
                title: { display: true, text: 'Value' },
              }
            }
          };

          // bufferHit, libHit, dictHit 지표에 대해서만 max값을 100으로 고정하고 stepSize를 5로 설정
          if (['buffHit', 'libHit', 'dictHit'].includes(metric.key)) {
            chartOptions.scales.y.max = 100;
          } else {
            // 나머지 지표는 Y축의 제한 없이 출력되도록 설정
            chartOptions.scales.y.max = undefined;  // max 제한 없애기
          }

          const chart = new Chart(canvas, {
            type: 'line',
            data: {
              labels: tbdbData.value.map(item => item.id.chkDate),
              datasets: [{
                label: metric.label,
                data,
                borderColor: getRandomPastelColor(),
                backgroundColor: getRandomPastelColor() + '50',
                fill: false,
                tension: 0.3,
              }]
            },
            options: chartOptions
          });

          canvas.chart = chart;
        }
      });
    };



    const renderCharts = () => {
      if (selectedDBType.value === 'oracle') {
        renderOracleFixedChart();
        renderRandomOraCharts();
      } else if (selectedDBType.value === 'tibero') {
        renderTiberoFixedChart();
        renderRandomTbCharts();
      }
    };

    onMounted(() => {
      fetchDbList();
    });

    return {
      expandedDBType,
      selectedDB,
      selectedDBType,
      oracleDBList,
      tiberoDBList,
      metricsToUse,
      dbData,
      tbdbData,
      randomMetrics,
      oracleMetrics,
      toggleDBType,
      selectDB,
    };
  },
};
</script>

<style scoped>
/* 전체 컨테이너 스타일 */
.container {
  display: flex;
  height: 100%; /* 전체 컨테이너 높이 100%로 설정, 필요에 따라 조정 가능 */
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

.tree-node:hover {
  background-color: #e1e1e1;
}

/* 토글 아이콘 여백 */
.toggle-icon {
  margin-right: 10px;
}

/* 서브 트리 스타일 */
.sub-tree {
  padding-left: 20px;
  margin-top: 10px;
}

/* DB 항목 스타일 */
.db-item {
  padding: 5px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.db-item:hover {
  background-color: #e9f1f7;
}

/* 차트 컨테이너 스타일 */
.chart-container {
  flex-grow: 1;
  width: 900px; /* 차트 컨테이너의 기본 너비, 필요에 따라 수정 */
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 100vh; /* 컨테이너가 최소 100vh 높이를 갖도록 설정 */
  /* height: 100vh; */
}

/* 차트 래퍼 스타일 */
.charts-wrapper {
  flex-grow: 1; /* 내부 요소가 차지할 공간을 자동으로 확장 */
  overflow-y: auto;
  padding-right: 10px;
}

/* 개별 차트 스타일 */
.metric-chart {
  width: 100%;
  height: 300px; /* 차트의 높이 설정 */
  margin-bottom: 20px;
}

/* 반응형 스타일 */
@media (max-width: 768px) {
  .container {
    flex-direction: column;
  }
  .db-tree {
    width: 100%; /* 모바일에서 DB 트리의 너비를 100%로 설정 */
    height: auto; /* 모바일에서 DB 트리의 높이를 자동으로 조정 */
  }
  .metric-chart {
    width: 100%; /* 차트의 너비를 100%로 설정 */
  }
}

/* 선택된 DB 항목 스타일 */
.db-item.selected {
  background-color: #d1e7fd;  /* 선택된 DB에 파란색 배경 */
  color: #1d70b8;  /* 텍스트 색상 강조 */
  font-weight: bold;  /* 텍스트 강조 */
}

/* 선택된 DB 항목 호버 스타일 */
.db-item.selected:hover {
  background-color: #a6c8f7;  /* 호버 시 배경 색상 변경 */
}
</style>


