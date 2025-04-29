<template>
  <div class="db-list-container">
    <h2>DB 리스트</h2>

    <!-- 상단 영역 -->
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
          <th @click="sortTable('assets')">자산<span v-if="sortKey === 'assets'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('dbDescript')">설명<span v-if="sortKey === 'dbDescript'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('hostname')">호스트<span v-if="sortKey === 'hostname'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('pubIp')">PubIP<span v-if="sortKey === 'pubIp'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('vip')">VIP<span v-if="sortKey === 'vip'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('dbName')">DB 이름<span v-if="sortKey === 'dbName'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('liveChk')">LIVE<span v-if="sortKey === 'liveChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('sizeChk')">TBS<span v-if="sortKey === 'sizeChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('trnBakChk')">TRN<span v-if="sortKey === 'trnBakChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('objSegSizeChk')">OBJ<span v-if="sortKey === 'objSegSizeChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th @click="sortTable('dailyChk')">일일점검<span v-if="sortKey === 'dailyChk'">{{ sortAsc ? '▲' : '▼' }}</span></th>
          <th>자사중지</th> <!-- 하나 행에 자사중지 버튼 -->
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
            <button :class="[db.liveChk === 'Y' ? 'btn-on' : 'btn-off', !isButtonEnabled(db, 'liveChk') ? 'btn-disabled' : '']"
              @click="showModal(db, 'liveChk')"
              :disabled="!isButtonEnabled(db, 'liveChk')">
              {{ db.liveChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="[db.sizeChk === 'Y' ? 'btn-on' : 'btn-off', !isButtonEnabled(db, 'sizeChk') ? 'btn-disabled' : '']"
              @click="showModal(db, 'sizeChk')"
              :disabled="!isButtonEnabled(db, 'sizeChk')">
              {{ db.sizeChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="[db.trnBakChk === 'Y' ? 'btn-on' : 'btn-off', !isButtonEnabled(db, 'trnBakChk') ? 'btn-disabled' : '']"
              @click="showModal(db, 'trnBakChk')"
              :disabled="!isButtonEnabled(db, 'trnBakChk')">
              {{ db.trnBakChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="[db.objSegSizeChk === 'Y' ? 'btn-on' : 'btn-off', !isButtonEnabled(db, 'objSegSizeChk') ? 'btn-disabled' : '']"
              @click="showModal(db, 'objSegSizeChk')"
              :disabled="!isButtonEnabled(db, 'objSegSizeChk')">
              {{ db.objSegSizeChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button :class="[db.dailyChk === 'Y' ? 'btn-on' : 'btn-off', !isButtonEnabled(db, 'dailyChk') ? 'btn-disabled' : '']"
              @click="showModal(db, 'dailyChk')"
              :disabled="!isButtonEnabled(db, 'dailyChk')">
              {{ db.dailyChk === 'Y' ? 'On' : 'Off' }}
            </button>
          </td>
          <td>
            <button
              class="pause-btn"
              :class="getAssetButtonClass(db)"
              @click="showPauseModal(db)"
            >
              {{ getAssetButtonLabel(db) }}
            </button>
          </td>

        </tr>
      </tbody>
    </table>

    <p v-else>데이터가 없습니다.</p>
    <!-- 자산중지 모달 -->
    <div v-if="isPauseModalVisible" class="modal-overlay">
      <div class="modal">
        <h3>정말 자산을 {{ getAssetButtonLabel(targetPauseDb) }} 하시겠습니까?</h3>
        <div class="modal-actions">
          <button class="modal-btn" @click="confirmPauseAssets">확인</button>
          <button class="modal-btn" @click="cancelPauseAssets">취소</button>
        </div>
      </div>
    </div>
    <!-- 개별 수정 모달 -->
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
import api from "@/api";
import { connectWebSocket, disconnectWebSocket } from "@/websocket";

const buttonRules = {
  liveChk: ['EDB', 'TIBERO', 'MSSQL', 'SYBASE', 'MARIADB', 'MYSQL', 'ORACLE'],
  sizeChk: ['ORACLE', 'TIBERO'],
  trnBakChk: ['SYBASE'],
  objSegSizeChk: ['ORACLE', 'TIBERO'],
  dailyChk: ['ORACLE', 'TIBERO'],
};

export default {
  data() {
    return {
      dbList: [],
      searchQuery: "",
      isModalVisible: false,
      currentDb: null,
      currentField: null,
      allChkStatus: null,
      isAllChkModalVisible: false,
      sortKey: "dbDescript", //  기본 정렬 키 설정
      sortAsc: true,     //  기본 오름차순
      isPauseModalVisible: false, //  자산중지 모달 추가
      targetPauseDb: null,         //  자산중지 대상 DB 저장
    };
  },
  computed: {
    filteredDbList() {
      const query = this.searchQuery.toLowerCase();
      let result = this.dbList.filter((db) => {
        return (
          (db.dbDescript && db.dbDescript.toLowerCase().includes(query)) ||
          (db.hostname && db.hostname.toLowerCase().includes(query)) ||
          (db.pubIp && db.pubIp.toLowerCase().includes(query)) ||
          (db.vip && db.vip.toLowerCase().includes(query)) ||
          (db.dbType && db.dbType.toLowerCase().includes(query)) ||
          (db.dbName && db.dbName.toLowerCase().includes(query))
        );
      });

      // 🔥 검색 후 정렬
      if (this.sortKey) {
        result = [...result].sort((a, b) => {
          const valA = a[this.sortKey] ? a[this.sortKey].toString().toLowerCase() : "";
          const valB = b[this.sortKey] ? b[this.sortKey].toString().toLowerCase() : "";
          if (valA < valB) return this.sortAsc ? -1 : 1;
          if (valA > valB) return this.sortAsc ? 1 : -1;
          return 0;
        });
      }

      return result;
    },
  },
  methods: {
    // 🔥 자산중지 모달 열기
      showPauseModal(db) {
        this.targetPauseDb = db;
        this.isPauseModalVisible = true;
      },

      // 🔥 자산중지 확정
      confirmPauseAssets() {
        const username = this.$store.state.user.username;
        const db = this.targetPauseDb;
        if (!db) return;

        const targetFields = ['liveChk', 'sizeChk', 'trnBakChk', 'objSegSizeChk', 'dailyChk'];
        const managedFields = targetFields.filter(field => {
          return buttonRules[field]?.includes(db.dbType);
        });

        const hasAnyY = managedFields.some(field => db[field] === "Y");
        const newStatus = hasAnyY ? "N" : "Y";

        managedFields.forEach(field => {
          if (field in db) {
            db[field] = newStatus;
          }
        });

        api.put(`/api/db-list/update/${db.id}`, {
          ...db,
          username: username,
        })
        .then(() => {
          this.isPauseModalVisible = false;
          this.targetPauseDb = null;
        })
        .catch((error) => {
          console.error("자산중지 업데이트 실패", error);
          this.isPauseModalVisible = false;
          this.targetPauseDb = null;
        });
      },

      // 🔥 자산중지 모달 취소
      cancelPauseAssets() {
        this.isPauseModalVisible = false;
        this.targetPauseDb = null;
      },

      // 버튼 라벨
      getAssetButtonLabel(db) {
        if (!db) return '';
        const targetFields = ['liveChk', 'sizeChk', 'trnBakChk', 'objSegSizeChk', 'dailyChk'];
        const managedFields = targetFields.filter(field => buttonRules[field]?.includes(db.dbType));
        const hasAnyY = managedFields.some(field => db[field] === "Y");
        return hasAnyY ? "자산중지" : "관제활성";
      },

      // 버튼 색상
      getAssetButtonClass(db) {
        const targetFields = ['liveChk', 'sizeChk', 'trnBakChk', 'objSegSizeChk', 'dailyChk'];
        const managedFields = targetFields.filter(field => buttonRules[field]?.includes(db.dbType));
        const hasAnyY = managedFields.some(field => db[field] === "Y");
        return hasAnyY ? 'btn-gray' : 'btn-off';
      },
    isButtonEnabled(db, field) {
      const allowedDbTypes = buttonRules[field];
      if (!allowedDbTypes) return false;
      return allowedDbTypes.includes(db.dbType);
    },
    sortTable(column) {
      if (this.sortKey === column) {
        this.sortAsc = !this.sortAsc;
      } else {
        this.sortKey = column;
        this.sortAsc = true;
      }
    },
    showModal(db, field) {
      this.currentDb = db;
      this.currentField = field;
      this.isModalVisible = true;
    },
    confirmUpdate() {
      if (this.currentDb && this.currentField) {
        const newStatus = this.currentDb[this.currentField] === "Y" ? "N" : "Y";
        this.currentDb[this.currentField] = newStatus;

        const username = this.$store.state.user.username;

        api.put(`/api/db-list/update/${this.currentDb.id}`, {
          ...this.currentDb,
          username: username,
        })
          .then(() => {
            this.isModalVisible = false;
          })
          .catch((error) => {
            console.error(`${this.currentField} 업데이트 실패`, error);
            this.currentDb[this.currentField] = newStatus === "Y" ? "N" : "Y";
            this.isModalVisible = false;
          });
      }
    },
    cancelUpdate() {
      this.isModalVisible = false;
    },
    showAllChkModal() {
      this.isAllChkModalVisible = true;
    },
    confirmAllChkUpdate() {
      const newStatus = this.allChkStatus === "N" ? null : "N";
      api.put("/api/db-list/update-allchk", { status: newStatus })
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
    },
    handleWebSocketMessage(updatedDb) {
      const index = this.dbList.findIndex((db) => db.id === updatedDb.id);
      if (index !== -1) {
        this.dbList.splice(index, 1, updatedDb);
      }
    },
  },
  mounted() {
    api.get("/api/db-list/all")
      .then((response) => {
        this.dbList = response.data;
      })
      .catch((error) => {
        console.error("API 호출 오류:", error);
      });

    api.get("/api/db-list/allchk")
      .then((response) => {
        this.allChkStatus = response.data;
      })
      .catch((error) => {
        console.error("전체관제 상태 조회 실패", error);
      });

    connectWebSocket({
      onDbStatusMessage: this.handleWebSocketMessage,
    });
  },
  beforeUnmount() {
    disconnectWebSocket();
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
  min-width: 50px; /* ✅ 최소 너비 */
  text-align: center;
  overflow: visible; /* ✅ overflow 해제 */
  text-overflow: unset; /* ✅ 말줄임 해제 */
  white-space: nowrap; /* ✅ 줄바꿈 방지 (On/Off만 딱 표시) */
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.8em;
  font-weight: bold;
  transition: background-color 0.3s ease, transform 0.2s ease;
}

button:disabled {
  background-color: #dcdcdc !important;
  color: #7f8c8d;
  cursor: not-allowed;
  opacity: 0.6;
}

button.btn-disabled {
  background-color: #dcdcdc !important;
  color: #7f8c8d !important;
}

.db-table td button {
  width: 100%; /* ✅ 버튼이 td를 가득 채우게 한다 */
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
.db-table td button {
  width: 100%;
  display: block;
  text-align: center;
}

.db-table td {
  overflow: hidden;
  text-overflow: clip;
  white-space: nowrap;
}
.pause-btn {
  padding: 5px 10px;
  font-size: 0.75em;
  min-width: auto;
  width: auto;
  max-width: 90px;
}
.db-table th:nth-child(12),
.db-table td:nth-child(12) {
  width: 5%; /* DB 이름 열 너비 설정 */
}
</style>

