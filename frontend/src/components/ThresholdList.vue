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
            <th
              class="px-4 py-2 text-left text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('dbName')"
            >
              DB 이름
            </th>
            <th
              class="px-4 py-2 text-left text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('tablespaceName')"
            >
              Tablespace
            </th>
            <th
              class="px-4 py-2 text-right text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('thresMb')"
            >
              임계치 (MB)
            </th>
            <th
              class="px-4 py-2 text-left text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('dbType')"
            >
              DB 타입
            </th>
          </tr>
        </thead>
        <tbody>
          <!-- 필터링된 데이터를 표시 -->
          <tr
            v-for="(threshold, index) in filteredData"
            :key="index"
            class="hover:bg-gray-50"
          >
            <td class="px-4 py-2 text-sm text-gray-700">{{ threshold.dbName }}</td>
            <td class="px-4 py-2 text-sm text-gray-700">{{ threshold.tablespaceName }}</td>
            <td class="px-4 py-2 text-sm text-gray-700 text-right">
              <!-- 클릭하면 인라인 편집 가능 -->
              <span
                v-if="!threshold.isEditing"
                @click="startEditing(threshold)"
                class="cursor-pointer text-orange-500 hover:underline"
              >
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
            </td>
            <td class="px-4 py-2 text-sm text-gray-700">{{ threshold.dbType }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="filteredData.length === 0" class="mt-4 text-sm text-gray-500">검색 결과가 없습니다.</p>
  </div>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      thresholds: [], // 원본 데이터
      searchQuery: "", // 검색어 입력값
      sortBy: "", // 정렬 기준
      sortOrder: "asc", // 정렬 방향 (오름차순 또는 내림차순)
    };
  },
  computed: {
    // 검색어에 따라 필터링된 데이터를 반환
    filteredData() {
      const query = this.searchQuery.toLowerCase();
      const filtered = this.thresholds.filter((threshold) => {
        return (
          threshold.dbName.toLowerCase().includes(query) ||
          threshold.tablespaceName.toLowerCase().includes(query)
        );
      });

      // 정렬된 데이터를 반환
      return this.sortDataBy(filtered);
    },
  },
  methods: {
    // 숫자 1000단위로 구분
    formatNumber(number) {
      return number.toLocaleString(); // 천 단위 구분 기호 추가
    },

    // 편집 시작
    startEditing(threshold) {
      threshold.isEditing = true;
      threshold.editedValue = threshold.thresMb; // 원본 값 저장
    },
    // 편집 취소
    cancelEditing(threshold) {
      threshold.isEditing = false;
      threshold.editedValue = null;
    },
    // 임계치 업데이트
    updateThreshold(threshold) {
      const updatedThreshold = {
        id: threshold.id, // ID가 필요
        thresMb: threshold.editedValue,
      };

      // 서버에 PUT 요청
      axios
        .put(`/api/threshold/${updatedThreshold.id}`, updatedThreshold)
        .then((response) => {
          if (response.data) {
            // 로컬 데이터 업데이트 및 편집 종료
            threshold.thresMb = threshold.editedValue;
            threshold.isEditing = false;
          } else {
            console.error("임계치 업데이트 실패");
          }
        })
        .catch((error) => {
          console.error("임계치 업데이트 오류:", error);
        });
    },

    // 테이블 정렬
    sortData(column) {
      if (this.sortBy === column) {
        // 이미 같은 컬럼을 클릭했으면 정렬 방향을 바꿔줌
        this.sortOrder = this.sortOrder === "asc" ? "desc" : "asc";
      } else {
        this.sortBy = column;
        this.sortOrder = "asc"; // 새 컬럼이 선택되면 오름차순으로 기본 설정
      }
    },

    // 데이터 정렬 로직
    sortDataBy(data) {
      return data.sort((a, b) => {
        let valA = a[this.sortBy];
        let valB = b[this.sortBy];

        // 숫자 정렬인 경우
        if (typeof valA === "number" && typeof valB === "number") {
          return this.sortOrder === "asc" ? valA - valB : valB - valA;
        }

        // 문자열 정렬인 경우
        if (typeof valA === "string" && typeof valB === "string") {
          return this.sortOrder === "asc"
            ? valA.localeCompare(valB)
            : valB.localeCompare(valA);
        }

        return 0;
      });
    },
  },
  mounted() {
    // API 호출
    axios
      .get("/api/threshold/all")
      .then((response) => {
        this.thresholds = response.data.map((threshold) => ({
          ...threshold,
          isEditing: false, // 각 데이터에 편집 상태 추가
          editedValue: null, // 편집 상태 초기화
        }));
      })
      .catch((error) => {
        console.error("API 호출 오류:", error);
      });
  },
};
</script>

<style scoped>
/* 공통 스타일 */
.container {
  font-family: 'Arial', sans-serif;
  padding: 20px;
  max-width: 1370px;
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
  text-align: left;
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
