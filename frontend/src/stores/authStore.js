import { defineStore } from 'pinia';
import axios from 'axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null
  }),
  actions: {
    async login(username, password) {
      try {
        const response = await axios.post('/api/auth/login', { username, password });
        this.token = response.data.token;
        this.user = response.data.user;
        localStorage.setItem('token', this.token);
        return true;
      } catch (error) {
        console.error('로그인 오류', error);
        return false;
      }
    },
    logout() {
      this.user = null;
      this.token = null;
      localStorage.removeItem('token');
    }
  }
});
