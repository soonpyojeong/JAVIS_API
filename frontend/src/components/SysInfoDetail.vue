<template>
  <div class="container-wrapper">
    <!-- 왼쪽 HOSTNAME 리스트 -->
    <div class="db-tree">
      <h3>DBMS 목록</h3>
      <ul>
        <li
          v-for="item in sortedHostList"
          :key="item.id"
          @click="selectHost(item.hostname)"
          class="host-item"
        >
          {{ item.hostname }}
        </li>
      </ul>
    </div>

    <!-- 가운데 시스템 정보 상세 -->
    <div class="sysinfo-detail">
      <!-- 🖥️ 서버 정보 영역 -->
      <div class="host-info">
        <h2>{{ summary.hostname }}</h2>
        <p>수집 시간: {{ summary.checkDate }}</p>
      </div>

      <!-- ⚙️ 요약 카드 영역 -->
      <div class="summary-cards">
        <div class="card">CPU 사용률<br /><strong>{{ summary.cpuUsage }}%</strong></div>
        <div class="card">메모리 사용률<br /><strong>{{ summary.memUsage }}%</strong></div>
        <div class="card">디스크 사용률<br /><strong>{{ summary.diskUsage }}%</strong></div>
      </div>

      <!-- 📊 디스크 정보 차트 + 테이블 -->
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
            <tr v-for="disk in disks" :key="disk.id">
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

      <!-- 📋 로그 영역 -->
      <div class="log-section">
        <h3>시스템 로그</h3>
        <div class="log-list">
          <div class="log-item" v-for="log in logs" :key="log.id">
            <span class="log-type">[{{ log.logType }}]</span>
            <span class="log-date">{{ log.logDate }}</span>
            <div class="log-message">{{ log.message }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed,onMounted, ref, nextTick } from 'vue';
import Chart from 'chart.js/auto';
import api from '@/api';

// 📦 상태 변수
const summary = ref({});
const disks = ref([]);
const logs = ref([]);
const chartKey = ref(0);
let chartInstance = null;
const hostList = ref([]);

const sortedHostList = computed(() => {
  return Array.isArray(hostList.value)
    ? [...hostList.value].sort((a, b) => a.hostname.localeCompare(b.hostname))
    : [];
});

// ✅ 시스템 정보 조회 (기본 또는 hostname 기준)
const fetchSysInfo = async (hostname = null) => {
  const url = hostname
    ? `/api/sysinfo/latest?hostname=${hostname}`
    : '/api/sysinfo/latest';

  const { data } = await api.get(url);
  summary.value = data.summary;
  disks.value = data.disks;
  logs.value = data.logs;
  chartKey.value += 1;
  nextTick(() => renderDiskChart());
};




// ✅ 호스트 목록 조회
const fetchHostList = async () => {
  const res = await api.get('/api/sysinfo/hostnames');
  hostList.value = res.data;
};

// ✅ 호스트 선택 시 최신 정보 조회
const selectHost = (hostname) => {
  fetchSysInfo(hostname);
};

// ✅ 디스크 차트 렌더링
const renderDiskChart = () => {
  const ctx = document.getElementById('diskChart');
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
      plugins: {
        legend: {
          labels: {
            color: '#333',
            font: { size: 14, weight: 'bold' }
          }
        },
        title: { display: false }
      },
      scales: {
        x: {
          max: 100,
          ticks: { color: '#000', font: { size: 13, weight: 'bold' } },
          title: {
            display: true,
            text: '사용률 (%)',
            color: '#000',
            font: { weight: 'bold', size: 14 }
          }
        },
        y: {
          ticks: { color: '#000', font: { size: 13, weight: 'bold' } }
        }
      }
    }
  });
};

// 초기 실행
onMounted(() => {
  fetchHostList();
});
</script>

<style scoped>
.container-wrapper {
  display: flex;
  height: 100%;
  background-color: #f4f4f4;
}

.db-tree {
  width: 250px;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  height: 100vh;
  overflow-y: auto;
  flex-shrink: 0;
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
  background-color: #e0f7fa;
}

.sysinfo-detail {
  flex-grow: 1;
  width: 900px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 100vh;
}

.host-info {
  margin-bottom: 10px;
}
.summary-cards {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.card {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  text-align: center;
  font-weight: bold;
  background: #f9f9f9;
}
.disk-section {
  margin-bottom: 30px;
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
.log-section .log-list {
  margin-top: 10px;
}
.log-item {
  margin-bottom: 8px;
  padding: 8px;
  border-left: 4px solid #555;
  background: #f4f4f4;
}
.log-type {
  font-weight: bold;
  margin-right: 10px;
}

canvas#diskChart {
  width: 100% !important;
  height: 250px !important;
  max-height: 300px;
}
</style>
