<template>
  <nav class="top-nav">
    <!-- 햄버거/모바일 -->
    <button class="hamburger" @click="toggleMobileMenu" aria-label="메뉴 열기">
      <span :class="{ open: mobileMenuOpen }"></span>
      <span :class="{ open: mobileMenuOpen }"></span>
      <span :class="{ open: mobileMenuOpen }"></span>
    </button>

    <!-- PC 메뉴(가운데 정렬) -->
    <ul v-if="isLoggedIn && !isMobile" class="nav-menu">
      <li
        v-for="item in filteredMenuItems"
        :key="item.path"
        :class="{ active: isMenuActive(item) }"
        @click="(e) => handleMenuClick(item, e)"
      >
        <i :class="item.iconClass + ' nav-icon'"></i>
        {{ item.name }}
        <span v-if="item.hasSub" class="menu-arrow">▼</span>
      </li>
    </ul>
    <!-- 우측 알림/프로필 -->
    <div v-if="isLoggedIn && !isMobile" class="nav-right">
      <!-- 알림(종) -->
      <div class="notification-wrapper">
        <div class="bell-icon" :class="{ active: hasUnread }" @click.stop="toggleModal">
          🔔
          <span v-if="hasUnread" class="badge">{{ unreadCount }}</span>
        </div>
        <transition name="fade">
          <div v-if="showModal" class="alert-modal-below-nav" ref="alertModal">
            <div class="alert-modal">
              <h3 class="modal-title">알림 내역</h3>
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
        </transition>
      </div>
      <!-- 프로필(사용자) -->
      <div class="user-info-wrapper">
        <div class="user-info-badge" @click.stop="toggleProfile">
          <span class="emoji">{{ roleEmoji }}</span>
          <span class="username">{{ user.username }}님</span>
          <span class="role">({{ user.userRole }})</span>
        </div>
        <transition name="fade">
          <div v-if="showProfile" class="profile-card" ref="profileCard">
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
        </transition>
      </div>
    </div>
    <!-- 모바일 메뉴(전체 오버레이) -->
    <transition name="slide">
      <div v-if="mobileMenuOpen && isMobile && isLoggedIn" class="mobile-menu-backdrop" @click="closeMobileMenu"></div>
    </transition>
    <transition name="slide">
      <ul v-if="mobileMenuOpen && isMobile && isLoggedIn" class="mobile-nav-menu">
        <li
          v-for="item in filteredMenuItems"
          :key="item.path"
          :class="{ active: selectedMenu === item.path }"
          @click="(e) => handleMobileMenuClick(item, e)"
        >
          <i :class="item.iconClass + ' nav-icon'"></i>
          {{ item.name }}
          <span v-if="item.hasSub" class="menu-arrow">▼</span>
        </li>
        <!-- 모바일 하단에 알림/프로필 -->
        <li style="margin-top: 20px; border-top: 1px solid #ddd;">
          <div style="display: flex; gap: 18px; align-items:center;">
            <div class="bell-icon mobile" :class="{ active: hasUnread }" @click.stop="toggleModal">
              🔔<span v-if="hasUnread" class="badge">{{ unreadCount }}</span>
            </div>
            <div class="user-info-badge mobile" @click.stop="toggleProfile">
              <span class="emoji">{{ roleEmoji }}</span>
              <span class="username">{{ user.username }}님</span>
              <span class="role">({{ user.userRole }})</span>
            </div>
          </div>
        </li>
        <!-- 모바일 알림/프로필 팝업(카드)도 별도로! -->
        <transition name="fade">
          <div v-if="showModal" class="alert-modal-below-nav mobile-modal">
            <div class="alert-modal">
              <h3 class="modal-title">알림 내역</h3>
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
        </transition>
        <transition name="fade">
          <div v-if="showProfile" class="profile-card mobile-modal">
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
        </transition>
      </ul>
    </transition>
    <div v-if="!isLoggedIn" style="color: white; margin-left:20px;">
      😆 로그인 상태가 아닙니다
    </div>
    <!-- 드롭다운 (기존과 동일) -->
    <transition name="fade-slide-down">
      <div v-if="showSubMenu" class="nav-dropdown" :style="dropdownStyle" @click.stop>
        <ul>
          <li v-for="sub in activeSubMenus" :key="sub.path" :class="{ active: selectedMenu === sub.path }" @click="() => navigateToSub(sub.path)">
            <i :class="sub.iconClass + ' mr-2'"></i>
            {{ sub.name }}
          </li>
        </ul>
      </div>
    </transition>
  </nav>
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
const user = computed(() => store.state.user || {});
const isMobile = ref(window.innerWidth <= 800);

const showProfile = ref(false);
const showModal = ref(false);
const alerts = ref([]);
const userId = computed(() => user.value?.loginId);
const selectedMenu = ref(route.path);
const hasUnread = computed(() => alerts.value.length > 0);
const unreadCount = computed(() => alerts.value.length);
const mobileMenuOpen = ref(false);

// 바깥 클릭시 닫힘 처리 (종/사용자 각각 독립)
const handleClickOutside = (event) => {
  // PC 모드: 알림/프로필
  const profileCard = document.querySelector(".profile-card:not(.mobile-modal)");
  const userBadge = document.querySelector(".user-info-badge:not(.mobile)");
  const alertModal = document.querySelector(".alert-modal-below-nav:not(.mobile-modal)");
  const bell = document.querySelector(".bell-icon:not(.mobile)");

  if (
    showProfile.value &&
    profileCard &&
    !profileCard.contains(event.target) &&
    userBadge &&
    !userBadge.contains(event.target)
  ) {
    showProfile.value = false;
  }
  if (
    showModal.value &&
    alertModal &&
    !alertModal.contains(event.target) &&
    bell &&
    !bell.contains(event.target)
  ) {
    showModal.value = false;
  }
  // 모바일: 오버레이 닫힐 때
  if (isMobile.value && !mobileMenuOpen.value) {
    showProfile.value = false;
    showModal.value = false;
  }
};

onMounted(() => {
  window.addEventListener("resize", handleResize);
  document.addEventListener("click", handleClickOutside);
  fetchAlerts();
  connectWebSocket({ onAlertMessage: handleAlertMessage });
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  document.removeEventListener("click", handleClickOutside);
  disconnectWebSocket();
});
watch(user, (newVal) => {
  if (!newVal || !newVal.username) showProfile.value = false;
});

// --- 기타 메뉴/드롭다운 기능(기존 그대로) ---
const showSubMenu = ref(false);
const dropdownStyle = ref({});
const activeSubMenus = ref([]);
const subMenuType = ref("");
const subMenuOriginRect = ref(null);

const menuItems = [
  { name: "첫화면", path: "/", roles: ["ADMIN", "DBA", "EAI", "VIEW"], iconClass: "pi pi-home" },
  { name: "DB 전체 리스트", path: "/db-list", roles: ["ADMIN", "DBA", "EAI"], iconClass: "pi pi-database" },
  { name: "SMS 전송 내역", path: "/sms-history", roles: ["ADMIN", "DBA", "EAI", "VIEW"], iconClass: "pi pi-send" },
  { name: "테이블스페이스관리", path: "#", roles: ["ADMIN", "DBA"], iconClass: "pi pi-align-left", hasSub: true, subMenu: "space" },
  { name: "일일점검", path: "#", roles: ["ADMIN", "DBA"], iconClass: "pi pi-calendar", hasSub: true, subMenu: "daily" },
  { name: "패스워드관리", path: "/Manager", roles: ["ADMIN", "DBA"], iconClass: "pi pi-key" },
  { name: "ETL DB관리", path: "#", roles: ["ADMIN"], iconClass: "pi pi-share-alt", hasSub: true, subMenu: "etl" },
];
const SpaceSubMenus = [
  { name: "임계치 리스트", path: "/threshold-list", roles: ["ADMIN", "DBA"], iconClass: "pi pi-sliders-h" },
  { name: "테이블스페이스 리스트", path: "/tablespaces", roles: ["ADMIN", "DBA"], iconClass: "pi pi-align-left" },
];
const dailySubMenus = [
  { name: "일일 점검(hit율)", path: "/dailyChk", iconClass: "pi pi-chart-bar" },
  { name: "일일 점검(SYS)", path: "/SysInfoDetail", iconClass: "pi pi-chart-line" },
];

const etlSubMenus = [
  { name: "DB연결정보관리", path: "/EtlDbConnection", iconClass: "pi pi-database" },
  { name: "관제 모듈 관리", path: "/MonitorModuleManage", iconClass: "pi pi-sitemap" },
  { name: "워크플로우 설계", path: "/EtlWorkflow", iconClass: "pi pi-sitemap" },
  { name: "ETL 작업 관리", path: "/ETLJobList", iconClass: "pi pi-cog" },
  { name: "ETL 실행 관리", path: "/ETLjob-Scheduler", iconClass: "pi pi-list" },
];


const subMenuMap = { daily: dailySubMenus, etl: etlSubMenus, space: SpaceSubMenus };
function isMenuActive(item) {
  if (item.hasSub && item.subMenu) {
    const subMenus = subMenuMap[item.subMenu] || [];
    return subMenus.some(sub => selectedMenu.value === sub.path);
  }
  return selectedMenu.value === item.path;
}
const filteredMenuItems = computed(() => {
  const role = user.value?.userRole?.toUpperCase();
  return menuItems.filter(item => item.roles.includes(role));
});
function handleMenuClick(item, event) {
  if (item.hasSub) {
    const rect = event.target.getBoundingClientRect();
    dropdownStyle.value = { left: `${rect.left}px`, top: `62px` };
    showSubMenu.value = subMenuType.value !== item.subMenu || !showSubMenu.value;
    activeSubMenus.value = subMenuMap[item.subMenu] || [];
    subMenuType.value = item.subMenu;
    subMenuOriginRect.value = rect;
  } else {
    showSubMenu.value = false;
    navigateTo(item.path);
  }
  selectedMenu.value = item.path;
}
function handleMobileMenuClick(item) {
  if (item.hasSub) {
    if (activeSubMenus.value === subMenuMap[item.subMenu]) {
      activeSubMenus.value = [];
    } else {
      activeSubMenus.value = subMenuMap[item.subMenu];
    }
    return;
  }
  navigateTo(item.path);
  closeMobileMenu();
}
function closeAllDropdowns() {
  showSubMenu.value = false;
  activeSubMenus.value = [];
  subMenuType.value = "";
}
function closeMobileMenu() {
  mobileMenuOpen.value = false;
  closeAllDropdowns();
  showProfile.value = false;
  showModal.value = false;
}
function navigateToSub(path) {
  selectedMenu.value = path;
  router.push(path);
  showSubMenu.value = false;
  closeMobileMenu();
}
function navigateTo(path) {
  selectedMenu.value = path;
  router.push(path);
  showSubMenu.value = false;
}
function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value;
  if (!mobileMenuOpen.value) activeSubMenus.value = [];
}

// --- 알림/유저/로그아웃/프로필 ---
const toggleProfile = () => (showProfile.value = !showProfile.value);
const toggleModal = () => { if (alerts.value.length > 0) showModal.value = !showModal.value; };
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
    case "DEV": return "💻";
    case "VIEW": return "🐥";
    default: return "😊";
  }
});
// --- 알림 관련 ---
const sortedAlerts = computed(() =>
  [...alerts.value].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
);
function fetchAlerts() {
  const uid = userId.value;
  const token = localStorage.getItem("accessToken");
  api
    .get(`/api/alerts/${uid}/alerts`, { headers: { Authorization: `Bearer ${token}` } })
    .then((res) => {
      alerts.value = res.data.map((a) => ({
        id: a.alert.id,
        message: a.alert.message,
        createdAt: a.alert.createdAt,
      }));
    })
    .catch((err) => console.error("알림 조회 실패:", err));
}
function dismissAlert(alert) {
  const uid = userId.value;
  const alertId = alert.id;
  api
    .put(`/api/alerts/${alertId}/hide?userId=${uid}`)
    .then(() => {
      alerts.value = alerts.value.filter((a) => a.id !== alertId);
    })
    .catch((err) => console.error("알림 삭제 실패:", err));
}
function handleAlertMessage(payload) {
  if (payload.message) fetchAlerts();
}
function formatDate(dateStr) {
  const date = new Date(dateStr);
  return date.toLocaleString("ko-KR", { hour12: false });
}
function handleResize() {
  isMobile.value = window.innerWidth <= 800;
  if (!isMobile.value) mobileMenuOpen.value = false;
}
</script>

<style scoped>
/* --- 전체 구조 --- */
.top-nav {
  position: fixed; top: 0; left: 0; width: 100vw; height: 64px;
  background: linear-gradient(90deg, #4caf50 70%, #388e3c 100%);
  box-shadow: 0 4px 18px 0 rgba(0,0,0,0.10), 0 1.5px 6px 0 rgba(34,139,34,0.13);
  z-index: 1100;
  display: flex; align-items: center; justify-content: center;
  font-family: 'Segoe UI', 'Pretendard', sans-serif;
  white-space: nowrap;
}

.nav-menu, .mobile-nav-menu {
  display: flex; align-items: center; gap: 8px; margin: 0; padding: 0; list-style: none;
}

.nav-menu {
  flex: 1 1 auto;
  justify-content: center;
}

.nav-menu li {
  display: flex;
  align-items: center;
  padding: 0 14px;
  height: 40px;
  min-height: 40px;
  line-height: 40px;
  font-size: 15px;
  color: #fff;
  border-radius: 14px 14px 0 0;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: all 0.18s;
  position: relative;
  background: transparent;
  margin: 0 2px;
}
.nav-menu li.active,
.nav-menu li:hover {
  background: rgba(255,255,255,0.18);
  color: #fff;
  box-shadow: 0 6px 24px 0 rgba(34,139,34,0.10);
  border-radius: 22px 22px 16px 16px;
  transform: translateY(-1px) scale(1.05);
}
.nav-menu li .nav-icon {
  font-size: 1.3em;
  margin-right: 7px;
}
.nav-menu li.active .nav-icon,
.nav-menu li:hover .nav-icon {
  transform: scale(1.18) rotate(-7deg);
}
.menu-arrow { margin-left: 7px; font-size: 14px; opacity: 0.7; }

.nav-right {
  margin-left: 28px; display: flex; align-items: center; gap: 18px;
  position: absolute; right: 36px; top: 0; height: 100%;
}

/* --- 알림/종 --- */
.notification-wrapper {
  position: relative; display: flex; align-items: center;
}
.bell-icon {
  font-size: 1.7rem; cursor: pointer; position: relative;
}
.bell-icon.active { color: #ff6b6b; }
.badge {
  position: absolute; top: -5px; right: -8px;
  background: #e74c3c; color: #fff; border-radius: 50%;
  padding: 2px 6px; font-size: 0.7rem; font-weight: bold;
}
.alert-modal-below-nav {
  position: absolute; top: 42px; right: 0;
  min-width: 320px; max-width: 370px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.13);
  padding: 22px 20px 16px 20px;
  z-index: 3000; overflow-y: auto; max-height: 55vh;
}
@media (max-width: 800px) {
  .alert-modal-below-nav { left: 12vw !important; right: auto !important; top: 70px !important; }
  .mobile-modal { width: 95vw !important; min-width: 0 !important; left: 0 !important; }
}
.modal-title {
  font-size: 20px; color: #333; font-weight: bold; margin-bottom: 14px; letter-spacing: -1px; display: flex; align-items: center; gap: 8px;
}
.modal-title::before { content: "📢"; font-size: 21px; }
.alert-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.alert-item { display: flex; justify-content: space-between; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid #eee; word-break: break-word; gap: 8px; }
.alert-item:last-child { border-bottom: none; }
.message { font-size: 15px; color: #212; white-space: normal; }
.timestamp { font-size: 12px; color: #888; margin-bottom: 2px; display: block; }
.delete-btn { background: #e74c3c; color: white; border: none; padding: 3px 9px; border-radius: 5px; cursor: pointer; font-size: 13px; font-weight: 500; margin-top: 6px; box-shadow: 0 2px 10px rgba(230, 80, 80, 0.08); transition: background 0.15s; }
.delete-btn:hover { background: #b71c1c; }

/* --- 사용자/프로필 --- */
.user-info-wrapper { position: relative; margin-left: 1rem; display: flex; align-items: center; }
.user-info-badge {
  background: rgba(255,255,255,0.15); color: #fff;
  padding: 6px 14px; border-radius: 18px; display: flex; align-items: center; gap: 7px; font-size: 15px; cursor: pointer;
}
.user-info-badge:hover { background: rgba(255,255,255,0.24);}
.profile-card {
  position: absolute; top: 44px; right: 0; background: white; border-radius: 12px; box-shadow: 0 8px 20px rgba(0,0,0,0.18); width: 250px; z-index: 3100; animation: slideDown 0.2s;
}
.card-inner { padding: 16px 20px; }
.card-header { display: flex; align-items: center; gap: 12px; }
.emoji-big { font-size: 30px; }
.card-header .name { font-weight: bold; font-size: 16px; color: #666; }
.card-header .email { font-size: 13px; color: #666; margin-top: 2px; }
.card-header .role { font-size: 12px; color: #888; }
.logout-card-btn { margin-top: 12px; width: 100%; padding: 10px; border: none; background: #f44336; color: white; font-weight: bold; border-radius: 6px; cursor: pointer; transition: background 0.2s ease; }
.logout-card-btn:hover { background: #d32f2f; }
@keyframes slideDown { from { transform: translateY(-10px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

/* --- 드롭다운/기타(기존 동일) --- */
.nav-dropdown {
  position: absolute; background: #fff; border-radius: 0 0 14px 14px; box-shadow: 0 6px 32px 0 rgba(76,175,80,0.14); animation: fadeInDrop 0.22s;
  min-width: 160px; padding: 8px 0 8px 0; z-index: 1300; opacity: 0.98; top: 62px;
}
@keyframes fadeInDrop { 0% { opacity: 0; transform: translateY(-16px);} 100% { opacity: 1; transform: translateY(0);} }
.nav-dropdown ul { display: flex; flex-direction: column; gap: 0; margin: 0; padding: 0; }
.nav-dropdown li { color: #388e3c; font-weight: 600; padding: 5px 16px; border-radius: 8px; transition: background 0.18s; cursor: pointer; list-style: none !important; margin: 0; min-width: 0;}
.nav-dropdown li.active, .nav-dropdown li:hover { background: #e8f5e9; color: #212; }

/* --- 햄버거/모바일 슬라이드 --- */
.hamburger { display: none; flex-direction: column; justify-content: center; gap: 6px; background: none; border: none; outline: none; margin-left: 16px; z-index: 1500; cursor: pointer;}
.hamburger span { width: 30px; height: 4px; border-radius: 3px; background: #fff; display: block; transition: all 0.24s; }
.hamburger span.open:nth-child(1) { transform: translateY(10px) rotate(45deg);}
.hamburger span.open:nth-child(2) { opacity: 0;}
.hamburger span.open:nth-child(3) { transform: translateY(-10px) rotate(-45deg);}
.mobile-nav-menu {
  display: flex; flex-direction: column; gap: 8px; position: fixed; top: 0; left: 0; width: 74vw; max-width: 340px; height: 100vh;
  background: linear-gradient(90deg, #4caf50 70%, #388e3c 100%); box-shadow: 2px 0 12px 0 rgba(0,0,0,0.08); z-index: 1600; padding: 80px 0 0 0;
  font-size: 15px; animation: slideIn 0.26s;
}
@keyframes slideIn { 0% { transform: translateX(-100%);} 100% { transform: translateX(0);} }
.mobile-menu-backdrop { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(30,40,45,0.09); z-index: 1599; animation: fadeInBg 0.18s; }
@keyframes fadeInBg { from {opacity:0;} to{opacity:1;} }
.user-info-badge.mobile { margin-top: 40px; }

/* --- 반응형 --- */
@media (max-width: 1100px) {
  .nav-menu li { padding: 0 13px; font-size: 15px;}
  .nav-dropdown { min-width: 175px; }
}
@media (max-width: 800px) {
  .top-nav { justify-content: flex-start;}
  .nav-menu { display: none !important; }
  .hamburger { display: flex; }
}
</style>
