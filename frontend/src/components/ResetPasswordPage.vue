<template>
  <div class="reset-password-container">
    <div class="reset-card">
      <h2 class="reset-title">비밀번호 재설정</h2>
      <form @submit.prevent="submitReset" class="reset-form">
        <div class="input-group">
          <label for="new-password">새 비밀번호</label>
          <input
            v-model="newPassword"
            id="new-password"
            type="password"
            placeholder="새 비밀번호 입력"
            required
            class="input"
          />
        </div>
        <div class="input-group">
          <label for="confirm-password">비밀번호 확인</label>
          <input
            v-model="confirmPassword"
            id="confirm-password"
            type="password"
            placeholder="비밀번호 확인"
            required
            class="input"
          />
        </div>
        <button type="submit" class="btn-primary">변경하기</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api';

const route = useRoute();
const router = useRouter();

onMounted(() => {
  const token = route.query.token;
  if (!token) {
    alert('유효하지 않은 접근입니다.');
    router.replace('/login');
  }
});

const newPassword = ref("");
const confirmPassword = ref("");
const token = route.query.token;

const submitReset = async () => {
  if (newPassword.value !== confirmPassword.value) {
    alert("비밀번호가 일치하지 않습니다.");
    return;
  }
  try {
    const res = await api.post('/api/auth/password-reset/confirm', {
      token,
      newPassword: newPassword.value
    });
    if (res.data.success) {
      alert("비밀번호가 변경되었습니다. 다시 로그인하세요.");
      router.push("/login");
    } else {
      alert(res.data.message || "비밀번호 변경 실패");
    }
  } catch (err) {
    alert(err.response?.data?.message || "에러 발생");
  }
};
</script>

<style scoped>
.reset-password-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(120deg, #e0e7ff 0%, #fdf6f0 100%);
}
.reset-card {
  background: #fff;
  padding: 36px 32px 28px 32px;
  border-radius: 16px;
  box-shadow: 0 6px 32px rgba(80, 90, 130, 0.10);
  max-width: 380px;
  width: 100%;
  text-align: center;
}
.reset-title {
  margin-bottom: 28px;
  font-size: 26px;
  color: #304ffe;
  font-weight: bold;
  letter-spacing: -1px;
}
.reset-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.input-group {
  text-align: left;
}
.input-group label {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
  color: #2e3557;
  display: block;
}
.input {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  border: 1.2px solid #cfd8dc;
  border-radius: 8px;
  transition: border 0.2s;
  outline: none;
}
.input:focus {
  border-color: #536dfe;
}
.btn-primary {
  margin-top: 8px;
  width: 100%;
  padding: 13px 0;
  background: linear-gradient(90deg, #536dfe 0%, #82aaff 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 16px rgba(80, 90, 130, 0.09);
  transition: background 0.15s;
}
.btn-primary:hover {
  background: linear-gradient(90deg, #304ffe 0%, #5c7cfa 100%);
}
</style>
