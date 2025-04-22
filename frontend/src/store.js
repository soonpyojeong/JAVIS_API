import { createStore } from 'vuex';

function loadUserFromStorage() {
  const userRaw = localStorage.getItem("user");
  if (!userRaw || userRaw === "undefined") {
    return null;
  }

  try {
    return JSON.parse(userRaw);
  } catch (e) {
    console.warn("⛔ user JSON 파싱 실패", e);
    return null;
  }
}

const store = createStore({
  state() {
    return {
      isLoggedIn: !!localStorage.getItem("accessToken"),
      user: loadUserFromStorage() || {}, // ✅ null 대신 {}로 기본값 설정
    };
  },
  mutations: {
    setUser(state, user) {
      console.log("🧩 Vuex setUser 호출됨:", user); // ✅ 추가
      state.user = user;
    },
    setLoggedIn(state, status) {
      state.isLoggedIn = status;
    },
  },
  actions: {
    login({ commit }, payload) {
      const { user, accessToken, refreshToken } = payload;
      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
      localStorage.setItem("user", JSON.stringify(user));
      commit("setUser", user);
      commit("setLoggedIn", true);
    },
    logout({ commit }) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      commit("setUser", {}); // ✅ 빈 객체로 초기화
      commit("setLoggedIn", false);
    },
  },
});

export default store;
