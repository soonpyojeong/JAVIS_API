<template>
  <div class="container">
    <h2 class="text-2xl font-bold text-orange-600 mb-4">DB 테이블스페이스 및 임계치 통합 뷰</h2>

    <!-- DB 선택 드롭다운 -->
    <div class="select-container">
      <select v-model="selectedDb" @change="handleDbChange">
        <option value="">임계치만보기</option>
        <option v-for="db in sortedDbList" :key="db.dbName" :value="db.dbName">
          {{ db.dbName === 'NIRIS' ? 'IRIS3.0' : db.dbName }} {{ db.sizeChk === 'N' ? ' (미수집)' : '' }}
        </option>
      </select>
      <div class="refresh-wrapper" @mouseenter="showTooltip = true" @mouseleave="showTooltip = false">
        <button class="refresh-btn" :class="{ rotating: isRotating }" @click="handleRefreshClick">
          <svg class="refresh-icon" viewBox="0 0 24 24">
            <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6h-2c0 4.41 3.59 8 8 8s8-3.59 8-8-3.59-8-8-8z" fill="currentColor"/>
          </svg>
        </button>
        <div v-if="showTooltip" class="tooltip-card">
          DB 정보 새로고침
          <div class="tooltip-arrow"></div>
        </div>
      </div>
    </div>

    <!-- 경고 메시지 -->
    <p v-if="message" class="text-red-500 text-center mb-2">{{ message }}</p>

    <!-- 검색 필드 -->
    <div class="mb-4">
      <input v-model="searchQuery" type="text" placeholder="Tablespace 검색"
        class="w-full p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500" />
    </div>

    <!-- 테이블 -->
    <table class="tablespace-table" v-if="filteredTablespaces.length || showAllThresholds">
      <thead>
        <tr>
          <th @click="setSort('dbName')">DB명<span v-if="sortKey === 'dbName'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('tsName')">Tablespace<span v-if="sortKey === 'tsName'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('totalSize')">Total(MB)<span v-if="sortKey === 'totalSize'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('usedSize')">Used(MB)<span v-if="sortKey === 'usedSize'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('usedRate')">사용률<span v-if="sortKey === 'usedRate'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('freeSize')">Free(MB)<span v-if="sortKey === 'freeSize'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('dbType')">DB TYPE<span v-if="sortKey === 'dbType'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('thresMb')">임계치<span v-if="sortKey === 'thresMb'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('defThresMb')">기본 임계치<span v-if="sortKey === 'defThresMb'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
          <th @click="setSort('imsiDel')">관제3일조용<span v-if="sortKey === 'imsiDel'">{{ sortOrder === 1 ? '▲' : '▼' }}</span> </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ts in sortedTablespaces" :key="ts.id.dbName + '-' + ts.id.tsName">
          <td v-if="ts.id.dbName=='NIRIS'">IRIS3.0</td>
          <td v-else>{{ts.id.dbName}}</td>
          <td>{{ ts.id.tsName }}</td>
          <td>{{ formatNumber(ts.totalSize) }}</td>
          <td>{{ formatNumber(ts.usedSize) }}</td>
          <td :class="{ 'text-red-500 font-bold bg-red-100': Number(ts.usedRate) >= 85 }">
            {{ formatNumber(ts.usedRate) }}%
          </td>
          <td>{{ formatNumber(ts.freeSize) }}</td>
          <td>{{ ts.dbType }}</td>
          <td>
            <div class="flex justify-end items-center gap-2">
              <div v-show="!ts.isEditing">
                <span v-if="ts.thresMb != null" @click="startEditing(ts)" class="cursor-pointer text-orange-500 hover:underline">
                  {{ formatNumber(ts.thresMb) }}
                </span>
                <button v-else class="add-threshold-button" @click="openAddThresholdModal(ts)">+</button>
              </div>
              <div v-show="ts.isEditing">
                <input
                  :ref="el => {
                    if (el && thresInputMap?.value instanceof Map && ts.id?.dbName && ts.id?.tsName) {
                      thresInputMap.value.set(ts.id.dbName + '_' + ts.id.tsName, el);
                    }
                  }"
                  v-model="ts.editedValue"
                  @keyup.enter="handleEnter(ts); handleBlur(ts)"
                  @blur="handleBlur(ts)"
                  type="number"
                  class="w-20 p-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500"
                />
                <button @click="resetToDefault(ts)" title="기본값으로 설정">
                  <i class="pi pi-refresh"></i>
                </button>
                <button @click="updateThreshold(ts)" class="bg-blue-500 text-white px-2 py-1 rounded">저장</button>
              </div>
            </div>
          </td>
          <td>
            <div class="flex justify-end items-center gap-2">
              <span v-show="!ts.editingDefault" @click="startEditingDefault(ts)" class="cursor-pointer text-blue-600 hover:underline">
                {{ formatNumber(ts.defThresMb) }}
              </span>
              <input v-show="ts.editingDefault" v-model="ts.editedDefault" @keyup.enter="updateDefaultThreshold(ts)" @blur="cancelEditingDefault(ts)"
                type="number" class="w-20 p-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </td>
          <td>
            <template v-if="ts.imsiDel">
              <div>{{ new Date(ts.imsiDel).toLocaleDateString() }}</div>
            </template>
            <template v-else-if="ts.thresMb != null && ts.defThresMb != null">
              <div class="flex justify-center">
                <button @click="releaseThreshold(ts.thresholdId)">해제</button>
              </div>
            </template>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="tablespaces.length === 0">임계치 설정된 테이블스페이스가 없습니다.</p>

    <!-- 메시지 모달 -->
    <div v-if="showMessageModal" class="modal-overlay">
      <div class="modal">
        <p>{{ messageModalText }}</p>
        <button @click="closeMessageModal" class="modal-close-btn">확인</button>
      </div>
    </div>

     <!-- 임계치 추가 모달 -->
        <div v-if="isAddModalVisible" class="modal-overlay">
          <div class="modal">
            <h3 class="font-bold text-lg mb-4">임계치 추가</h3>
            <form @submit.prevent="saveNewThreshold">
              <div class="form-group mb-2">
                <label class="block font-medium">DB 이름</label>
                <input type="text" v-model="addModalData.dbName" readonly class="w-full border p-2 rounded" />
              </div>
              <div class="form-group mb-2">
                <label class="block font-medium">Tablespace</label>
                <input type="text" v-model="addModalData.tablespaceName" readonly class="w-full border p-2 rounded" />
              </div>
              <div class="form-group mb-2">
                <label class="block font-medium">DB 타입</label>
                <input type="text" v-model="addModalData.dbType" readonly class="w-full border p-2 rounded" />
              </div>
              <div class="form-group mb-2">
                <label class="block font-medium">임계치 (MB)</label>
                <input type="number" v-model="addModalData.thresMb" required class="w-full border p-2 rounded" />
              </div>
              <div class="form-group mb-2">
                <label class="block font-medium">체크 여부</label>
                <select v-model="addModalData.chkFlag" class="w-full border p-2 rounded">
                  <option value="Y">Y</option>
                  <option value="N">N</option>
                </select>
              </div>
              <div class="form-group mb-2">
                <label class="block font-medium">코멘트</label>
                <textarea v-model="addModalData.commt" class="w-full border p-2 rounded"></textarea>
              </div>
              <div class="flex justify-end gap-2 mt-4">
                <button type="submit" class="bg-green-500 text-white px-4 py-2 rounded">저장</button>
                <button type="button" @click="closeAddModal" class="bg-gray-400 text-white px-4 py-2 rounded">취소</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </template>
<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useStore } from 'vuex';
import api from '@/api';
import Chart from 'chart.js/auto';
import ChartDataLabels from 'chartjs-plugin-datalabels';

const store = useStore();
const selectedDb = ref('DB 선택');
const dbList = ref([]);
const tablespaces = ref([]);
const searchQuery = ref('');
const chartInstances = ref({});
const showTooltip = ref(false);
const isRotating = ref(false);
const showMessageModal = ref(false);
const messageModalText = ref('');
const message = ref('');
const isAddModalVisible = ref(false);
const addModalData = ref({ dbName: '', tablespaceName: '', dbType: '', thresMb: 0, chkFlag: 'Y', commt: '' });
const sortKey = ref('');
const sortOrder = ref(1); // 1: 오름차순, -1: 내림차순

const allThresholds = ref([]);
const thresInputMap = ref(new Map());



function handleEnter(ts) {
  console.log('ENTER PRESSED FOR:', ts);
  updateThreshold(ts);
}

function handleBlur(ts) {
  ts.isEditing = false;
  ts.editedValue = null;
}

function fetchAllThresholds() {
  api.get('/api/threshold/all').then((res) => {
    //console.log('전체 임계치 목록:', res.data); // 🔍 콘솔 확인
    allThresholds.value = res.data || [];

  });
}

const sortedTablespaces = computed(() => {
  const list = [...filteredTablespaces.value];

  if (sortKey.value) {
    list.sort((a, b) => {
      const aVal = getSortValue(a, sortKey.value);
      const bVal = getSortValue(b, sortKey.value);

      if (aVal == null) return 1;
      if (bVal == null) return -1;

      if (typeof aVal === 'number') return (aVal - bVal) * sortOrder.value;
      return String(aVal).localeCompare(String(bVal)) * sortOrder.value;
    });
  }

  return list;
});

function getSortValue(obj, key) {
  if (key === 'dbName') return obj.id.dbName;
  if (key === 'tsName') return obj.id.tsName;
  return obj[key];
}

function setSort(key) {
  if (sortKey.value === key) {
    sortOrder.value *= -1; // 같은 키 다시 누르면 정렬 반전
  } else {
    sortKey.value = key;
    sortOrder.value = 1;
  }
}


const showAllThresholds = ref(true); // 기본값: 전체 표시

function fetchThresholdsWithUsage() {
  api.get('/api/threshold/with-usage').then((res) => {
    const result = res.data || [];
    //console.log('[임계치 리스트 확인]', result);
    tablespaces.value = result.map((item) => ({
      id: {
        dbName: item.dbName,
        tsName: item.tablespaceName,
      },
      dbType: item.dbType,
      totalSize: item.totalSize,
      usedSize: item.usedSize,
      usedRate: item.usedRate,
      freeSize: item.freeSize,
      thresholdId: item.id, // 없어도 괜찮음 (id가 없으면 임계치 없는 상태로 처리)
      thresMb: item.thresMb,
      defThresMb: item.defThresMb,
      imsiDel: item.imsiDel,
      isEditing: false,
      editingDefault: false,
      editedValue: null,
      editedDefault: null,
    }));
  });
}




onMounted(async () => {
  await fetchDbList();
  await fetchThresholdsWithUsage();
  await fetchAllThresholds();

  // 전체 threshold 목록 기반으로 tablespaces 구성
  const mapped = allThresholds.value.map((thr) => ({
    id: {
      dbName: thr.dbName,
      tsName: thr.tablespaceName
    },
    dbType: thr.dbType,
    totalSize: thr.totalSize,      // 필요한 경우 API로 채움
    usedSize: thr.usedSize,
    usedRate: thr.usedRate,
    freeSize: thr.freeSize,
    thresholdId: thr.id,
    thresMb: thr.thresMb,
    defThresMb: thr.defThresMb,
    imsiDel: thr.imsiDel,
    isEditing: false,
    editingDefault: false,
    editedValue: null,
    editedDefault: null,
  }));

  tablespaces.value = mapped;
});

function fetchDbList() {
  api.get('/api/db-list').then((res) => {
    dbList.value = Array.isArray(res.data) ? res.data : [];
  });
}


function handleDbChange() {
      if (!selectedDb.value) {
        // ✅ '임계치만보기' 선택 시, 사용률 포함된 최신 임계치 목록 재조회
        fetchThresholdsWithUsage();  // 👈 여기 추가
        return;
      }

    const selected = dbList.value.find((d) => d.dbName === selectedDb.value);
    if (!selected || selected.sizeChk === 'N') {
    message.value = '해당 DB는 테이블스페이스 수집이 되지 않았습니다.';
    tablespaces.value = [];
    return;
    }

  message.value = '';
  api.get(`/api/tb/${selectedDb.value}/tablespaces`).then((res) => {
    const data = res.data.map((ts) => {
      const match = allThresholds.value.find(
        (thr) =>
          thr.dbName === ts.id.dbName &&
          thr.dbType === ts.dbType &&
          thr.tablespaceName === ts.id.tsName
      );

      //console.log('[매칭 결과]', match, 'for ts:', ts);

      return {
        ...ts,
        thresholdId: match?.id ?? null,
        thresMb: match?.thresMb ?? null,
        defThresMb: match?.defThresMb ?? null,
        imsiDel: match?.imsiDel ?? null,
        isEditing: false,
        editingDefault: false,
        editedValue: null,
        editedDefault: null,
      };
    });


    tablespaces.value = data;

  });
}



const filteredTablespaces = computed(() => {
  const keyword = (searchQuery.value || '').toLowerCase();
  return tablespaces.value.filter(ts =>
    `${ts.id.dbName}.${ts.id.tsName}`.toLowerCase().includes(keyword)
  );
});


function formatNumber(num) {
  return num != null ? num.toLocaleString() : '-';
}

function handleRefreshClick() {
  isRotating.value = true;
  api.post('/api/tb/dbList/refresh')
    .then(() => {
      openMessageModal('DB 목록이 새로고침되었습니다!');
      fetchDbList();
    })
    .finally(() => setTimeout(() => (isRotating.value = false), 1000));
}

function openMessageModal(message) {
  messageModalText.value = message;
  showMessageModal.value = true;
}

function closeMessageModal() {
  showMessageModal.value = false;
}

// DB 목록 정렬된 computed
const sortedDbList = computed(() => {
  return [...dbList.value].sort((a, b) => a.dbName.localeCompare(b.dbName))
})

function startEditing(ts) {
  if (ts.thresMb == null) return openAddThresholdModal(ts);
  ts.isEditing = true;
  ts.editedValue = ts.thresMb;

  nextTick(() => {
    const input = thresInputMap.value.get(ts.id.dbName + '_' + ts.id.tsName);
    if (input) {
      input.focus();
      input.select();
    }
  });
}

function cancelEditing(ts) {
  ts.isEditing = false;
  ts.editedValue = null;
}

function resetToDefault(ts) {
  ts.editedValue = ts.defThresMb;
}

function updateThreshold(tablespace) {
  const username = store.state.user?.username || 'unknown';
  if (!tablespace.thresholdId) return;

  api.put(`/api/threshold/${tablespace.thresholdId}`, {
    id: tablespace.thresholdId,
    thresMb: tablespace.editedValue,
    username
  }).then((res) => {
    if (res.data) {
      tablespace.thresMb = tablespace.editedValue;
      tablespace.isEditing = false;
    }
  });
}

function startEditingDefault(ts) {
  ts.editingDefault = true;
  ts.editedDefault = ts.defThresMb;
}

function cancelEditingDefault(ts) {
  ts.editingDefault = false;
  ts.editedDefault = null;
}

function updateDefaultThreshold(tablespace) {
  const username = store.state.user?.username || 'unknown';
  if (!tablespace.thresholdId) return;

  api.put(`/api/threshold/${tablespace.thresholdId}/default`, {
    defThresMb: tablespace.editedDefault,
    commt: username
  }).then((res) => {
    if (res.data) {
      console.log('임계치 변경 결과:', res.data);
      tablespace.defThresMb = tablespace.editedDefault;
      tablespace.editingDefault = false;
    }
  });
}


function releaseThreshold(thresholdId) {
  if (!thresholdId || typeof thresholdId !== 'number') {
    //console.warn('[임시해제] 잘못된 thresholdId:', thresholdId);
    return;
  }

  api.put(`/api/threshold/${thresholdId}/release`)
    .then((res) => {
      if (res.data) {
        alert('임시해제가 완료되었습니다.');
        refreshThresholds();
      }
    })
    .catch((err) => {
      console.error('임시해제 실패:', err);
    });
}


// 데이터 새로고침
function refreshThresholds() {
  api.get('/api/threshold/all')
    .then((res) => {
      thresholds.value = res.data.map((t) => ({
        ...t,
        isEditing: false,
        editedValue: null,
      }));
    })
    .catch((err) => {
      console.error('데이터 로딩 실패:', err);
    });
}

function openAddThresholdModal(ts) {
  addModalData.value = {
    dbName: ts.id.dbName,
    tablespaceName: ts.id.tsName,
    dbType: ts.dbType,
    thresMb: ts.freeSize,
    chkFlag: 'Y',
    commt: '',
  };
  isAddModalVisible.value = true;
}

function saveNewThreshold() {
  const username = store.state.user?.username || 'unknown';
  const payload = { ...addModalData.value, username };
  api.post('/api/threshold/save', payload)
    .then(() => {
      isAddModalVisible.value = false;
      openMessageModal('임계치가 저장되었습니다.');
      handleDbChange();
    });
}

function closeAddModal() {
  isAddModalVisible.value = false;
}
</script>



<style scoped>
/* 기본 설정 */
.container {
  font-family: 'Arial', sans-serif;
  padding: 30px;
  max-width: 1450px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.1);
  transition: 0.3s;
}

.container:hover {
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.2); /* 마우스 오버 시 그림자 효과 */
}

h2 {
  color: #333;
  text-align: center;
  font-size: 28px;
  margin-bottom: 30px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: color 0.3s;
  padding: 30px;
}

h2:hover {
  color: #4caf50; /* 마우스 오버 시 색상 변화 */
}

/* 드롭다운 스타일 */
.select-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
select {
  padding: 12px 18px;
  font-size: 16px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  background-color: #f4f7f6;
  color: #333;
  transition: 0.3s;
}

select:focus {
  border-color: #4caf50;
  box-shadow: 0 0 6px rgba(76, 175, 80, 0.4);
  background-color: #e8f5e9;
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: transparent;
  border: 5px solid #3498db;
  color: #3498db;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
}

.refresh-btn:hover {
  background-color: rgba(52, 152, 219, 0.1);
}

.refresh-icon {
  width: 20px;
  height: 20px;
  transition: transform 0.5s ease;
}
.refresh-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ✅ 클릭하거나 hover할 때 부드럽게 회전 */
.refresh-btn:hover .refresh-icon,
.refresh-btn.rotating .refresh-icon {
  transform: rotate(360deg);
}

/* 버튼 스타일 (파스텔 느낌) */
button {
  padding: 12px 20px;
  font-size: 16px;
  border-radius: 5px;
  background-color: #4caf50;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.3s ease;
}

button:hover {
  background-color: #388e3c;
  transform: translateY(-2px);
}

button:focus {
  outline: none;
  box-shadow: 0 0 8px rgba(76, 175, 80, 0.6);
}

/* 테이블 스타일 */
.tablespace-table {
  width: 100%;
  border-collapse: collapse;
  background-color: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.tablespace-table th, .tablespace-table td {
  padding: 10px 10px; /* 패딩을 줄여서 높이를 조정 */
  text-align: center;
  border: 1px solid #ddd; /* 테이블 경계선 */
}

.tablespace-table th {
  background-color: #4caf50;
  color: #fff;
  font-weight: 600;
  text-transform: uppercase;
  cursor: pointer;
}

.tablespace-table th:hover {
  background-color: #388e3c;
}

.tablespace-table td {
  font-size: 16px;
  color: #555;
  padding: 3px 10px; /* 패딩을 줄여서 높이를 조정 */
}

.tablespace-table td.used-rate {
  width: 100px; /* 고정된 가로 크기 */
  min-width: 100px; /* 최소 가로 크기 */
  max-width: 100px; /* 최대 가로 크기 */
  height: 40px; /* 고정된 세로 크기 */
  text-align: center;
  justify-content: flex-start;  /* 왼쪽 정렬 */
  padding-left: 0; /* 여백 제거 */
}

.tablespace-table td.free-size, .tablespace-table td.used-size {
  text-align: right;
}

.tablespace-table tr:hover {
  background-color: #f9f9f9;
}

/* 차트 스타일 */
.used-rate-container {
  width: 100%;
  height: 42px;
  display: flex;
  justify-content: flex-start; /* ✅ 왼쪽 정렬 유지 */
  align-items: center; /* ✅ 수직 정렬 정확히 */
  padding-left:1px;  /* ✅ 좌측 여백 약간 줘서 답답함 방지 */
  box-sizing: border-box;
}

.rate-chart {
  width: 90%;           /* ✅ 100% 대신 적당히 조절 가능 */
  height: 28px;         /* ✅ 고정 높이로 안정된 시각 제공 */
  max-width: 240px;     /* ✅ 너무 커지지 않게 제한 */
  border-radius: 4px;   /* ✅ 부드러운 느낌 */
}

.used-rate-td {
  width: 100px; /* ✅ td 너비를 원하는 크기로 지정 */
  min-width: 100px; /* ✅ 최소 크기 설정 (선택) */
  max-width: 100px; /* ✅ 최대 크기 제한 (선택) */
  padding: 4px 8px; /* ✅ 패딩 여유 */
  text-align: left; /* ✅ 안쪽 내용 왼쪽 정렬 */
}

/* 반응형 스타일 */
@media (max-width: 768px) {
  .container {
    padding: 20px;
  }

  h2 {
    font-size: 24px;
  }

  .tablespace-table th, .tablespace-table td {
    padding: 1px;
    font-size: 15px;
  }

  button {
    font-size: 14px;
    padding: 10px 18px;
  }
}
.add-threshold-button {
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s ease;
}

.add-threshold-button:hover {
  background-color: #388e3c;
}

.add-threshold-button:focus {
  outline: none;
  box-shadow: 0 0 4px rgba(76, 175, 80, 0.5);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  width: 400px;
  max-width: 90%;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

input, textarea {
  width: 50%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 5px;
}

button {
  margin-top: 10px;
  padding: 10px 15px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

button[type="submit"] {
  background: #4caf50;
  color: white;
}

button[type="button"] {
  background: #f44336;
  color: white;
}

button:hover {
  opacity: 0.9;
}


.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(0,0,0,0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.modal {
  background: white;
  padding: 20px 30px;
  border-radius: 10px;
  text-align: center;
  box-shadow: 0 5px 15px rgba(0,0,0,0.3);
}

.modal-close-btn {
  margin-top: 20px;
  padding: 8px 16px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.modal-close-btn:hover {
  background-color: #2980b9;
}
.tooltip-card {
  position: absolute;
  top: -38px; /* 더 가까워짐 */
  left: 50%;
  transform: translateX(-50%);
  background: white;
  color: #333;
  padding: 5px 10px;
  font-size: 15px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  white-space: nowrap;
  z-index: 100;
  transition: opacity 0.2s ease;
}


.tooltip-arrow {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid white;
}
</style>