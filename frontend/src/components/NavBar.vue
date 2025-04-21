<template>
  <div class="top-nav">
    <ul>
      <li
        v-for="item in menuItems"
        :key="item.path"
        :class="{ active: selectedMenu === item.path }"
        @click="navigateTo(item.path)"
      >
        {{ item.name }}
      </li>
      <li @click="logout" class="logout-btn">로그아웃</li>
    </ul>
  </div>
</template>

<script>
import { ref, onMounted, watchEffect } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useStore } from "vuex";

export default {
  setup() {
    const router = useRouter();
    const route = useRoute();
    const store = useStore();
    const selectedMenu = ref(route.path);

    const menuItems = [
      { name: "첫화면", path: "/" },
      { name: "DB 전체 리스트", path: "/db-list" },
      { name: "SMS 전송 내역", path: "/sms-history" },
      { name: "임계치 리스트", path: "/threshold-list" },
      { name: "테이블스페이스 리스트", path: "/tablespaces" },
      { name: "일일 점검(hit율)", path: "/dailyChk" },
    ];

    const navigateTo = (path) => {
      selectedMenu.value = path;
      localStorage.setItem("selectedMenu", path);
      router.replace(path).catch((err) => {
        if (err.name !== "NavigationDuplicated") {
          console.error("Navigation error:", err);
        }
      });
    };

    const logout = async () => {
      const loginId = store.state.user?.loginId;
      if (loginId) {
        try {
          await fetch("http://10.90.4.60:8813/api/auth/logout", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ loginId }),
          });
        } catch (e) {
          console.warn("서버 로그아웃 실패", e);
        }
      }

      store.dispatch("logout");
      router.push("/login").catch(() => {});
    };



    onMounted(() => {
      const storedMenu = localStorage.getItem("selectedMenu");
      if (storedMenu) {
        selectedMenu.value = storedMenu;
        router.replace(storedMenu);
      }
    });

    watchEffect(() => {
      const storedMenu = localStorage.getItem("selectedMenu");
      if (storedMenu) {
        selectedMenu.value = storedMenu;
      }
    });

    return {
      menuItems,
      selectedMenu,
      navigateTo,
      logout,
    };
  },
};
</script>



<style>
/* 네비게이션 바 스타일 */
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
</style>
