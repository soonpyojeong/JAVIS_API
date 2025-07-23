<template>

  <div id="app">

    <Toast position="center" />
    <!-- NavBar/메인레이아웃은 로그인 등 일반 화면에서만 노출 -->
    <NavBar v-if="isNavReady && showNav" @logout="handleLogout" />
    <div class="main-content" v-if="showNav">
      <router-view />
    </div>
    <!-- NavBar 등 없이 그냥 이 페이지만 뜨게 -->
    <router-view v-else />
    <footer style="text-align: center; font-size: 12px; color: #888; margin-top: 40px;">
      Made by <strong style="color: #555;">soonpyo.jeong</strong> ·
      <a href="mailto:jsp5247@gmail.com" style="color: #666; text-decoration: underline;">
        jsp5247@gmail.com
      </a>
    </footer>
  </div>

</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useStore } from "vuex";
import { useRouter, useRoute } from "vue-router";
import NavBar from "./components/NavBar.vue";
import api from "@/api";
const store = useStore();
const router = useRouter();
const route = useRoute();

const isNavReady = ref(false);

// 여기에 reset-password name 추가! (라우트 name 반드시 맞춰야 함)
const hideNavForRoutes = ["ResetPasswordPage"]; // 확장 가능
const showNav = computed(() => !hideNavForRoutes.includes(route.name));

// 경로 자체로도 예외처리 가능 (더 확실하게!)
const isResetPasswordPath = computed(() => route.path.startsWith("/reset-password"));

const handleLogout = () => {
  store.dispatch("logout");
  router.push("/login");
};

onMounted(async () => {
  await router.isReady();

  const accessToken = localStorage.getItem("accessToken");
  const refreshToken = localStorage.getItem("refreshToken");
  const userRaw = localStorage.getItem("user");

  let sessionRestored = false;

  if (accessToken && !store.state.isLoggedIn) {
    try {
      const user = JSON.parse(userRaw);
      store.commit("setUser", user);
      store.commit("setLoggedIn", true);
      sessionRestored = true;
    } catch {
      store.dispatch("logout");
    }
  }

  // ✅ 리프레시 토큰 처리도 reset-password 경로는 예외!
  if (!accessToken && refreshToken && !isResetPasswordPath.value) {
    try {
      // refresh API는 항상 /api/auth/refresh 형태로 호출!
      const res = await api.post("/api/auth/refresh", { refreshToken });
      const newToken = res.data.accessToken;
      localStorage.setItem("accessToken", newToken);
      store.commit("setLoggedIn", true);
      sessionRestored = true;
    } catch {
      store.dispatch("logout");
      router.push("/login");
      return;
    }
  }

  // ✅ 로그인 강제 이동도 reset-password에서는 하지 않음!
  if (
    !accessToken &&
    !refreshToken &&
    router.currentRoute.value.path !== "/login" &&
    !isResetPasswordPath.value
  ) {
    router.push("/login");
  }

  // 로그인 상태 복구 후 /login에 있다면 홈으로 이동 (예외: reset-password)
  if (
    sessionRestored &&
    router.currentRoute.value.path === "/login" &&
    !isResetPasswordPath.value
  ) {
    router.replace("/");
  }

  isNavReady.value = true;
});
</script>

<style>
.main-content {
  margin-top: 55px;
  padding: 10px;
}
</style>
