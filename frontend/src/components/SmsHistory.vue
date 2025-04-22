<template>
  <div class="container">
    <h1 class="title">📨 SMS 전송 히스토리</h1>

    <div class="control-box">
      <!-- 조회일수 + 조회 버튼 한 줄 정렬 -->
      <div class="row-group">
        <label for="days">조회할 일수</label>
        <input type="number" id="days" v-model="day" min="1" />
        <button @click="fetchSmsHistories">조회</button>
      </div>

      <div class="form-group">
        <label for="msgSearch">MSG 내용 검색</label>
        <input type="text" id="msgSearch" v-model="msgSearch" @input="filterSmsHistories" />
      </div>

      <div class="button-group">
        <button @click="updateAllSmsHistories" class="orange" title="명절이나 장시간 관제 중지시 사용!!">대량 메시지 전송 처리</button>
      </div>
    </div>

    <table>
      <thead>
        <tr>
          <th>INDATE</th>
          <th>INTIME</th>
          <th>SENDNAME</th>
          <th>RECVNAME</th>
          <th>MSG</th>
          <th>RESULT</th>
          <th>KIND</th>
          <th>ERRCODE</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="history in filteredSmsHistories" :key="history.SEQNO">
          <td>{{ history.inDate }}</td>
          <td>{{ history.inTime }}</td>
          <td>{{ history.sendName }}</td>
          <td>{{ history.recName }}</td>
          <td>{{ history.msg }}</td>
          <td> <span v-if="history.result === 0">미전송</span>
                <span v-else>전송</span></td>
          <td>{{ history.kind }}</td>
          <td>{{ history.errCode }}</td>
        </tr>
      </tbody>

    </table>

    <!-- 모달 팝업 -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <p>✅ {{ updateMessage }}</p>
        <button @click="showModal = false">확인</button>
      </div>
    </div>
  </div>
</template>

<script>
import api from "@/api"; // 공통 axios 인스턴스 가져오기

export default {
  data() {
    return {
      smsHistories: [],
      filteredSmsHistories: [],
      day: 3,  // 기본값
      msgSearch: '',
      showModal: false,
      updateMessage: '',
    };
  },
  mounted() {
    this.fetchSmsHistories();
  },
  methods: {
    fetchSmsHistories() {
      console.log("조회할 일수(day) 값:", this.day);

      if (this.day && this.day > 0) {  // day 값이 1 이상일 때만 API 요청
        api.get(`/api/sms/all?day=${this.day}`)
          .then(response => {
            //console.log("API 응답:", response);
            if (Array.isArray(response.data)) {
              this.smsHistories = response.data;

              // inDate와 inTime을 결합하여 비교할 수 있도록 정렬
              this.smsHistories.sort((a, b) => {
                const dateA = a.inDate + a.inTime;
                const dateB = b.inDate + b.inTime;
                return dateB.localeCompare(dateA); // 최신 순으로 정렬
              });

              this.filteredSmsHistories = [...this.smsHistories]; // 필터링된 목록 갱신
            } else {
              console.error("응답 데이터가 배열이 아닙니다:", response.data);
            }
          })
          .catch(error => {
            console.error('Error fetching SMS histories:', error);
          });
      } else {
        console.log('조회할 일수가 올바르지 않습니다.');
        this.smsHistories = [];
        this.filteredSmsHistories = [];
      }
    },

    // 메시지 내용 필터링 함수
    filterSmsHistories() {
      this.filteredSmsHistories = this.msgSearch
        ? this.smsHistories.filter(history =>
            history.msg && history.msg.includes(this.msgSearch))
        : this.smsHistories;
    },

    // 대량 메시지 전송 처리
    updateAllSmsHistories() {
      api.put('/api/sms/updateall')
        .then(response => {
          const updatedCount = response.data?.updatedCount || 0;
          this.updateMessage = `전체 메시지 전송 처리가 완료되었습니다. (총 ${updatedCount}건)`;
          this.showModal = true;
          this.fetchSmsHistories();  // 완료 후 다시 조회
        })
        .catch(error => {
          console.error('Error updating SMS histories:', error);
        });
    },
  }
};
</script>


<style scoped>
/* 기존 스타일 */

.container {
  font-family: 'Arial', sans-serif;
  padding: 20px;
  max-width: 1250px;
  margin: 0 auto;
  background: #ffffff; /* 흰색 배경 */
  border-radius: 10px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1); /* 그림자 강조 */
}

.title {
  font-size: 26px;
  font-weight: bold;
  margin-bottom: 30px;
}

.control-box {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background-color: #fafafa;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.row-group {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

input[type="number"],
input[type="text"] {
  padding: 8px;
  width: 200px;
  border-radius: 4px;
  border: 1px solid #ccc;
}

.button-group {
  display: flex;
  gap: 10px;
}

button {
  padding: 10px 16px;
  border: none;
  background-color: #4CAF50;
  color: white;
  border-radius: 5px;
  cursor: pointer;
  font-weight: bold;
}

button.orange {
  background-color: #f57c00;
}

button:hover {
  opacity: 0.9;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
  font-size: 14px;
}

th {
  background-color: #f2f2f2;
  padding: 10px;
}

td {
  padding: 10px;
  text-align: center;
  border-bottom: 1px solid #ddd;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  min-width: 300px;
  text-align: center;
}
</style>
