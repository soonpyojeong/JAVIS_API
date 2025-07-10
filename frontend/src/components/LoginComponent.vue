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
          <button type="button" class="btn-link" @click="showResetModal = true">비밀번호 재설정</button>

        </div>
      </form>
    </div>
        <!-- 🔽🔽 모달 팝업 구조 추가 -->
        <div v-if="showResetModal" class="modal-overlay">
          <div class="modal-content">
            <h2>비밀번호 재설정</h2>
            <form @submit.prevent="resetPassword">
              <div class="input-group">
                <label for="reset-email">이메일</label>
                <input v-model="resetEmail" id="reset-email" type="email" placeholder="가입 시 등록한 이메일을 입력하세요" required />
              </div>
              <button type="submit" class="btn-primary" style="width:100%;">재설정 링크 전송</button>
              <button type="button" class="btn-secondary" style="margin-top:10px;width:100%;" @click="showResetModal = false">닫기</button>
            </form>
          </div>
        </div>
        <!-- 🔼🔼 모달 팝업 구조 추가 끝 -->

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

<script setup>
import { useStore } from "vuex";
import { ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/api";

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

const showResetModal = ref(false);  // 모달 상태
const resetEmail = ref("");

const navigateTo = (page) => {
  if (page === "register") {
    isRegisterPage.value = true;
  } else {
    isRegisterPage.value = false;
  }
};

const resetPassword = async () => {
  try {
    console.log("[resetPassword] 요청 시작, 입력 이메일:", resetEmail.value);

    const response = await api.post("/api/auth/password-reset/request", { email: resetEmail.value });
    console.log("[resetPassword] 서버 응답:", response);

    if (response.data.success) {
      console.log("[resetPassword] 비밀번호 재설정 성공: 이메일로 링크 전송 완료");
      alert("비밀번호 재설정 링크가 이메일로 전송되었습니다.");
      showResetModal.value = false;  // 전송 후 모달 닫기
    } else {
      console.warn("[resetPassword] 비밀번호 재설정 요청 실패:", response.data);
      alert("비밀번호 재설정 요청 실패");
    }
  } catch (error) {
    // axios의 에러 응답 객체를 더 자세히 콘솔에 남김
    if (error.response) {
      console.error("[resetPassword] 서버 응답 에러:", error.response);
      alert(error.response?.data?.message || "비밀번호 재설정 요청 중 문제가 발생했습니다.");
    } else if (error.request) {
      console.error("[resetPassword] 서버에 요청 자체가 가지 않음:", error.request);
      alert("네트워크 또는 서버 연결 문제로 요청이 실패했습니다.");
    } else {
      console.error("[resetPassword] 예기치 않은 에러:", error.message);
      alert("예상치 못한 오류가 발생했습니다.");
    }
  }
};


const openResetModal = () => {
  resetEmail.value = "";
  showResetModal.value = true;
};

const login = async () => {
  try {
    const response = await api.post("/api/auth/login", loginForm.value);
    const { user, accessToken, refreshToken, menuAuthList } = response.data;

    store.commit("setMenuAuthList", menuAuthList);
    localStorage.setItem("menuAuthList", JSON.stringify(menuAuthList));
    store.dispatch("login", { user, accessToken, refreshToken, menuAuthList });
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    localStorage.setItem("user", JSON.stringify(user));

    router.push("/");
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

.btn-link {
  background: none;
  border: none;
  color: #007bff;
  font-size: 14px;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}
.btn-link:hover {
  color: #0056b3;
}

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}
.modal-content {
  background: white;
  border-radius: 10px;
  padding: 30px 30px 20px 30px;
  box-shadow: 0 4px 32px rgba(0,0,0,0.15);
  width: 350px;
  text-align: center;
}
</style>
