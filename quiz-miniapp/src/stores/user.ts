import { defineStore } from "pinia";
import { storage } from "@/utils/storage";
import type { UserInfo } from "@/api/auth";
import { getInfo } from "@/api/auth";

const TOKEN_KEY = "quiz_token";
const USER_KEY = "quiz_user";
let pendingLoginAction: null | (() => void | Promise<void>) = null;
let refreshUserPromise: null | Promise<UserInfo | null> = null;
let lastRefreshAt = 0;

export const useUserStore = defineStore("user", {
  state: () => ({
    token: storage.get<string>(TOKEN_KEY, "") || "",
    userInfo: storage.get<UserInfo | null>(USER_KEY, null),
    loginSheetVisible: false,
    loginSheetMessage: ""
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
    async refreshUser(force = false) {
      if (!this.token) return null;
      const now = Date.now();
      if (!force && refreshUserPromise) {
        return refreshUserPromise;
      }
      if (!force && this.userInfo && now - lastRefreshAt < 3000) {
        return this.userInfo;
      }
      refreshUserPromise = getInfo()
        .then((info) => {
          this.setUserInfo(info);
          lastRefreshAt = Date.now();
          return info;
        })
        .finally(() => {
          refreshUserPromise = null;
        });
      return refreshUserPromise;
    },
    openLoginSheet(message = "") {
      this.loginSheetMessage = message;
      this.loginSheetVisible = true;
    },
    requestLogin(action?: null | (() => void | Promise<void>), message = "请先登录") {
      pendingLoginAction = action || null;
      this.openLoginSheet(message);
    },
    closeLoginSheet() {
      this.loginSheetVisible = false;
      this.loginSheetMessage = "";
    },
    async runPendingLoginAction() {
      const action = pendingLoginAction;
      pendingLoginAction = null;
      if (action) {
        await action();
      }
    },
    handleSessionExpired(message = "登录已过期，请重新登录") {
      pendingLoginAction = null;
      this.logout();
      this.openLoginSheet(message);
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      lastRefreshAt = 0;
      refreshUserPromise = null;
      storage.remove(TOKEN_KEY);
      storage.remove(USER_KEY);
    }
  }
});
