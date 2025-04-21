<template>
  <div id="app">
    <NavBar v-if="isLoggedIn" @logout="handleLogout" />
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import NavBar from "./components/NavBar.vue";
import api from "@/api";

const store = useStore();
const router = useRouter();
const isLoggedIn = computed(() => store.state.isLoggedIn);

const handleLogout = () => {
  store.dispatch("logout");
  router.push("/login");
};

onMounted(async () => {
  const accessToken = localStorage.getItem("accessToken");
  const refreshToken = localStorage.getItem("refreshToken");
  const userRaw = localStorage.getItem("user");

  // ✅ 복원 처리 (user가 없으면 강제 로그아웃도 가능)
  if (accessToken && !store.state.isLoggedIn) {
    try {
      const user = JSON.parse(userRaw);
      store.commit("setUser", user);
      store.commit("setLoggedIn", true);
    } catch (e) {
      console.warn("유저 정보 복원 실패", e);
      store.dispatch("logout");
    }
  }

  // ✅ accessToken 없고 refreshToken 있을 경우 → 자동 재발급
  if (!accessToken && refreshToken) {
    try {
      const res = await api.post("/auth/refresh", { refreshToken });
      const newToken = res.data.accessToken;
      localStorage.setItem("accessToken", newToken);
      store.commit("setLoggedIn", true);
    } catch (err) {
      console.warn("토큰 재발급 실패", err);
      store.dispatch("logout");
      router.push("/login");
    }
  }

  // 둘 다 없고 로그인 페이지가 아니라면
  if (!accessToken && !refreshToken && router.currentRoute.value.path !== "/login") {
    router.push("/login");
  }
});
</script>


<style>
.main-content {
  margin-top: 60px;
  padding: 20px;
}
</style>
