<template>
  <div class="dashboard">

    <!-- 🔵 상단 통계 요약 카드 -->
    <div class="stats-summary">
      <div class="stat-card">
        <h3>운영중 DB 수</h3>
        <p>32</p>
      </div>
      <div class="stat-card">
        <h3>진단 완료</h3>
        <p>28</p>
      </div>
      <div class="stat-card">
        <h3>경고 발생</h3>
        <p>4</p>
      </div>
      <div class="stat-card">
        <h3>장애 발생</h3>
        <p>1</p>
      </div>
    </div>

    <!-- 🟠 공지사항 영역 -->
    <div class="notice-board">
      <h2>📢 시스템 공지사항</h2>
      <ul>
        <li>※ 5월 1일 근로자의 날 점검 예정</li>
        <li>※ 5월 3일 정기 점검으로 서버 10분간 중단 예정</li>
      </ul>
    </div>

    <!-- 🟢 모니터링 박스들 -->
    <div class="monitoring-section">
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

    <!-- 🟣 최근 이벤트 로그 -->
    <div class="recent-events">
      <h2>📝 최근 이벤트 기록</h2>
      <ul>
        <li>04/29 14:00 - DB01 Tablespace 경고 발생</li>
        <li>04/29 13:30 - DB03 Live Check 실패</li>
        <li>04/29 12:50 - DB02 Deadlock 탐지</li>
        <li>04/29 12:00 - DB04 정상</li>
      </ul>
    </div>

  </div>
</template>
<style>
.dashboard {
  font-family: 'Arial', sans-serif;
  padding: 20px;
  max-width: 1250px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

/* 상단 통계 카드 */
.stats-summary {
  display: flex;
  justify-content: space-around;
  margin-bottom: 30px;
}

.stat-card {
  background: #f0f4f8;
  border-radius: 10px;
  padding: 20px;
  text-align: center;
  width: 200px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.stat-card h3 {
  margin-bottom: 10px;
  font-size: 18px;
  color: #555;
}

.stat-card p {
  font-size: 24px;
  font-weight: bold;
  color: #2196f3;
}

/* 공지사항 */
.notice-board {
  background: #fff3e0;
  padding: 20px;
  margin-bottom: 30px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.notice-board h2 {
  font-size: 20px;
  margin-bottom: 10px;
  color: #ff7043;
}

.notice-board ul {
  margin: 0;
  padding-left: 20px;
}

.notice-board li {
  font-size: 16px;
  margin-bottom: 5px;
}

/* 모니터링 섹션 */
.monitoring-section {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 30px;
}

/* 최근 이벤트 */
.recent-events {
  background: #e8f5e9;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.recent-events h2 {
  font-size: 20px;
  color: #66bb6a;
  margin-bottom: 10px;
}

.recent-events ul {
  margin: 0;
  padding-left: 20px;
}

.recent-events li {
  font-size: 16px;
  margin-bottom: 5px;
}
</style>
