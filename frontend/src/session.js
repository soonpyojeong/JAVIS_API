export function restoreSessionFromLocalStorage(store) {
  const token = localStorage.getItem("accessToken");
  const userRaw = localStorage.getItem("user");
  if (token && userRaw && !store.state.isLoggedIn) {
    try {
      const user = JSON.parse(userRaw);
      store.commit("setUser", user);
      store.commit("setLoggedIn", true);
    } catch (e) {
      store.dispatch("logout");
    }
  }
}