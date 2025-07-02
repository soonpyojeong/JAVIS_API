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

function loadMenuAuthListFromStorage() {
  const raw = localStorage.getItem("menuAuthList");
  if (!raw || raw === "undefined") {
    return [];
  }
  try {
    return JSON.parse(raw);
  } catch (e) {
    console.warn("⛔ menuAuthList JSON 파싱 실패", e);
    return [];
  }
}

const store = createStore({
  state() {
    return {
      isLoggedIn: !!localStorage.getItem("accessToken"),
      user: loadUserFromStorage() || {},
      menuAuthList: loadMenuAuthListFromStorage(), // ✅ 추가!
    };
  },
  mutations: {
    setUser(state, user) {
      state.user = user;
    },
    setLoggedIn(state, status) {
      state.isLoggedIn = status;
    },
    setMenuAuthList(state, menuAuthList) { // ✅ 추가!
      state.menuAuthList = menuAuthList;
      localStorage.setItem("menuAuthList", JSON.stringify(menuAuthList));
    },
  },
  actions: {
    login({ commit }, payload) {
      // payload에서 menuAuthList도 받아야 함!
      const { user, accessToken, refreshToken, menuAuthList } = payload;
      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
      localStorage.setItem("user", JSON.stringify(user));
      localStorage.setItem("menuAuthList", JSON.stringify(menuAuthList)); // ✅ 추가!
      commit("setUser", user);
      commit("setLoggedIn", true);
      commit("setMenuAuthList", menuAuthList); // ✅ 추가!
    },
    logout({ commit }) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      localStorage.removeItem("menuAuthList"); // ✅ 추가!
      commit("setUser", {});
      commit("setLoggedIn", false);
      commit("setMenuAuthList", []); // ✅ 추가!
    },
  },
});

export default store;
