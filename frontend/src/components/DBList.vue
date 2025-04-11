<template>
  <div class="db-list-container">
    <h2>DB 리스트</h2>

    <!-- 상단 영역: 검색 박스와 전체관제 해제 버튼 같은 줄에 배치 -->
    <div class="top-container">
      <div class="search-box">
        <label for="search" class="search-label">검색:</label>
        <input
          type="text"
          id="search"
          v-model="searchQuery"
          placeholder="설명, 호스트, PubIP, VIP, DB 타입, DB 이름"
        />
      </div>
      <div class="allchk-container">
        <button
          :class="allChkStatus === 'N' ? 'btn-off' : 'btn-gray'"
          @click="showAllChkModal">
          {{ allChkStatus === 'N' ? '전체관제중지중' : '전체관제 해제' }}
        </button>
      </div>
    </div>

    <!-- DB 리스트 테이블 -->
    <table v-if="filteredDbList.length > 0" class="db-table">
      <thead>
        <tr>
          <th @click="sortTable('assets')">자산 <span v-if="sortKey === 'assets'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('dbDescript')">설명 <span v-if="sortKey === 'dbDescript'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('hostname')">호스트 <span v-if="sortKey === 'hostname'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('pubIp')">PubIP <span v-if="sortKey === 'pubIp'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('vip')">VIP <span v-if="sortKey === 'vip'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('dbName')">DB 이름 <span v-if="sortKey === 'dbName'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('liveChk')">LIVE <span v-if="sortKey === 'liveChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('sizeChk')">TBS <span v-if="sortKey === 'sizeChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('trnBakChk')">TRN <span v-if="sortKey === 'trnBakChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('objSegSizeChk')">OBJ <span v-if="sortKey === 'objSegSizeChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('dailyChk')">Daily <span v-if="sortKey === 'dailyChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="db in filteredDbList" :key="db.id">
          <td>{{ db.assets }}</td>
          <td>{{ db.dbDescript }}</td>
          <td>{{ db.hostname }}</td>
          <td>{{ db.pubIp }}</td>
          <td>{{ db.vip }}</td>
          <td>{{ db.dbName }}</td>
          <td>
            <button :class="db.liveChk === 'Y' ? 'btn-on' : 'btn-off'" @click="showModal(db, 'liveChk')">
              {{ db.liveChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="db.sizeChk === 'Y' ? 'btn-on' : 'btn-off'" @click="showModal(db, 'sizeChk')">
              {{ db.sizeChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="db.trnBakChk === 'Y' ? 'btn-on' : 'btn-off'" @click="showModal(db, 'trnBakChk')">
              {{ db.trnBakChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="db.objSegSizeChk === 'Y' ? 'btn-on' : 'btn-off'" @click="showModal(db, 'objSegSizeChk')">
              {{ db.objSegSizeChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="db.dailyChk === 'Y' ? 'btn-on' : 'btn-off'" @click="showModal(db, 'dailyChk')">
              {{ db.dailyChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else>데이터가 없습니다.</p>

    <!-- 개별 수정 모달 (기존) -->
    <div v-if="isModalVisible" class="modal-overlay">
      <div class="modal">
        <h3>정말 수정하시겠습니까?</h3>
        <div class="modal-actions">
          <button class="modal-btn" @click="confirmUpdate">확인</button>
          <button class="modal-btn" @click="cancelUpdate">취소</button>
        </div>
      </div>
    </div>

    <!-- 전체관제 상태 변경 모달 -->
    <div v-if="isAllChkModalVisible" class="modal-overlay">
      <div class="modal">
        <h3>정말 변경 하시겠습니까?</h3>
        <div class="modal-actions">
          <button class="modal-btn" @click="confirmAllChkUpdate">확인</button>
          <button class="modal-btn" @click="cancelAllChkUpdate">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      dbList: [],
      searchQuery: "",
      isModalVisible: false,
      currentDb: null,
      currentField: null,
      allChkStatus: null, // 전체관제 상태 (backend에서 "N"이면 전체관제중지중)
      isAllChkModalVisible: false, // 전체관제 상태 변경 모달 표시 여부
      sortKey: "", // 현재 정렬 기준 열
      sortAsc: true, // 정렬 방향 (true: 오름차순, false: 내림차순)
    };
  },
  computed: {
    filteredDbList() {
      const query = this.searchQuery.toLowerCase();
      return this.dbList.filter(db => {
        return (
          (db.dbDescript && db.dbDescript.toLowerCase().includes(query)) ||
          (db.hostname && db.hostname.toLowerCase().includes(query)) ||
          (db.pubIp && db.pubIp.toLowerCase().includes(query)) ||
          (db.vip && db.vip.toLowerCase().includes(query)) ||
          (db.dbType && db.dbType.toLowerCase().includes(query)) ||
          (db.dbName && db.dbName.toLowerCase().includes(query))
        );
      });
    }
  },
  methods: {
    sortTable(column) {
      if (this.sortKey === column) {
        this.sortAsc = !this.sortAsc;
      } else {
        this.sortKey = column;
        this.sortAsc = true;
      }

      // 새로운 배열로 할당
      this.dbList = [...this.dbList].sort((a, b) => {
        const valA = a[column] ? a[column].toString().toLowerCase() : "";
        const valB = b[column] ? b[column].toString().toLowerCase() : "";
        if (valA < valB) return this.sortAsc ? -1 : 1;
        if (valA > valB) return this.sortAsc ? 1 : -1;
        return 0;
      });
    },
    // 개별 수정 모달 관련
    showModal(db, field) {
      this.currentDb = db;
      this.currentField = field;
      this.isModalVisible = true;
    },
    confirmUpdate() {
      if (this.currentDb && this.currentField) {
        const newStatus = this.currentDb[this.currentField] === 'Y' ? 'N' : 'Y';
        this.currentDb[this.currentField] = newStatus;
        axios
          .put(`/api/db-list/update/${this.currentDb.id}`, { [this.currentField]: newStatus })
          .then(() => {
            this.isModalVisible = false;
          })
          .catch((error) => {
            console.error(`${this.currentField} 업데이트 실패`, error);
            this.currentDb[this.currentField] = this.currentDb[this.currentField] === 'Y' ? 'N' : 'Y';
            this.isModalVisible = false;
          });
      }
    },
    cancelUpdate() {
      this.isModalVisible = false;
    },
    // 전체관제 상태 변경 모달 관련
    showAllChkModal() {
      this.isAllChkModalVisible = true;
    },
    confirmAllChkUpdate() {
      // 현재 상태가 "N"이면 해제(null), 아니면 "N"으로 업데이트
      const newStatus = this.allChkStatus === 'N' ? null : 'N';
      axios
        .put("/api/db-list/update-allchk", { status: newStatus })
        .then(() => {
          this.allChkStatus = newStatus;
          this.isAllChkModalVisible = false;
        })
        .catch((error) => {
          console.error("전체관제 상태 변경 실패", error);
          this.isAllChkModalVisible = false;
        });
    },
    cancelAllChkUpdate() {
      this.isAllChkModalVisible = false;
    }
  },
  mounted() {
    axios
      .get("/api/db-list/all")
      .then((response) => {
        this.dbList = response.data;
      })
      .catch((error) => {
        console.error("API 호출 오류:", error);
      });
    // 전체관제 상태 초기값을 가져오기 (GET /allchk)
    axios
      .get("/api/db-list/allchk")
      .then((response) => {
        this.allChkStatus = response.data;
      })
      .catch((error) => {
        console.error("전체관제 상태 조회 실패", error);
      });
  },
};
</script>
<style scoped>
.db-list-container {
  font-family: 'Arial', sans-serif;
  padding: 20px;
  max-width: 1250px;
  margin: 0 auto;
  background: #ffffff; /* 흰색 배경 */
  border-radius: 10px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1); /* 그림자 강조 */
}

h2 {
  text-align: center;
  font-size: 2.4em;
  margin-bottom: 20px;
  color: #2d3e50;
  font-weight: 600;
}

/* 상단 영역: 같은 줄에 배치 */
.top-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 5px;
  padding: 8px 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-label {
  margin-right: 10px;
  font-size: 1.1em;
  color: #34495e;
}

.search-box input {
  padding: 8px;
  font-size: 1.1em;
  border: 1px solid #ccd6e0;
  border-radius: 4px;
  width: 300px;
  transition: border-color 0.3s;
}

.search-box input:focus {
  border-color: #1abc9c;
  outline: none;
}

.db-table {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
  background: #ffffff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  table-layout: fixed;
}

.db-table th,
.db-table td {
  padding: 12px 15px;
  text-align: center;
  border: 1px solid #ddd;
  word-wrap: break-word;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.9em;
}

.db-table th {
  background-color: #1abc9c;
  color: white;
  font-weight: bold;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.db-table tr:nth-child(even) {
  background-color: #f7f8fa;
}

.db-table tr:hover {
  background-color: #eafaf1;
}

button {
  padding: 8px 15px;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.8em;
  font-weight: bold;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

button:active {
  transform: scale(0.95);
}

.btn-on {
  background-color: #1abc9c;
  color: white;
}

.btn-on:hover {
  background-color: #16a085;
}

.btn-off {
  background-color: #e74c3c;
  color: white;
}

.btn-off:hover {
  background-color: #c0392b;
}

.btn-gray {
  background-color: #95a5a6;
  color: white;
}

.btn-gray:hover {
  background-color: #7f8c8d;
}

p {
  text-align: center;
  font-size: 0.9em;
  color: #7f8c8d;
}

/* 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background-color: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  text-align: center;
}

.modal h3 {
  font-size: 1.8em;
  margin-bottom: 20px;
  color: #333;
}

.modal-actions {
  margin-top: 20px;
}

.modal-btn {
  padding: 10px 20px;
  font-size: 1.1em;
  margin: 0 10px;
  border-radius: 4px;
  cursor: pointer;
  background-color: #1abc9c;
  color: white;
  border: none;
  transition: background-color 0.3s;
}

.modal-btn:hover {
  background-color: #16a085;
}

.modal-btn:first-child {
  background-color: #e74c3c;
}

.modal-btn:first-child:hover {
  background-color: #c0392b;
}

/* 반응형 스타일 */
@media (max-width: 768px) {
  .db-list-container {
    padding: 10px;
  }

  .search-box input {
    width: 100%;
  }

  .top-container {
    flex-direction: column;
    align-items: flex-start;
  }

  button {
    font-size: 0.8em;
  }

  .modal {
    width: 90%;
  }
}

.db-table th:nth-child(1),
.db-table td:nth-child(1) {
  width: 7%; /* 자산 열 너비 설정 */
}
.db-table th:nth-child(2),
.db-table td:nth-child(2) {
  width: 13%; /* 설명 열 너비 설정 */
}

.db-table th:nth-child(3),
.db-table td:nth-child(3) {
  width: 7%; /* 호스트 열 너비 설정 */
}
.db-table th:nth-child(4),
.db-table td:nth-child(4) {
  width: 7%; /* pubip 열 너비 설정 */
}

.db-table th:nth-child(5),
.db-table td:nth-child(5) {
  width: 7%; /* pubip 열 너비 설정 */
}
.db-table th:nth-child(6),
.db-table td:nth-child(6) {
  width: 5%; /* DB 이름 열 너비 설정 */
}

.db-table th:nth-child(7),
.db-table td:nth-child(7) {
  width: 3%; /* DB 이름 열 너비 설정 */
}
.db-table th:nth-child(8),
.db-table td:nth-child(8) {
  width: 3%; /* DB 이름 열 너비 설정 */
}
.db-table th:nth-child(9),
.db-table td:nth-child(9) {
  width: 3%; /* DB 이름 열 너비 설정 */
}
.db-table th:nth-child(10),
.db-table td:nth-child(10) {
  width: 3%; /* DB 이름 열 너비 설정 */
}
.db-table th:nth-child(11),
.db-table td:nth-child(11) {
  width: 3%; /* DB 이름 열 너비 설정 */
}
</style>

