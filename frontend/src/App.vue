<template>
  <div id="app">
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
      </ul>
    </div>
    <div class="main-content">
      <router-view /> <!-- 라우팅된 컴포넌트를 렌더링 -->
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  setup() {
    const router = useRouter()
    const route = useRoute()
    const selectedMenu = ref(route.path) // 현재 선택된 메뉴

    const navigateTo = (path) => {
      selectedMenu.value = path // 선택된 메뉴 경로 업데이트
      router.push(path)
    }

    const menuItems = [
      { name: '첫화면', path: '/' },
      { name: 'DB 전체 리스트', path: '/db-list' },
      { name: 'SMS 전송 내역', path: '/sms-history' },
      { name: '임계치 리스트', path: '/threshold-list' },
      { name: '테이블스페이스 리스트', path: '/tablespaces' },
      { name: '일일 점검(hit율)', path: '/dailyChk' }
    ]

    return { navigateTo, menuItems, selectedMenu }
  }

}
</script>

<style>
#app {
  height: 100%;
}

.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  background: #4CAF50; /* 상단 네비게이션 배경색 */
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

.main-content {
  margin-top: 60px; /* 상단 네비게이션 높이 만큼 여백 */
  padding: 20px;
}
</style>
