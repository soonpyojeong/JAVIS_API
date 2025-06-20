<template>
  <div id="app">
  <Toast position="center" />
    <!-- 로그인 상태 복원 후에만 NavBar 표시 -->
    <NavBar v-if="isNavReady" @logout="handleLogout" />
    <div class="main-content">
      <router-view />
    </div>
  </div>
    <footer>

    </footer>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import NavBar from "./components/NavBar.vue";
import api from "@/api";
const store = useStore();
const router = useRouter();

const isNavReady = ref(false);
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

  if (!accessToken && refreshToken) {
    try {
      const res = await api.post("/auth/refresh", { refreshToken });
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

  if (!accessToken && !refreshToken && router.currentRoute.value.path !== "/login") {
    router.push("/login");
  }

  if (sessionRestored && router.currentRoute.value.path === "/login") {
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

