<template>
  <div class="top-nav">
    <ul v-if="isLoggedIn">
      <li
        v-for="item in menuItems"
        :key="item.path"
        :class="{ active: selectedMenu === item.path }"
        @click="navigateTo(item.path)"
      >
        {{ item.name }}
      </li>
       <li v-if="user && user.username" class="user-info-badge" @click="showProfile = true">
         <span class="emoji">{{ roleEmoji }}</span>
         <span class="username">{{ user.username }}님</span>
         <span class="role">({{ user.userRole }})</span>
       </li>

       <!-- ✅ 프로필 모달 -->
       <div v-if="showProfile" class="modal-overlay" @click.self="showProfile = false">
         <div class="modal-content">
           <h3>😎 프로필 정보</h3>
           <p><strong>이름:</strong> {{ user.username }}</p>
           <p><strong>권한:</strong> {{ user.userRole }}</p>
           <p><strong>이메일:</strong> {{ user.email || '이메일 없음' }}</p>
           <button @click="showProfile = false" class="close-btn">닫기</button>
         </div>
       </div>
       <li @click="logout" class="logout-btn">로그아웃</li>
    </ul>
    <div v-else style="color: white;">🙋 로그인 상태가 아닙니다</div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from "vue";
import { useStore } from "vuex";
import { useRouter, useRoute } from "vue-router";

const store = useStore();
const router = useRouter();
const route = useRoute();

const isLoggedIn = computed(() => store.state.isLoggedIn);
// ✅ 사용자 정보 가져오기
const user = computed(() => store.state.user);
const showProfile = ref(false);

const selectedMenu = ref(route.path);

watch(() => route.path, (newPath) => {
  selectedMenu.value = newPath;
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

onMounted(() => {
  console.log("✅ NavBar 마운트 완료. 로그인 상태:", store.state.isLoggedIn);
    console.log("🧩 사용자 정보 username:", user.value.username);
    console.log("🧩 사용자 정보 userRole:", user.value.userRole);
});
</script>

<style scoped>
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #4caf50;
  padding: 10px 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  justify-content: center;
  font-family: 'Arial', sans-serif;
}

.top-nav ul {
  list-style: none;
  display: flex;
  margin: 0;
  padding: 0;
}

.top-nav li {
  margin: 0 15px;
  padding: 10px 15px;
  cursor: pointer;
  color: white;
  font-weight: bold;
  transition: background 0.3s ease, color 0.3s ease;
}

.top-nav li:hover {
  background: #3e8e41;
  border-radius: 5px;
}

.top-nav li.active {
  background: #2e7d32;
  color: #ffffff;
  border-radius: 5px;
}

.logout-btn {
  color: red;
  cursor: pointer;
}

/* ✅ 사용자 정보 스타일 */
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 320px;
  max-width: 90%;
  text-align: center;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
  animation: pop-in 0.2s ease;
}

.modal-content h3 {
  margin-bottom: 15px;
}

.modal-content p {
  margin: 10px 0;
  font-size: 14px;
}

.close-btn {
  margin-top: 15px;
  background: #4caf50;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.close-btn:hover {
  background: #388e3c;
}

@keyframes pop-in {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
