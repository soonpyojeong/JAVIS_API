// Login.vue
<template>
  <div>
    <form @submit.prevent="handleLogin">
      <input v-model="username" placeholder="Username" />
      <input v-model="password" type="password" placeholder="Password" />
      <button type="submit">로그인</button>
    </form>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'vue-router'

export default {
  name: 'UserLogin',
  setup() {
    const username = ref('')
    const password = ref('')
    const authStore = useAuthStore()
    const router = useRouter()  // router 객체를 가져옵니다.

    const handleLogin = async () => {
      const success = await authStore.login(username.value, password.value)
      if (success) {
        router.push('/main')  // 로그인 성공 시 메인 페이지로 이동
      } else {
        alert('로그인 실패')
      }
    }

    return { username, password, handleLogin }
  }
}
</script>
