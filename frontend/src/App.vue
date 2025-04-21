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
import api from "@/api"; // 공통 axios 인스턴스 가져오기

const store = useStore();
const router = useRouter();

// 로그인 상태 계산
const isLoggedIn = computed(() => store.state.isLoggedIn);

api.create({
  baseURL: "http://10.90.4.60:8813/api",
  withCredentials: true, // 쿠키를 전달
});


// 로그아웃 핸들러
const handleLogout = () => {
  store.dispatch("logout");
  router.push("/login");
};

// 컴포넌트 마운트 후 로그인 상태 확인
onMounted(() => {
  if (!isLoggedIn.value && router.currentRoute.value.path !== "/login") {
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
