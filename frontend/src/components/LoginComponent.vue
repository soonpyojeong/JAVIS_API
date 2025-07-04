<template>
  <div class="auth-container">
    <div v-if="!isRegisterPage" class="form-card">
      <h2>로그인</h2>
      <form @submit.prevent="login">
        <div class="input-group">
          <label for="loginId">사용자ID</label>
          <input v-model="loginForm.loginId" id="loginId" type="text" placeholder="사용자ID을 입력하세요" required />
        </div>
        <div class="input-group">
          <label for="password">비밀번호</label>
          <input v-model="loginForm.password" id="password" type="password" placeholder="비밀번호를 입력하세요" required />
        </div>
        <button type="submit" class="btn-primary">로그인</button>
        <div class="switch-page">
          <button type="button" @click="navigateTo('register')" class="btn-secondary">회원가입</button>
        </div>
      </form>
    </div>

    <div v-if="isRegisterPage" class="form-card">
      <h2>회원가입</h2>
      <form @submit.prevent="register">
        <div class="input-group">
          <label for="register-loginid">사용자ID</label>
          <input v-model="registerForm.loginId" id="register-loginId" type="text" placeholder="사용자ID 을 입력하세요" required />
        </div>
        <div class="input-group">
          <label for="register-username">사용자명</label>
          <input v-model="registerForm.username" id="register-username" type="text" placeholder="사용자명 을 입력하세요" required />
        </div>
        <div class="input-group">
          <label for="register-email">이메일</label>
          <input v-model="registerForm.email" id="register-email" type="email" placeholder="이메일을 입력하세요" required />
        </div>
        <div class="input-group">
          <label for="register-user_role">권한(DBA권한은 관리자에게 문의 하세요)</label>
          <input readonly  v-model="registerForm.userRole" id="register-userRole" type="text"  required />
        </div>
        <div class="input-group">
          <label for="register-password">비밀 번호</label>
          <input v-model="registerForm.password" id="register-password" type="password" placeholder="비밀번호를 입력하세요" required />
        </div>
        <div class="input-group">
          <label for="confirm-password">비밀 번호 확인</label>
          <input v-model="registerForm.confirmPassword" id="confirm-password" type="password" placeholder="비밀번호를 확인하세요" required />
        </div>
        <button type="submit" class="btn-primary">회원가입</button>
        <div class="switch-page">
          <button type="button" @click="navigateTo('login')" class="btn-secondary">로그인 화면으로</button>
        </div>
      </form>
    </div>
  </div>
</template>
<script>
import { useStore } from "vuex";
import { ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/api";

export default {
  setup() {
    const store = useStore();
    const router = useRouter();

    const isRegisterPage = ref(false);
    const loginForm = ref({
      loginId: "",
      password: "",
    });

    const registerForm = ref({
      loginId: "",
      username: "",
      email: "",
      userRole: "VIEW",
      password: "",
      confirmPassword: "",
    });

    const navigateTo = (page) => {
      isRegisterPage.value = page === "register";
    };

    const login = async () => {
      try {
        const response = await api.post("/api/auth/login", loginForm.value);
        const { user, accessToken, refreshToken, menuAuthList } = response.data;

        // 1. Vuex에 menuAuthList 저장 (mutation/action 만들어놨을 것)
        store.commit("setMenuAuthList", menuAuthList);
        // 만약 store.dispatch("login", ...)에서 menuAuthList도 저장하도록 되어 있다면 아래 코드 필요 없음
        // store.dispatch("login", { user, accessToken, refreshToken, menuAuthList });

        // 2. localStorage에도 저장(새로고침 후에도 권한 정보 유지!)
        localStorage.setItem("menuAuthList", JSON.stringify(menuAuthList));

        // 3. 기존과 동일하게 나머지 처리
        store.dispatch("login", { user, accessToken, refreshToken, menuAuthList });
        localStorage.setItem("accessToken", accessToken);
        localStorage.setItem("refreshToken", refreshToken);
        localStorage.setItem("user", JSON.stringify(user));

        // 3. 홈으로 이동
        router.push("/");
        window.location.href = "/";
      } catch (error) {
        alert(error.response?.data?.message || "로그인 중 문제가 발생했습니다.");
      }
    };

    const register = async () => {
      if (registerForm.value.password !== registerForm.value.confirmPassword) {
        alert("비밀번호가 일치하지 않습니다.");
        return;
      }

      try {
        const response = await api.post("/api/auth/register", registerForm.value);
        if (response.data.success) {
          alert("회원가입 성공! 로그인 해주세요.");
          navigateTo("login");
        } else {
          alert("회원가입 실패: " + response.data.message);
        }
      } catch (error) {
        alert(error.response?.data?.message || "회원가입 중 문제가 발생했습니다.");
      }
    };

    return {
      isRegisterPage,
      loginForm,
      registerForm,
      navigateTo,
      login,
      register,
    };
  },
};
</script>



<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f4f7fa;
}

.form-card {
  background-color: white;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  text-align: center;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.input-group {
  margin-bottom: 20px;
  text-align: left;
}

.input-group label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.input-group input {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 5px;
  box-sizing: border-box;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  width: 100%;
  padding: 12px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.switch-page {
  margin-top: 15px;
}

.switch-page button {
  background: none;
  border: none;
  color: #007bff;
  font-size: 14px;
  cursor: pointer;
}

.switch-page button:hover {
  text-decoration: underline;
}
</style>
