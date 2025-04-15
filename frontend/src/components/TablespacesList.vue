<template>
  <div class="container">
    <h2>{{ selectedDb }} DB - 테이블스페이스 리스트</h2>

    <!-- DB 목록 -->
    <div class="select-container">
      <select v-model="selectedDb" @change="fetchTablespaces(selectedDb)">
        <option value="DB 선택" disabled>DB 선택</option>
        <option v-for="(db, index) in tbList" :key="index" :value="db">
          {{ db }}
        </option>
      </select>
    </div>

    <!-- 테이블 -->
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
        <tr
          v-for="ts in filteredTablespaces"
          :key="ts.id.tsName"
        >
          <td class="ts-name">{{ ts.id.tsName }}</td>
          <td class="used-size">{{ formatNumber(ts.totalSize) }}</td>
          <td class="used-size">{{ formatNumber(ts.usedSize) }}</td>
          <td class="used-rate">
            <div class="used-rate-container">
              <canvas :id="'chart-' + ts.id.tsName" class="rate-chart" width="200" height="100"></canvas>
            </div>
          </td>
          <td class="free-size">{{ formatNumber(ts.freeSize) }}</td>
          <td class="ts-name">{{ formatNumber(ts.dbType) }}</td>
          <td class="free-size">
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
 <!-- 모달 팝업 -->
    <div v-if="isModalVisible" class="modal-overlay">
      <div class="modal">
        <h3>임계치 추가 설정</h3>
        <form @submit.prevent="saveThreshold">
          <div class="form-group">
            <label for="dbName">DB 이름:</label>
            <input type="text" id="dbName" v-model="modalData.dbName" readonly />
          </div>
          <div class="form-group">
            <label for="tsName">Tablespace 이름:</label>
            <input type="text" id="tsName" v-model="modalData.tablespaceName" readonly />
          </div>
          <div class="form-group">
            <label for="dbType">DB 타입:</label>
            <input type="text" id="dbType" v-model="modalData.dbType" readonly />
          </div>
          <div class="form-group">
            <label for="thresMb">Threshold MB:</label>
            <input type="number" id="thresMb" v-model="modalData.thresMb" required />
          </div>
          <div class="form-group">
            <label for="chkFlag">체크 플래그:</label>
            <select id="chkFlag" v-model="modalData.chkFlag">
              <option value="Y">Y</option>
              <option value="N">N</option>
            </select>
          </div>
          <div class="form-group">
            <label for="commt">코멘트:</label>
            <textarea id="commt" v-model="modalData.commt"></textarea>
          </div>
          <button type="submit">저장</button>
          <button type="button" @click="closeModal">닫기</button>
        </form>
      </div>
    </div>
    </div>
</template>


<script>
import Chart from "chart.js/auto"; // Chart.js 자동 로드
import ChartDataLabels from 'chartjs-plugin-datalabels'; // 플러그인 import

import api from "@/api"; // 공통 axios 인스턴스 가져오기

export default {
  data() {
    return {
      selectedDb: "DB조회중",
      tbList: [],
      tablespaces: [],
      searchQuery: "",
      isModalVisible: false, // 팝업 초기 상태
      modalData: {chkFlag: 'Y',}, // 모달에 필요한 데이터 초기화
    };
  },
  computed: {
    filteredTablespaces() {
      return this.tablespaces.filter((ts) =>
        ts.id.tsName.toLowerCase().includes(this.searchQuery.toLowerCase())
      );
    },
  },
  methods: {
      handleAddThreshold(ts) {
            console.log("handleAddThreshold 호출 시 this:", ts.dbType); // this 확인
            this.modalData = {
              dbType: ts.dbType,
              dbName: ts.id.dbName,
              tablespaceName: ts.id.tsName,
              thresMb: ts.freeSize,
              chkFlag: "Y",
              commt: "",
            };
            this.isModalVisible = true;
      },
      closeModal() {
      this.isModalVisible = false;
      },
      saveThreshold() {
            console.log("saveThreshold 호출 시 this.modalData:", this.modalData); // this 확인
            api.post("/api/threshold/save", this.modalData)
              .then(() => {
                alert("Threshold 설정이 저장되었습니다.");
                this.closeModal();
                this.fetchTablespaces(this.selectedDb); // 데이터 새로 고침
              })
              .catch((error) => {
                console.error("Error saving threshold:", error);
                alert("Threshold 설정 저장 실패!");
              });
      },
    formatNumber(number) {
      return number.toLocaleString(); // 천 단위 구분 기호 추가
    },
    fetchDbList() {
      api.get("/api/tb/list")
        .then((res) => {
          this.tbList = res.data.sort((a, b) => a.localeCompare(b)); // DB 목록 알파벳 순 정렬
          this.selectedDb = "DB 선택"; // 기본값 설정
        })
        .catch((error) => {
          console.error("Error fetching DB list:", error);
        });
    },
    fetchTablespaces(dbName) {
      api.get(`/api/tb/${dbName}/tablespaces`)
        .then((res) => {
          console.log("전체 응답 데이터:", res.data); // 전체 응답 데이터 출력
          if (JSON.stringify(this.tablespaces) === JSON.stringify(res.data)) return;
          this.tablespaces = res.data || [];
          this.$nextTick(() => {
            this.tablespaces.forEach((ts) => {
              this.drawBarChart(ts);
            });
          });
        })
        .catch((error) => {
          console.error("테이블스페이스 데이터 가져오기 실패:", error);
        });
    },

    drawBarChart(ts) {
        if (!ts || !ts.id || !ts.id.tsName) {
            console.error("Invalid ts object:", ts);
            return;
        }

        const canvasId = `chart-${ts.id.tsName.replace(/\s+/g, '_')}`;

        // Vue가 DOM 업데이트 후 실행
        this.$nextTick(() => {
            const canvas = document.getElementById(canvasId);

            // 🚨 canvas가 존재하지 않으면 에러 방지
            if (!canvas) {
                console.error(`Canvas element not found: ${canvasId}`);
                return;
            }

            const ctx = canvas.getContext('2d');

            // 기존 차트가 있으면 삭제
            if (this.chartInstances && this.chartInstances[ts.id.tsName]) {
                this.chartInstances[ts.id.tsName].destroy();
            }

            // chartInstances 객체 초기화
            if (!this.chartInstances) {
                this.chartInstances = {};
            }

            // 🔢 사용률 반올림 (소수점 1자리)
            const roundedUsedRate = parseFloat(ts.usedRate.toFixed(1));

            // 새 차트 생성
            this.chartInstances[ts.id.tsName] = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: ['사용률'],
                    datasets: [{
                        label: '사용률',
                        data: [roundedUsedRate], // 반올림된 값 적용
                        backgroundColor: 'rgba(75, 192, 192, 0.2)',
                        borderColor: 'rgb(75, 192, 192)',
                        borderRadius: 1,
                        borderWidth: 1,
                    }],
                },
                options: {
                    responsive: true,
                    indexAxis: 'y',
                    scales: {
                        x: {
                            min: 0,
                            max: 100,
                            ticks: { display: false }, // X축 눈금 숨김
                            grid: { display: false }, // X축 그리드 선 숨김
                            border: { display: false }, // X축 경계선 숨김
                        },
                        y: {
                            beginAtZero: true,
                            display: false, // Y축 숨김
                            grid: { display: true }, // Y축 그리드 선 숨김
                        },
                    },
                    plugins: {
                        legend: { display: false }, // 범례 숨김
                        datalabels: {
                            display: true,
                            align: (ctx) => ctx.dataset.data[0] >= 56 ? 'center' : 'end',
                            anchor: (ctx) => ctx.dataset.data[0] >= 56 ? 'center' : 'end',
                            formatter: (value) => `${value.toFixed(1)}%`, // 🔢 소수점 1자리 적용
                            color: (ctx) => ctx.dataset.data[0] >= 85 ? 'red' : 'rgb(75, 192, 192)',
                            font: { weight: 'bold', size: 12 },
                        },
                    },
                },
                plugins: [ChartDataLabels],
            });
        });
    }


  }
,

  mounted() {
    this.fetchDbList();
  },
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
</style>
