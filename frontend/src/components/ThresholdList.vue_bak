<template>
  <div class="container mx-auto p-4">
    <h2 class="text-2xl font-bold text-orange-600 mb-4">임계치 리스트</h2>

    <!-- 검색 입력 필드 -->
    <div class="mb-4">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="DB 이름 또는 Tablespace 검색"
        class="w-full p-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
      />
    </div>

    <!-- 테이블 -->
    <div class="overflow-x-auto">
      <table class="min-w-full bg-white border border-gray-200 rounded-lg shadow-md">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100" @click="sortData('dbName')">DB 이름</th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100" @click="sortData('tablespaceName')">Tablespace</th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100" @click="sortData('thresMb')">임계치 (MB)</th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100">기본 임계치 (MB)</th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100" @click="sortData('dbType')">DB 타입</th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100" @click="sortData('imsiDel')">관제임시해제(3일)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(threshold, index) in filteredData" :key="index" class="hover:bg-gray-50">
            <td class="px-4 py-2 text-sm text-gray-700 text-center">{{ threshold.dbName }}</td>
            <td class="px-4 py-2 text-sm text-gray-700 text-center">{{ threshold.tablespaceName }}</td>
            <td class="px-4 py-2 text-sm text-gray-700 text-right">
              <div class="flex justify-end items-center gap-2">
                <span v-if="!threshold.isEditing" @click="startEditing(threshold)" class="cursor-pointer text-orange-500 hover:underline">
                  {{ formatNumber(threshold.thresMb) }}
                </span>
                <input
                  v-else
                  v-model="threshold.editedValue"
                  @keyup.enter="updateThreshold(threshold)"
                  @blur="cancelEditing(threshold)"
                  type="number"
                  class="w-20 p-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-orange-500"
                />
                <button
                  v-if="threshold.isEditing"
                  @click="resetToDefault(threshold)"
                  class="text-sm text-blue-500 hover:underline"
                >
                  기본값
                </button>
              </div>
            </td>
            <td class="px-4 py-2 text-sm text-gray-700 text-right">
              <div class="flex justify-end items-center gap-2">
                <span
                  v-if="!threshold.editingDefault"
                  @click="startEditingDefault(threshold)"
                  class="cursor-pointer text-blue-600 hover:underline"
                >
                  {{ formatNumber(threshold.defThresMb) }}
                </span>
                <input
                  v-if="threshold.editingDefault"
                  v-model="threshold.editedDefault"
                  @keyup.enter="updateDefaultThreshold(threshold)"
                  @blur="cancelEditingDefault(threshold)"
                  type="number"
                  class="w-20 p-1 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </td>
            <td class="px-4 py-2 text-sm text-gray-700 text-center">{{ threshold.dbType }}</td>
            <td class="px-4 py-2 text-sm text-gray-700 text-center">
              <template v-if="threshold.imsiDel">
                <div>{{ new Date(threshold.imsiDel).toLocaleDateString() }}</div>
              </template>
              <template v-else>
                <div class="flex justify-center">
                  <button class="text-blue-500 hover:underline" @click="releaseThreshold(threshold.id)">해제</button>
                </div>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="filteredData.length === 0" class="mt-4 text-sm text-gray-500">검색 결과가 없습니다.</p>
  </div>
</template>





<script setup>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import api from '@/api';

const store = useStore();

const thresholds = ref([]);
const searchQuery = ref('');
const sortBy = ref('');
const sortOrder = ref('asc');

// 포맷 숫자
const formatNumber = (num) => num.toLocaleString();

// 정렬 필드 설정
function sortData(column) {
  if (sortBy.value === column) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortBy.value = column;
    sortOrder.value = 'asc';
  }
}

// 정렬 로직
function sortDataBy(data) {
  return [...data].sort((a, b) => {
    const valA = a[sortBy.value];
    const valB = b[sortBy.value];

    if (sortBy.value === 'imsiDel') {
      const timeA = valA ? new Date(valA).getTime() : 0;
      const timeB = valB ? new Date(valB).getTime() : 0;
      return sortOrder.value === 'asc' ? timeA - timeB : timeB - timeA;
    }

    if (typeof valA === 'number' && typeof valB === 'number') {
      return sortOrder.value === 'asc' ? valA - valB : valB - valA;
    }

    if (typeof valA === 'string' && typeof valB === 'string') {
      return sortOrder.value === 'asc'
        ? valA.localeCompare(valB)
        : valB.localeCompare(valA);
    }

    return 0;
  });
}

// 필터된 데이터
const filteredData = computed(() => {
  if (!Array.isArray(thresholds.value)) return [];
  const query = searchQuery.value.toLowerCase();
  const filtered = thresholds.value.filter((t) => {
    return (
      t.dbName?.toLowerCase().includes(query) ||
      t.tablespaceName?.toLowerCase().includes(query)
    );
  });
  return sortDataBy(filtered);
});

// 편집 시작
function startEditing(threshold) {
  threshold.isEditing = true;
  threshold.editedValue = threshold.thresMb;
}

// 편집 취소
function cancelEditing(threshold) {
  threshold.isEditing = false;
  threshold.editedValue = null;
}

// 기본값으로 초기화
function resetToDefault(threshold) {
  if (!threshold) return;
  threshold.editedValue = threshold.defThresMb;
}

// 업데이트 실행
function updateThreshold(threshold) {
  const username = store.state.user.username;

  const updatedThreshold = {
    id: threshold.id,
    thresMb: threshold.editedValue,
    username,
  };

  api.put(`/api/threshold/${updatedThreshold.id}`, updatedThreshold)
    .then((res) => {
      if (res.data) {
        threshold.thresMb = threshold.editedValue;
        threshold.isEditing = false;
      } else {
        console.error('임계치 업데이트 실패');
      }
    })
    .catch((err) => {
      console.error('임계치 업데이트 오류:', err);
    });
}

// 임시 해제
function releaseThreshold(id) {
  api.put(`/api/threshold/${id}/release`)
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

function startEditingDefault(threshold) {
  threshold.editingDefault = true;
  threshold.editedDefault = threshold.defThresMb;
}

function cancelEditingDefault(threshold) {
  threshold.editingDefault = false;
  threshold.editedDefault = null;
}

function updateDefaultThreshold(threshold) {
  const username = store.state.user.username || '';
  const newValue = threshold.editedDefault;

  if (newValue === threshold.defThresMb) {
    threshold.editingDefault = false;
    return;
  }

  const payload = {
    defThresMb: newValue,
    commt: username,
  };

  api.put(`/api/threshold/${threshold.id}/default`, payload)
    .then((res) => {
      if (res.data) {
        threshold.defThresMb = newValue;
        threshold.editingDefault = false;
      } else {
        console.error('기본 임계치 업데이트 실패');
      }
    })
    .catch((err) => {
      console.error('기본 임계치 업데이트 오류:', err);
    });
}


// 마운트 시 초기 로딩
onMounted(() => {
  refreshThresholds();
});
</script>

<style scoped>
/* 공통 스타일 */
.container {
  font-family: 'Arial', sans-serif;
  padding: 20px;
  max-width: 1250px;
  margin: 0 auto;
  background: #ffffff; /* 흰색 배경 */
  border-radius: 10px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1); /* 그림자 강조 */
}


h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2980b9;
  text-align: center;
  margin-bottom: 20px;
  letter-spacing: 0.5px;
}

input:focus {
  outline: none;
  border-color: #2980b9;
  box-shadow: 0 0 6px rgba(41, 128, 185, 0.5);
}

/* 검색 필드 */
input {
  font-size: 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

input:focus {
  outline: none;
  border-color: #2980b9;
  box-shadow: 0 0 6px rgba(41, 128, 185, 0.5);
}

/* 테이블 */
table {
  border-collapse: collapse;
  width: 100%;
  margin-top: 10px;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

th,
td {
  padding: 14px;
  border: 1px solid #ddd;
}

th {
  background-color: #d6eaf8;
  color: #2980b9;
  font-weight: 600;
  text-transform: uppercase;
  cursor: pointer;
  text-align: center; /* 가운데 정렬 */
}

th:hover {
  background-color: #aed6f1;
}

td {
  font-size: 0.9rem;
  color: #555;
}

td:hover {
  background-color: #f9f9f9;
}

/* 임계치 필드 스타일 */
td span {
  cursor: pointer;
  color: #2980b9;
}

td span:hover {
  text-decoration: underline;
}

/* 숫자 입력 필드 스타일 */
input[type="number"] {
  padding: 6px;
  font-size: 0.9rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

input[type="number"]:focus {
  border-color: #2980b9;
  box-shadow: 0 0 4px rgba(41, 128, 185, 0.5);
}

/* 버튼 스타일 (푸른색 계열) */
button {
  padding: 12px 20px;
  font-size: 16px;
  border-radius: 8px;
  background-color: #2980b9;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.3s ease;
}

button:hover {
  background-color: #1f6fa3;
  transform: translateY(-2px);
}

button:focus {
  outline: none;
  box-shadow: 0 0 8px rgba(41, 128, 185, 0.6);
}

/* 반응형 스타일 */
@media (max-width: 768px) {
  .container {
    padding: 16px;
  }

  h2 {
    font-size: 1.25rem;
  }

  table {
    font-size: 0.85rem;
  }

  th,
  td {
    padding: 10px;
  }

  input {
    font-size: 0.9rem;
  }

  button {
    font-size: 14px;
    padding: 10px 18px;
  }
}

table td:nth-child(3),
table th:nth-child(3) {
  text-align: right; /* 오른쪽 정렬 */
}


</style>
