// src/store.js
import { createStore } from 'vuex';

const store = createStore({
  state() {
    return {
      isLoggedIn: !!localStorage.getItem('accessToken'),
    };
  },
  mutations: {
    setLoginState(state, status) {
      state.isLoggedIn = status;
    },
  },
  actions: {
    login({ commit }, token) {
      localStorage.setItem('accessToken', token);
      commit('setLoginState', true);
    },
    logout({ commit }) {
      localStorage.removeItem('accessToken');
      commit('setLoginState', false);
    },
  },
});

export default store;
