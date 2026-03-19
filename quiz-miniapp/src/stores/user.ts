import { defineStore } from "pinia";
import { storage } from "@/utils/storage";
import type { UserInfo } from "@/api/auth";
import { getInfo } from "@/api/auth";

const TOKEN_KEY = "quiz_token";
const USER_KEY = "quiz_user";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: storage.get<string>(TOKEN_KEY, "") || "",
    userInfo: storage.get<UserInfo | null>(USER_KEY, null)
  }),
  getters: {
    isLogin: (state) => !!state.token
  },
  actions: {
    setToken(token: string) {
      this.token = token;
      storage.set(TOKEN_KEY, token);
    },
    setUserInfo(user: UserInfo | null) {
      this.userInfo = user;
      storage.set(USER_KEY, user);
    },
    async refreshUser() {
      if (!this.token) return;
      const info = await getInfo();
      this.setUserInfo(info);
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      storage.remove(TOKEN_KEY);
      storage.remove(USER_KEY);
    }
  }
});
