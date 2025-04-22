<template>
  <div class="dashboard">
    <div
      class="monitoring-box"
      v-for="(item, index) in monitoringItems"
      :key="item.title"
      :style="item.style"
    >
      <h2>
        {{ item.title }}
        <div class="controls">
          <input
            type="number"
            v-model.number="item.refreshInterval"
            min="1"
            class="interval-input"
            :disabled="item.isActive"
            placeholder="초 단위"
          />
          <button
            @click="toggleMonitoring(index)"
            :class="{ active: item.isActive }"
            class="monitoring-button"
          >
            <span v-if="item.isActive">■</span>
            <span v-else>▶</span>
          </button>
          <div class="time-remaining">
            남은 시간: {{ item.remainingTime }}초
          </div>
        </div>
      </h2>

      <div class="content">
        <ul>
          <li v-for="event in item.events" :key="event.id">
            {{ event.message }}
          </li>
        </ul>
        <div v-if="item.events.length === 0" class="no-events">현재 이벤트가 없습니다.</div>
      </div>
    </div>
  </div>
</template>

<script>
import api from "@/api"; // 공통 axios 인스턴스 가져오기

export default {
  data() {
    return {
      monitoringItems: [
        {
          title: '테이블스페이스 관제',
          events: [],
          style: { position: 'absolute', top: '20px', left: '300px', zIndex: 1, width: '550px', height: '300px' },
          isActive: false,
          timerId: null,
          refreshInterval: 500,
          remainingTime: 500,
          fetchMethod: this.fetchTablespaceMonitoringData,
        },
        {
          title: '생사 관제',
          events: [],
          style: { position: 'absolute', top: '20px', left: '900px', zIndex: 1, width: '550px', height: '300px' },
          isActive: false,
          timerId: null,
          refreshInterval: 500,
          remainingTime: 500,
          fetchMethod: this.fetchLiveMonitoringData,
        },
        {
          title: '오브젝트 인밸리드 관제',
          events: [{ id: 3, message: 'Invalid Object 발견: FUNCTION_A' }],
          style: { position: 'absolute', top: '380px', left: '300px', zIndex: 1, width: '550px', height: '300px' },
          isActive: false,
          timerId: null,
          refreshInterval: 10,
          remainingTime: 10,
          fetchMethod: this.fetchInvalidObjectsMonitoringData,
        },
        {
          title: '락 리스트 관제',
          events: [{ id: 4, message: 'Deadlock 발생: Session 101' }],
          style: { position: 'absolute', top: '380px', left: '900px', zIndex: 1, width: '550px', height: '300px' },
          isActive: false,
          timerId: null,
          refreshInterval: 10,
          remainingTime: 10,
          fetchMethod: this.fetchLockListMonitoringData,
        },
      ],
    };
  },
  methods: {
    async fetchTablespaceMonitoringData(index) {
      try {
        const response = await api.get(`/api/tbsChkmon/all`);
        const events = response.data.map(item => {
          const formattedUsedSize = new Intl.NumberFormat().format(item.usedSize);
          const formattedFreeSize = new Intl.NumberFormat().format(item.freeSize);

          return {
            id: item.id,
            message: `${item.dbName} DB의 ${formattedUsedSize}MB 사용 중, 여유공간 ${formattedFreeSize}MB`,
          };
        });
        this.monitoringItems[index].events = events;
      } catch (error) {
        console.error(`테이블스페이스 데이터 가져오기 실패 (index: ${index}):`, error);
      }
    },

    async fetchLiveMonitoringData(index) {
      try {
        const response = await api.get(`/api/LiveChkmon/all`);
        const events = response.data.map(item => ({
          id: item.id,
          message: `${item.dbDesc} 서버 상태: 오류 발생 점검 바람`,
        }));
        this.monitoringItems[index].events = events;
      } catch (error) {
        console.error(`생사 데이터 가져오기 실패 (index: ${index}):`, error);
      }
    },

    async fetchInvalidObjectsMonitoringData(index) {
      console.log("Invalid Object 데이터 가져오기 - API 필요");
      this.monitoringItems[index].events = [];
    },

    async fetchLockListMonitoringData(index) {
      console.log("Lock List 데이터 가져오기 - API 필요");
      this.monitoringItems[index].events = [];
    },

    fetchMonitoringData(index) {
      const item = this.monitoringItems[index];
      if (item.fetchMethod) {
        item.fetchMethod(index);
      }
    },

    toggleMonitoring(index) {
      const item = this.monitoringItems[index];

      if (item.isActive) {
        // 모니터링이 활성화 상태에서 버튼 클릭시 타이머를 멈추고 상태를 비활성화로 변경
        clearInterval(item.timerId);
        item.timerId = null;
        item.isActive = false;
        item.remainingTime = item.refreshInterval;  // 남은 시간을 초기화
      } else {
        // 모니터링이 비활성화 상태에서 버튼 클릭시 데이터 요청 후 타이머 시작
        this.fetchMonitoringData(index);  // 초기 데이터 요청
        item.timerId = setInterval(() => {
          if (item.remainingTime <= 0) {
            this.fetchMonitoringData(index);  // 주기적으로 데이터 요청
            item.remainingTime = item.refreshInterval;  // 남은 시간 초기화
          } else {
            item.remainingTime -= 1;  // 타이머 감소
          }
        }, 1000);  // 1초마다 타이머 실행
        item.isActive = true;  // 활성화 상태로 변경
      }
    }
,
  },

};
</script>

<style>
.dashboard {
  position: relative;
  height: 100vh;
  background: #f5f7fa;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  overflow: hidden;
  padding: 20px;
}

.monitoring-box {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin: 20px;
  width: 100%;
  max-width: 600px;
  height: 350px;
  position: relative;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  background-color: #f9f9f9;
}

.monitoring-box h2 {
  color: #6c757d;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 10px;
  border-bottom: 2px solid #64b5f6;
  padding-bottom: 5px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.monitoring-box .content {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
}

.monitoring-box ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.monitoring-box li {
  padding: 10px;
  font-size: 16px;
  color: #555;
  background: #e3f2fd;
  margin-bottom: 5px;
  border-radius: 8px;
}

.monitoring-box .no-events {
  color: #9e9e9e;
  text-align: center;
  font-size: 16px;
  margin-top: 20px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.interval-input {
  width: 45px;
  padding: 6px;
  margin-right: 10px;
  font-size: 14px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  transition: border-color 0.3s;
  background-color: #f0f4f7;
}

.interval-input:disabled {
  background-color: #e3e3e3;
}

.monitoring-button {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 35px;
  height: 35px;
  font-size: 18px;
  background-color: #81c784;
  color: white;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.3s ease;
}

.monitoring-button:hover {
  background-color: #66bb6a;
  transform: scale(1.1);
}

.monitoring-button.active {
  background-color: #ff7043;
}

.monitoring-button:focus {
  outline: none;
}

.monitoring-button span {
  font-weight: bold;
}

.time-remaining {
  font-size: 16px;
  color: #6c757d;
}
</style>