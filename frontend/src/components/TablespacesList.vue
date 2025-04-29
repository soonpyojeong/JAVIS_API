<template>
  <div class="container">
    <h2>{{ selectedDb }} DB - 테이블스페이스 리스트</h2>

    <div class="select-container">
      <select v-model="selectedDb" @change="fetchTablespaces(selectedDb)">
        <option value="DB 선택" disabled>DB 선택</option>
        <option v-for="(db, index) in tbList" :key="index" :value="db">
          {{ db }}
        </option>
      </select>
       <!-- ✅ wrapper div에 hover 이벤트 정확히 추가 -->
       <div
         class="refresh-wrapper"
         @mouseenter="showTooltip = true"
         @mouseleave="showTooltip = false"
       >
         <button
           class="refresh-btn"
           :class="{ rotating: isRotating }"
           @click="handleRefreshClick"
         >
           <svg class="refresh-icon" viewBox="0 0 24 24">
             <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6h-2c0 4.41 3.59 8 8 8s8-3.59 8-8-3.59-8-8-8z" fill="currentColor"/>
           </svg>
         </button>

         <!-- ✅ 툴팁 카드 표시 -->
         <div v-if="showTooltip" class="tooltip-card">
           DB 정보 새로고침
           <div class="tooltip-arrow"></div> <!-- 화살표 -->
         </div>
       </div>
    </div>
    <!-- 메시지 모달 팝업 -->
    <div v-if="showMessageModal" class="modal-overlay">
      <div class="modal">
        <p>{{ messageModalText }}</p>
        <button @click="closeMessageModal" class="modal-close-btn">확인</button>
      </div>
    </div>
    <table class="tablespace-table">
      <thead>
        <tr>
          <th>Tablespace 이름</th>
          <th>TOTAL(MB)</th>
          <th>USED(MB)</th>
          <th>사용률 (%)</th>
          <th>FREE(MB)</th>
          <th>DB TYPE</th>
          <th>임계치</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ts in filteredTablespaces" :key="ts.id.tsName">
          <td>{{ ts.id.tsName }}</td>
          <td>{{ formatNumber(ts.totalSize) }}</td>
          <td>{{ formatNumber(ts.usedSize) }}</td>
          <td>
            <div class="used-rate-container">
              <canvas :id="'chart-' + ts.id.tsName" class="rate-chart" width="200" height="100"></canvas>
            </div>
          </td>
          <td>{{ formatNumber(ts.freeSize) }}</td>
          <td>{{ ts.dbType }}</td>
          <td>
            <template v-if="ts.thresMb != null">
              {{ formatNumber(ts.thresMb) }}
            </template>
            <template v-else>
              <button @click="handleAddThreshold(ts)" class="add-threshold-button">+</button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="filteredTablespaces.length === 0">검색 결과가 없습니다.</p>

    <div v-if="isModalVisible" class="modal-overlay">
      <div class="modal">
        <h3>임계치 추가 설정</h3>
        <form @submit.prevent="saveThreshold">
          <div class="form-group">
            <label>DB 이름:</label>
            <input type="text" v-model="modalData.dbName" readonly />
          </div>
          <div class="form-group">
            <label>Tablespace 이름:</label>
            <input type="text" v-model="modalData.tablespaceName" readonly />
          </div>
          <div class="form-group">
            <label>DB 타입:</label>
            <input type="text" v-model="modalData.dbType" readonly />
          </div>
          <div class="form-group">
            <label>Threshold MB:</label>
            <input type="number" v-model="modalData.thresMb" required />
          </div>
          <div class="form-group">
            <label>체크 플래그:</label>
            <select v-model="modalData.chkFlag">
              <option value="Y">Y</option>
              <option value="N">N</option>
            </select>
          </div>
          <div class="form-group">
            <label>컨먼트:</label>
            <textarea v-model="modalData.commt"></textarea>
          </div>
          <button type="submit">저장</button>
          <button type="button" @click="closeModal">닫기</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import Chart from "chart.js/auto";
import ChartDataLabels from "chartjs-plugin-datalabels";
import api from "@/api";

export default {
  data() {
    return {
      selectedDb: "DB조회중",
      tbList: [],
      tablespaces: [],
      searchQuery: "",
      isModalVisible: false,
      modalData: { chkFlag: 'Y' },
      chartInstances: {},
      isRotating: false, // ✅ 회전 상태 추가
      showMessageModal: false, // ✅ 모달 알림용 데이터
      messageModalText: "",
      showTooltip: false, // ✅ 툴팁 표시 여부
    };
  },
  computed: {
    filteredTablespaces() {
      return this.tablespaces.filter(ts =>
        ts.id.tsName.toLowerCase().includes(this.searchQuery.toLowerCase())
      );
    },
  },
  methods: {
    formatNumber(number) {
      return number.toLocaleString();
    },
    handleRefreshClick() {
          this.isRotating = true;
          this.refreshDbList();

          // 1초 후에 회전 멈추기 (자연스럽게)
          setTimeout(() => {
            this.isRotating = false;
          }, 1000);
    },
    refreshDbList() {
      api.post("/api/tb/dbList/refresh").then(() => {
        this.openMessageModal("DB 목록이 새로고침되었습니다!");
        this.fetchDbList();
      }).catch(() => {
        this.openMessageModal("DB 목록 새로고침 실패!");
      });
    },
    fetchDbList() {
      api.get("/api/tb/list").then((res) => {
        this.tbList = res.data.sort((a, b) => a.localeCompare(b));
        this.selectedDb = "DB 선택";
      });
    },
    fetchTablespaces(dbName) {
      api.get(`/api/tb/${dbName}/tablespaces`).then((res) => {
        this.tablespaces = res.data || [];
        this.$nextTick(() => {
          this.tablespaces.forEach(ts => this.drawBarChart(ts));
        });
      });
    },
    openMessageModal(message) {
      this.messageModalText = message;
      this.showMessageModal = true;
    },

    closeMessageModal() {
      this.showMessageModal = false;
    },
    drawBarChart(ts) {
      const canvasId = `chart-${ts.id.tsName.replace(/\s+/g, '_')}`;
      const canvas = document.getElementById(canvasId);
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (this.chartInstances[ts.id.tsName]) {
        this.chartInstances[ts.id.tsName].destroy();
      }
      const roundedUsedRate = parseFloat(ts.usedRate.toFixed(1));
      this.chartInstances[ts.id.tsName] = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: ['사용률'],
          datasets: [{
            label: '사용률',
            data: [roundedUsedRate],
            backgroundColor: 'rgba(75, 192, 192, 0.2)',
            borderColor: 'rgb(75, 192, 192)',
            borderRadius: 1,
            borderWidth: 1,
          }]
        },
        options: {
          responsive: true,
          indexAxis: 'y',
          scales: { x: { min: 0, max: 100, ticks: { display: false }, grid: { display: false } }, y: { display: false } },
          plugins: {
            legend: { display: false },
            datalabels: {
              display: true,
              align: (ctx) => ctx.dataset.data[0] >= 56 ? 'center' : 'end',
              anchor: (ctx) => ctx.dataset.data[0] >= 56 ? 'center' : 'end',
              formatter: (value) => `${value.toFixed(1)}%`,
              color: (ctx) => ctx.dataset.data[0] >= 85 ? 'red' : 'rgb(75, 192, 192)',
              font: { weight: 'bold', size: 12 },
            },
          },
        },
        plugins: [ChartDataLabels],
      });
    },
    handleAddThreshold(ts) {
      this.modalData = { dbType: ts.dbType, dbName: ts.id.dbName, tablespaceName: ts.id.tsName, thresMb: ts.freeSize, chkFlag: 'Y', commt: '' };
      this.isModalVisible = true;
    },
    closeModal() {
      this.isModalVisible = false;
    },
    saveThreshold() {
      const username = this.$store.state.user.username;
      api.post("/api/threshold/save", { ...this.modalData, username })
        .then(() => {
          alert("Threshold 설정이 저장되어왔습니다.");
          this.closeModal();
          this.fetchTablespaces(this.selectedDb);
        });
    }
  },
  mounted() {
    this.fetchDbList();
  }
};
</script>


<style scoped>
/* 기본 설정 */
.container {
  font-family: 'Arial', sans-serif;
  padding: 30px;
  max-width: 1000px;
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
  border-radius: 8px;
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
  font-size: 14px;
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
  display: flex;
  justify-content: left;
  align-items: left;
  width: 100%;
  height: 40px;
  min-width: 100px; /* 최소 크기 설정 */
}

.rate-chart {
  max-width: 120px;
  max-height: 90px;
  width: 100%;
  height: auto;
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
    font-size: 12px;
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
