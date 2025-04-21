import { createStore } from 'vuex';

function loadUserFromStorage() {
  try {
    return JSON.parse(localStorage.getItem("user"));
  } catch (e) {
    console.warn("⛔ user JSON 파싱 실패", e);
    return null;
  }
}

const store = createStore({
  state() {
    return {
      isLoggedIn: !!localStorage.getItem("accessToken"),
      user: loadUserFromStorage(),
    };
  },
  mutations: {
    setUser(state, user) {
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
      commit("setUser", null);
      commit("setLoggedIn", false);
    },
  },
});

export default store;
