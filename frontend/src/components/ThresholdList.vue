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
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('dbName')" > DB 이름 </th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('tablespaceName')">Tablespace</th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('thresMb')"> 임계치 (MB) </th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
              @click="sortData('dbType')" > DB 타입 </th>
            <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600 cursor-pointer hover:bg-gray-100"
             @click="sortData('imsiDel')" > 관제임시해제(3일) </th>
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
            <td class="px-4 py-2 text-sm text-gray-700 text-center">
              <template v-if="threshold.imsiDel">
                <div class="text-center">
                  {{ new Date(threshold.imsiDel).toLocaleDateString() }}
                </div>
              </template>
              <template v-else>
                <div class="flex justify-center">
                  <button class="text-blue-500 hover:underline">
                    해제
                  </button>
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
<script>
import api from "@/api"; // 공통 axios 인스턴스 가져오기

export default {
  data() {
    return {
      thresholds: [],
      searchQuery: "",
      sortBy: "",
      sortOrder: "asc",
    };
  },
  computed: {
    filteredData() {
      const query = this.searchQuery.toLowerCase();
      const filtered = this.thresholds.filter((t) => {
        return (
          t.dbName.toLowerCase().includes(query) ||
          t.tablespaceName.toLowerCase().includes(query)
        );
      });
      return this.sortDataBy(filtered);
    },
  },
  methods: {
    formatNumber(num) {
      return num.toLocaleString();
    },

    startEditing(threshold) {
      threshold.isEditing = true;
      threshold.editedValue = threshold.thresMb;
    },

    cancelEditing(threshold) {
      threshold.isEditing = false;
      threshold.editedValue = null;
    },

    updateThreshold(threshold) {
      const username = this.$store.state.user.username;
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
            console.error("임계치 업데이트 실패");
          }
        })
        .catch((err) => {
          console.error("임계치 업데이트 오류:", err);
        });
    },

    releaseThreshold(id) {
      api.put(`/api/threshold/${id}/release`)
        .then((res) => {
          if (res.data) {
            alert("임시해제가 완료되었습니다.");
            this.refreshThresholds();
          }
        })
        .catch((err) => {
          console.error("임시해제 실패:", err);
        });
    },

    refreshThresholds() {
      api.get("/api/threshold/all")
        .then((res) => {
          this.thresholds = res.data.map((t) => ({
            ...t,
            isEditing: false,
            editedValue: null,
          }));
        })
        .catch((err) => {
          console.error("데이터 로딩 실패:", err);
        });
    },

    sortData(column) {
      if (this.sortBy === column) {
        this.sortOrder = this.sortOrder === "asc" ? "desc" : "asc";
      } else {
        this.sortBy = column;
        this.sortOrder = "asc";
      }
    },

    sortDataBy(data) {
      return data.sort((a, b) => {
        const valA = a[this.sortBy];
        const valB = b[this.sortBy];

        // 날짜 타입 (imsiDel 혼합) 정렬
        if (this.sortBy === "imsiDel") {
          const timeA = valA ? new Date(valA).getTime() : 0; // null일 경우 0 또는 Infinity로 바꿀 수 있음
          const timeB = valB ? new Date(valB).getTime() : 0;

          return this.sortOrder === "asc" ? timeA - timeB : timeB - timeA;
        }

        // 숫자 정렬
        if (typeof valA === "number" && typeof valB === "number") {
          return this.sortOrder === "asc" ? valA - valB : valB - valA;
        }

        // 문자열 정렬
        if (typeof valA === "string" && typeof valB === "string") {
          return this.sortOrder === "asc"
            ? valA.localeCompare(valB)
            : valB.localeCompare(valA);
        }

        return 0;
      });
    }

,
  },
  mounted() {
    this.refreshThresholds();
  },
};
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
