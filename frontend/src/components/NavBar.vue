<template>
  <div class="top-nav">
    <ul v-if="isLoggedIn">
      <li
        v-for="item in menuItems"
        :key="item.path"
        :class="{ active: selectedMenu === item.path }"
        @click="navigateTo(item.path)"
      >        {{ item.name }}
      </li>
      <!-- 👤 유저 뱃지 및 프로필 카드 감싸기 -->
      <li v-if="user && user.username" class="user-info-wrapper">
        <div class="user-info-badge" @click="toggleProfile">
          <span class="emoji">{{ roleEmoji }}</span>
          <span class="username">{{ user.username }}님</span>
          <span class="role">({{ user.userRole }})</span>
        </div>
        <!-- ✨ 프로필 카드: 유저 뱃지 아래에 위치 -->
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
      </li>
    </ul>
    <div v-else style="color: white;">🙆 로그인 상태가 아니다</div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from "vue";
import { useStore } from "vuex";
import { useRouter, useRoute } from "vue-router";

const store = useStore();
const router = useRouter();
const route = useRoute();

const isLoggedIn = computed(() => store.state.isLoggedIn);
const user = computed(() => store.state.user);
const showProfile = ref(false);
const toggleProfile = () => (showProfile.value = !showProfile.value);

const handleClickOutside = (event) => {
  const card = document.querySelector(".profile-card");
  const badge = document.querySelector(".user-info-badge");
  if (card && !card.contains(event.target) && badge && !badge.contains(event.target)) {
    showProfile.value = false;
  }
};

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
  console.log("✅ NavBar 마우트 완료. 로그인 상태:", store.state.isLoggedIn);
  console.log("🥉 사용자 정보 username:", user.value.username);
  console.log("🥉 사용자 정보 userRole:", user.value.userRole);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside);
});

const selectedMenu = ref(route.path);

watch(user, (newVal) => {
  if (!newVal || !newVal.username) {
    showProfile.value = false;
  }
});

const menuItems = [
  { name: "첫화면", path: "/" },
  { name: "DB 전체 리스트", path: "/db-list" },
  { name: "SMS 전송 내역", path: "/sms-history" },
  { name: "임계치 리스트", path: "/threshold-list" },
  { name: "테이블스페이스 리스트", path: "/tablespaces" },
  { name: "일일 점검(hit율)", path: "/dailyChk" },
];

const navigateTo = (path) => {
  router.push(path);
};

const logout = () => {
  store.dispatch("logout");
  window.location.href = "/";
  router.push("/login");
};

const roleEmoji = computed(() => {
  if (!user.value || !user.value.userRole) return "🙂";
  switch (user.value.userRole.toUpperCase()) {
    case "DBA": return "👑";
    case "DEV": return "🧑‍💻";
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
</style>

