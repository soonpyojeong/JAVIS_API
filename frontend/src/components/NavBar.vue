<template>
  <div class="top-nav">
    <ul v-if="isLoggedIn">
      <li
        v-for="item in filteredMenuItems"
        :key="item.path"
        :class="{ active: selectedMenu === item.path }"
        @click="navigateTo(item.path)"
      >
        {{ item.name }}
      </li>
      <li v-if="user && user.username" class="user-info-wrapper">
        <div class="user-info-badge" @click="toggleProfile">
          <span class="emoji">{{ roleEmoji }}</span>
          <span class="username">{{ user.username }}님</span>
          <span class="role">({{ user.userRole }})</span>
        </div>
        <div v-if="showProfile" class="profile-card">
          <div class="card-inner">
            <div class="card-header">
              <span class="emoji-big">{{ roleEmoji }}</span>
              <div>
                <div class="name">{{ user.username }}</div>
                <div class="email">{{ user.email || "이메일 없음" }}</div>
                <div class="role">🏷 {{ user.userRole }}</div>
              </div>
            </div>
            <hr />
            <button class="logout-card-btn" @click="logout">🚪 로그아웃</button>
          </div>
        </div>
        <div class="notification-wrapper">
        <div class="bell-icon" :class="{ active: hasUnread }" @click="toggleModal">
        🔔
          <span v-if="hasUnread" class="badge">{{ unreadCount }}</span>
         </div>
         <div v-if="showModal" class="alert-modal-below-nav">
         <div class="alert-modal">
             <h3 class="modal-title">📢 알림 내역</h3>
             <ul class="alert-list">
              <li v-for="alert in sortedAlerts" :key="alert.id" class="alert-item">
                <div>
                 <span class="timestamp">{{ formatDate(alert.createdAt) }}</span><br>
                  <span class="message">{{ alert.message }}</span>
                </div>
                 <button class="delete-btn" @click="dismissAlert(alert)">삭제</button>
                </li>
              </ul>
          </div>
         </div>
        </div>
    </li>
    </ul>

    <div v-else style="color: white;">😆 로그인 상태가 아닌다</div>
  </div>
</template>
<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from "vue";
import { useStore } from "vuex";
import { useRouter, useRoute } from "vue-router";
import api from "@/api";
import { connectWebSocket, disconnectWebSocket } from "@/websocket";

const store = useStore();
const router = useRouter();
const route = useRoute();

const isLoggedIn = computed(() => store.state.isLoggedIn);
const user = computed(() => store.state.user);
const showProfile = ref(false);
const showModal = ref(false);
const alerts = ref([]);
const userId = computed(() => user.value?.loginId);
const toggleProfile = () => (showProfile.value = !showProfile.value);
const toggleModal = () => {
  if (alerts.value.length > 0) {
    showModal.value = !showModal.value;
  }
};
const selectedMenu = ref(route.path);
const hasUnread = computed(() => alerts.value.length > 0);
const unreadCount = computed(() => alerts.value.length);

const sortedAlerts = computed(() => {
  return [...alerts.value].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
});
function fetchAlerts() {
  const uid = userId.value;
  const token = localStorage.getItem("accessToken"); // 또는 Vuex에서 가져오기

  api.get(`/api/alerts/${uid}/alerts`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
    .then((res) => {
      alerts.value = res.data.map(a => ({
        id: a.alert.id,
        message: a.alert.message,
        createdAt: a.alert.createdAt
      }));
    })
    .catch((err) => console.error("알림 조회 실패:", err));
}


function dismissAlert(alert) {
  const uid = userId.value;
  const alertId = alert.id;

  //console.log("삭제 요청:", alertId, uid);
  api.put(`/api/alerts/${alertId}/hide?userId=${uid}`)
    .then(() => {
      alerts.value = alerts.value.filter(a => a.id !== alertId);
    })
    .catch(err => console.error("알림 삭제 실패:", err));
}

function handleAlertMessage(payload) {
  if (payload.message) {
    //console.log("📩 웹소켓 수신:", payload.message);
    fetchAlerts();
  }
}

function formatDate(dateStr) {
  const date = new Date(dateStr);
  return date.toLocaleString('ko-KR', { hour12: false });
}

const handleClickOutside = (event) => {
  const card = document.querySelector(".profile-card");
  const badge = document.querySelector(".user-info-badge");
  const modal = document.querySelector(".notification-wrapper .alert-modal-below-nav");
  const bell = document.querySelector(".bell-icon");

  const isOutsideProfile = card && !card.contains(event.target) && badge && !badge.contains(event.target);
  const isOutsideBell = modal && !modal.contains(event.target) && bell && !bell.contains(event.target);

  if (isOutsideProfile) {
    showProfile.value = false;
  }
  if (isOutsideBell) {
    showModal.value = false;
  }
};

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
  fetchAlerts();
  connectWebSocket({ onAlertMessage: handleAlertMessage });
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside);
  disconnectWebSocket();
});

watch(user, (newVal) => {
  if (!newVal || !newVal.username) {
    showProfile.value = false;
  }
});

const menuItems = [
  { name: "첫화면", path: "/", roles: ["DBA", "DEV", "VIEW"] },
  { name: "DB 전체 리스트", path: "/db-list", roles: ["DBA"] },
  { name: "SMS 전송 내역", path: "/sms-history", roles: ["DBA", "DEV", "VIEW"] },
  { name: "임계치 리스트", path: "/threshold-list", roles: ["DBA"] },
  { name: "테이블스페이스 리스트", path: "/tablespaces", roles: ["DBA"] },
  { name: "일일 점검(hit율)", path: "/dailyChk", roles: ["DBA"] },
  { name: "일일 점검(SYS)", path: "/SysInfoDetail", roles: ["DBA"] },
  { name: "패스워드관리", path: "/Manager", roles: ["DBA"]},
];

const filteredMenuItems = computed(() => {
  const role = user.value?.userRole?.toUpperCase();
  return menuItems.filter(item => item.roles.includes(role));
});

const navigateTo = (path) => {
  selectedMenu.value = path; // ✅ 클릭한 메뉴로 선택 경로 업데이트
  router.push(path);
};

const logout = () => {
  store.dispatch("logout");
  showProfile.value = false;
  window.location.href = "/";
  router.push("/login");
};

const roleEmoji = computed(() => {
  if (!user.value || !user.value.userRole) return "🙂";
  switch (user.value.userRole.toUpperCase()) {
    case "DBA": return "👑";
    case "DEV": return "‍💻";
    case "VIEW": return "🐥";
    default: return "😊";
  }
});
</script>


<style scoped>
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background-color: #4caf50;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Segoe UI', 'Pretendard', sans-serif;
  white-space: nowrap;
}

.top-nav ul {
  list-style: none;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
  gap: 8px;
}

.top-nav li {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  margin: 0;
  color: white;
  font-weight: 600;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.25s ease;
  border-radius: 20px;
  position: relative;
}

.top-nav li:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-1px);
}

.top-nav li.active {
  background-color: #2e7d32;
  color: #fff;
}

.logout-btn {
  color: #ffdddd;
  font-weight: bold;
}

.logout-btn:hover {
  background-color: rgba(255, 0, 0, 0.1);
}

.user-info-badge {
  background-color: rgba(255, 255, 255, 0.15);
  color: #fff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.user-info-badge:hover {
  background-color: rgba(255, 255, 255, 0.25);
  transform: translateY(-1px);
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.3);
}

.user-info-badge .emoji {
  font-size: 18px;
}

.user-info-badge .username {
  font-weight: 600;
  font-family: "Segoe UI", "Pretendard", sans-serif;
}

.user-info-badge .role {
  font-size: 12px;
  font-style: italic;
  opacity: 0.9;
}

.user-info-wrapper {
  position: relative;
  z-index: 10001;
}

.profile-card {
  position: absolute;
  top: 45px;
  right: 0;
  z-index: 10002;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
  width: 260px;
  animation: slideDown 0.2s ease-out;
}

.card-inner {
  padding: 16px 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.emoji-big {
  font-size: 30px;
}

.card-header .name {
  font-weight: bold;
  font-size: 16px;
}

.card-header .email {
  font-size: 13px;
  color: #666;
  margin-top: 2px;
}

.card-header .role {
  font-size: 12px;
  color: #888;
}

.logout-card-btn {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  border: none;
  background: #f44336;
  color: white;
  font-weight: bold;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.logout-card-btn:hover {
  background: #d32f2f;
}

@keyframes slideDown {
  from {
    transform: translateY(-10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.notification-wrapper {
  position: relative;
  display: inline-block;
  margin-left: 1rem;
}

.bell-icon {
  font-size: 1.8rem;
  cursor: pointer;
  transition: transform 0.3s;
  position: relative;
}

.bell-icon.active {
  color: #ff6b6b; /* 예쁜 밝은 레드 */
  //border: 5px solid rgba(255, 107, 107, 0.7); /* 살짝 투명한 테두리 */
  border-radius: 50%;
  padding: 6px;
  background-color: rgba(255, 107, 107, 0.15); /* 연한 배경 */
  box-shadow: 0 0 12px rgba(255, 107, 107, 0.5); /* 부드러운 글로우 */
  transition: all 0.3s ease;
  animation: softPulse 1.8s infinite ease-in-out; /* 부드럽게  퍼지는 애니메이션 */
}

@keyframes softPulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.6);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(255, 107, 107, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0);
  }
}

.badge {
  position: absolute;
  top: -4px;
  right: -6px;
  background: red;
  color: white;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 0.7rem;
  font-weight: bold;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(255,0,0,0.6); }
  70% { box-shadow: 0 0 0 8px rgba(255,0,0,0); }
  100% { box-shadow: 0 0 0 0 rgba(255,0,0,0); }
}
.alert-modal-below-nav {
  position: absolute;
  top: 60px; /* 네비게이션 바 아래 위치 */
  right: 10px;
  background: white;
  padding: 1.5rem;
  border-radius: 10px;
  width: 420px;
  max-height: 70vh;
  overflow-y: auto;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
  z-index: 2000;
}

.alert-modal {
  width: 100%;
}

.modal-title {
  font-size: 20px;
  color: Black;
  font-weight: bold;
  margin-bottom: 1rem;
}

.alert-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column; /* ✅ 아래쪽으로 나열 */
  gap: 8px;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px;
  border-bottom: 1px solid #eee;
  word-break: break-word;
}

.message {
  font-size: 15px;
  margin-right: 10px;
  color: red;
  white-space: normal;
  word-break: break-word;
}
.timestamp {
  font-size: 15px;
  color: #888;
  color: red;
}

.delete-btn {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
}

.delete-btn:hover {
  background: #c0392b;
}

</style>

