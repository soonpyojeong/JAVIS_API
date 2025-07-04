<template>
  <div>
    <div v-if="mobileMenuOpen && isMobile && isLoggedIn" class="mobile-menu-backdrop" @click="closeMobileMenu" />
    <nav class="top-nav">
      <button class="hamburger" @click="toggleMobileMenu" aria-label="메뉴 열기">
        <span :class="{ open: mobileMenuOpen }"></span>
        <span :class="{ open: mobileMenuOpen }"></span>
        <span :class="{ open: mobileMenuOpen }"></span>
      </button>

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
      <!-- ✅ PC용 서브 메뉴 렌더링 -->
      <transition name="fade">
        <ul
          v-if="showSubMenu && activeSubMenus.length"
          class="pc-sub-menu"
          :style="dropdownStyle"
        >
          <li
            v-for="sub in activeSubMenus"
            :key="sub.path"
            :class="{ active: selectedMenu === sub.path }"
            @click="navigateTo(sub.path)"
          >
            <i v-if="sub.iconClass" :class="sub.iconClass + ' sub-icon'" />
            {{ sub.name }}
          </li>
        </ul>
      </transition>

      <div v-if="isLoggedIn && !isMobile" class="nav-right">
        <div class="notification-wrapper">
          <div class="bell-icon" :class="{ active: hasUnread }" @click.stop="toggleModal">
            🔔<span v-if="hasUnread" class="badge">{{ unreadCount }}</span>
          </div>
        </div>
        <transition name="fade">
          <div v-if="showModal" class="alert-modal-vertical">
            <div v-if="alerts.length === 0" class="empty-alert">📭 알림이 없습니다.</div>
            <div class="alert-scroll-vertical">
              <div
                v-for="alert in sortedAlerts"
                :key="alert.id"
                class="alert-card-vertical"
              >
                <div class="alert-message">{{ alert.message }}</div>
                <div class="alert-time">{{ formatDate(alert.createdAt) }}</div>
                <button class="alert-dismiss-btn" @click="dismissAlert(alert)">❌</button>
              </div>
            </div>
          </div>
        </transition>

        <div class="user-info-wrapper">
          <div class="user-info-badge" @click.stop="toggleProfile">
            <span class="emoji">{{ roleEmoji }}</span>
            <span class="username">{{ user.username }}님</span>
            <span class="role">({{ user.userRole }})</span>
          </div>
          <transition name="fade">
            <div v-if="showProfile" class="profile-card">
              <div class="card-inner">
                <div class="card-header">
                  <span class="emoji-big">{{ roleEmoji }}</span>
                  <div>
                    <div class="name">{{ user.username }}</div>
                    <div class="email">{{ user.email || '이메일 없음' }}</div>
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

      <!-- 모바일 메뉴 -->
      <transition name="slide">
        <ul
          v-if="mobileMenuOpen && isMobile && isLoggedIn"
          class="mobile-nav-menu"
        >
          <li v-for="item in filteredMenuItems" :key="item.path">
            <div
              @click.stop="handleMobileMenuClick(item)"
              class="mobile-main-menu-item"
            >
              <div class="menu-item-content">
                <i :class="item.iconClass + ' nav-icon'" />
                {{ item.name }}
              </div>
              <span v-if="item.hasSub" class="menu-arrow">▼</span>
            </div>

            <!-- 🔐 수정된 부분 -->
            <transition name="slide-sub">
              <ul
                v-if="item.hasSub && activeSubMenuType === item.subMenu"
                class="mobile-sub-menu"
              >
                <li
                  v-for="sub in subMenuMap[item.subMenu] || []"
                  :key="sub.path"
                  @click="navigateToMobileSub(sub.path)"
                  :class="{ active: selectedMenu === sub.path }"
                >
                  {{ sub.name }}
                </li>
              </ul>
            </transition>
          </li>

          <!-- 유저 정보 -->
          <div class="user-info-wrapper-mobile">
            <div class="user-info-badge" @click.stop="toggleProfile">
              <span class="emoji">{{ roleEmoji }}</span>
              <span class="username">{{ user.username }}님</span>
              <span class="role">({{ user.userRole }})</span>
            </div>
            <transition name="fade">
              <div v-if="showProfile" class="profile-card-mobile">
                <div class="card-inner">
                  <div class="card-header">
                    <span class="emoji-big">{{ roleEmoji }}</span>
                    <div>
                      <div class="name">{{ user.username }}</div>
                      <div class="role">🏷 {{ user.userRole }}</div>
                    </div>
                  </div>
                  <hr />
                  <button class="logout-card-btn" @click="logout">🚪 로그아웃</button>
                </div>
              </div>
            </transition>
          </div>
        </ul>
      </transition>

    </nav>
  </div>
</template>



<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount,nextTick } from "vue";
import { useStore } from "vuex";
import { useRouter, useRoute } from "vue-router";
import api from "@/api";
import { connectWebSocket, disconnectWebSocket } from "@/websocket";

const store = useStore();
const router = useRouter();
const route = useRoute();

const isLoggedIn = computed(() => store.state.isLoggedIn);
const user = computed(() => store.state.user || {});

const showProfile = ref(false);
const showModal = ref(false);
const alerts = ref([]);
const userId = computed(() => user.value?.loginId);
const selectedMenu = ref(route.path);
const hasUnread = computed(() => alerts.value.length > 0);
const unreadCount = computed(() => alerts.value.length);
const mobileMenuOpen = ref(false);
const isMobile = ref(window.innerWidth <= 1400);
const activeSubMenuType = ref('');
const activeSubMenus = ref([]);



// 바깥 클릭시 닫힘 처리 (종/사용자 각각 독립)
const handleClickOutside = (event) => {
  // PC 모드: 알림/프로필
  const profileCard = document.querySelector(".profile-card:not(.mobile-modal)");
  const userBadge = document.querySelector(".user-info-badge:not(.mobile)");
  const alertModal = document.querySelector(".alert-modal-vertical:not(.mobile-modal)");
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


async function navigateToMobileSub(path) {
  try {
    await router.push(path);
    selectedMenu.value = path;
    mobileMenuOpen.value = false;
    activeSubMenuType.value = '';
  } catch (err) {
    console.error('라우팅 실패:', err);
  }
}



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

watch(mobileMenuOpen, (isOpen) => {
  document.body.style.overflow = isOpen ? 'hidden' : 'auto';
});

// --- 기타 메뉴/드롭다운 기능(기존 그대로) ---
const showSubMenu = ref(false);
const dropdownStyle = ref({});
const subMenuType = ref("");
const subMenuOriginRect = ref(null);

const menuItems = [
  { name: "첫화면", path: "/", roles: ["ADMIN", "DBA", "EAI", "VIEW"], iconClass: "pi pi-home" },
  { name: "DB 전체 리스트", path: "/db-list", roles: ["ADMIN", "DBA", "EAI"], iconClass: "pi pi-database" },
  { name: "SMS 전송 내역", path: "/sms-history", roles: ["ADMIN", "DBA", "EAI", "VIEW"], iconClass: "pi pi-send" },
  { name: "테이블스페이스관리", path: "#", roles: ["ADMIN", "DBA"], iconClass: "pi pi-align-left", hasSub: true, subMenu: "space" },
  { name: "일일점검", path: "#", roles: ["ADMIN", "DBA"], iconClass: "pi pi-calendar", hasSub: true, subMenu: "daily" },
  { name: "패스워드관리", path: "/Manager", roles: ["ADMIN", "DBA"], iconClass: "pi pi-key" },
  { name: "ETL DB관리", path: "#", roles: ["ADMIN", "DBA"], iconClass: "pi pi-share-alt", hasSub: true, subMenu: "etl" },
  { name: "JAVIS권한관리", path: "/ManagerMenuRole", roles: ["ADMIN"], iconClass: "pi pi-share-alt"},
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
  { name: "ETL 실행 관리", path: "/ETLjob-Scheduler", iconClass: "pi pi-list" }
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
    const rect = event.currentTarget.getBoundingClientRect();
    dropdownStyle.value = {
      left: `${rect.left}px`,
      // top 제거 또는 아래처럼 상대 위치 조정하지 않기
    };
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
    const nextMenu = activeSubMenuType.value === item.subMenu ? '' : item.subMenu;
    //console.log('📌 서브메뉴 토글 시도:', nextMenu, '현재:', item.subMenu);
    activeSubMenuType.value = nextMenu;
  } else {
    navigateToMobileSub(item.path);
  }
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
  showSubMenu.value = path; // 추가
  router.push(path).catch(err => {
    console.error('라우팅 실패:', err);
  });
}


function navigateTo(path) {
  selectedMenu.value = path;
  router.push(path);
  showSubMenu.value = false;
}


function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value;
  if (!mobileMenuOpen.value) {
    closeAllDropdowns(); // 서브메뉴 초기화만!
  }
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
  isMobile.value = window.innerWidth <= 1400;
  if (!isMobile.value) mobileMenuOpen.value = false;
}
</script>

<style scoped>
/* --- 전체 구조 --- */
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 64px;
  background: linear-gradient(90deg, #4caf50 70%, #388e3c 100%);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.1), 0 1.5px 6px rgba(34, 139, 34, 0.13);
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Segoe UI', 'Pretendard', sans-serif;
  white-space: nowrap;
}

/* --- PC 메뉴 --- */
.nav-menu {
  flex: 1 1 auto;
  justify-content: center;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
  position: relative; /* 서브 메뉴 위치 기준이 됨 */
}

.nav-menu li {
  display: flex;
  align-items: center;
  padding: 0 14px;
  height: 40px;
  font-size: 15px;
  color: #fff;
  border-radius: 14px 14px 0 0;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
  position: relative;
  margin: 0 2px;
  background: transparent;
}

.nav-menu li.active,
.nav-menu li:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  box-shadow: 0 6px 24px rgba(34, 139, 34, 0.1);
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

.menu-arrow {
  margin-left: 7px;
  font-size: 14px;
  opacity: 0.7;
}

/* --- PC 서브 메뉴 컨테이너 --- */
.pc-sub-menu {
  position: absolute;
  top: calc(60% + 1px); /* 부모 메뉴 아래 약간 간격 */
  left: 0;
  background: white;
  color: #2e7d32;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  min-width: 180px;
  padding: 10px 0;
  z-index: 1500;
}

/* --- 서브 메뉴 항목 --- */
.pc-sub-menu li {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s ease;
  white-space: nowrap;
}

.pc-sub-menu li:hover {
  background: #f1f8f2;
}

.pc-sub-menu li.active {
  background: #d9efe0;
  font-weight: bold;
}

/* --- 아이콘 있는 항목 --- */
.pc-sub-menu .sub-icon {
  margin-right: 8px;
  font-size: 1.1em;
}


/* --- 알림 영역 --- */
.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
  position: absolute;
  right: 36px;
  height: 100%;
}

.notification-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.bell-icon {
  font-size: 1.7rem;
  cursor: pointer;
  position: relative;
}

.bell-icon.active {
  color: #ff6b6b;
}

.badge {
  position: absolute;
  top: -5px;
  right: -8px;
  background: #e74c3c;
  color: #fff;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 0.7rem;
  font-weight: bold;
}


.alert-modal-vertical {
  position: absolute;
  top: 50px;
  right: 0;
  width: 420px;
  max-height: 400px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  z-index: 3000;
  padding: 20px;
  overflow-y: auto; /* ✅ 세로 스크롤만 */
  overflow-x: hidden;
}

.alert-scroll-vertical {
  display: flex;
  flex-direction: column; /* ✅ 세로 정렬 */
  gap: 14px;
}

.alert-card-vertical {
  background: #f8fff9;
  border: 1px solid #d9efdb;
  border-radius: 10px;
  padding: 14px 18px;
  font-size: 14px;
  color: #2e7d32;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.alert-message {
  font-weight: 500;
  font-size: 15px;
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
}

.alert-time {
  font-size: 13px;
  color: #666;
}

.alert-dismiss-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: none;
  border: none;
  color: #d32f2f;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
}


/* --- 사용자/프로필 --- */
.user-info-wrapper {
  position: relative;
  margin-left: 1rem;
  display: flex;
  align-items: center;
  font-family: 'Segoe UI', 'Pretendard', sans-serif;
  font-weight: bold;
}

.user-info-badge {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  margin: 0; /* 왼쪽 여백 제거 */
  justify-content: flex-start; /* 왼쪽 정렬 명시 */
  padding: 6px 14px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 15px;
  cursor: pointer;
}

.user-info-badge:hover {
  background: rgba(255, 255, 255, 0.24);
}

.profile-card {
  position: absolute;
  top: 44px;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.18);
  width: 250px;
  z-index: 3100;
  animation: slideDown 0.2s;
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
  color: #666;
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

/* --- 모바일 메뉴 전체 컨테이너 --- */
.mobile-nav-menu {
  position: fixed;
  top: 48px; /* 햄버거 아래로 시작 */
  left: 0;
  height: calc(100vh - 64px);
  width: 240px;
  background-color: #d5f2da;
  z-index: 1600;
  padding-top: 16px;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
  animation: slideIn 0.2s ease-out;
  list-style: none;
}

/* --- 메인 메뉴 항목 --- */
.mobile-main-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
  color: #2e7d32;
  padding: 10px 18px;
  cursor: pointer;
  transition: background 0.2s;
}
.mobile-main-menu-item:hover {
  background: #c6eacb;
}
.mobile-main-menu-item .nav-icon-wrapper {
  width: 28px;
  height: 28px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
}
.mobile-main-menu-item .label {
  flex: 1;
}

/* --- 모바일 메뉴 --- */
.mobile-nav-menu {
  position: fixed;
  top: 48px; /* 햄버거 아래로 시작 */
  left: 0;
  height: calc(100vh - 64px); /* 남은 화면 높이만큼 */
  width: 240px;
  background-color: #d5f2da; /* 연한 초록 단색 */
  z-index: 1600;
  padding-top: 16px;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  gap: 6px;
}



/* --- 햄버거 버튼 기본 숨김 --- */
.hamburger {
  position: fixed;
  top: 12px;
  left: 16px;
  display: none; /* 기본은 숨김 */
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 44px;
  height: 44px;
  background: none;
  border: none;
  outline: none;
  cursor: pointer;
  z-index: 2000;
}

/* --- 1000px 이하일 때만 보이게 --- */
@media (max-width: 1000px) {
  .hamburger {
    display: flex;
  }
}
.hamburger span {
  display: block;
  width: 26px;
  height: 3px;
  background-color: #ffffff;
  border-radius: 2px;
  transition: transform 0.25s ease, opacity 0.25s ease;
}

/* --- 햄버거 토글(open) 시 애니메이션 --- */
.hamburger span.open:nth-child(1) {
  transform: translateY(8px) rotate(45deg);
}
.hamburger span.open:nth-child(2) {
  opacity: 0;
}
.hamburger span.open:nth-child(3) {
  transform: translateY(-8px) rotate(-45deg);
}

@media (max-width: 1400px) {
  .hamburger {
    display: flex;
  }
}



/* --- 서브 메뉴 전체 --- */
.mobile-sub-menu {
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

/* --- 서브 메뉴 항목 --- */
.mobile-sub-menu li {
  position: relative;
  padding: 6px 12px 6px 22px; /* 왼쪽 여백 확보 */
  color: #2e7d32;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

/* Hover 시 배경 살짝 강조 */
.mobile-sub-menu li:hover {
  background: #c8eacd;
}

/* 활성화 서브 메뉴 항목 */
.mobile-sub-menu li.active {
  background: #aedbb2;
  font-weight: 600;
}
/* 모바일 유저 영역 전체 */
.user-info-wrapper-mobile {
  padding: 0 16px 16px 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

/* 모바일 유저 배지 */
.user-info-wrapper-mobile .user-info-badge {
  width: 100%;
  background: rgba(255, 255, 255, 0.18);
  color: #2e7d32;
  padding: 10px 16px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

/* 모바일 프로필 카드 */
.profile-card-mobile {
  width: 100%;
  margin-top: 12px;
  background: #ffffff;
  color: #2e7d32;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.profile-card-mobile .card-inner {
  padding: 16px;
}

.profile-card-mobile .card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.profile-card-mobile .emoji-big {
  font-size: 32px;
}

.profile-card-mobile .name {
  font-size: 16px;
  font-weight: bold;
}

.profile-card-mobile .email,
.profile-card-mobile .role {
  font-size: 14px;
  color: #4e944f;
}

.logout-card-btn {
  width: 100%;
  background: #f0f0f0;
  border: none;
  padding: 10px 12px;
  border-radius: 8px;
  color: #333;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.logout-card-btn:hover {
  background: #e0e0e0;
}


</style>
