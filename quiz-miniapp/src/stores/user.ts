import { defineStore } from "pinia";
import { storage } from "@/utils/storage";
import type { UserInfo } from "@/api/auth";
import { getInfo } from "@/api/auth";
import { normalizeAvatarPath } from "@/utils/assets";

const TOKEN_KEY = "quiz_token";
const USER_KEY = "quiz_user";
let pendingLoginAction: null | (() => void | Promise<void>) = null;
let refreshUserPromise: null | Promise<UserInfo | null> = null;
let lastRefreshAt = 0;

const sanitizeUserInfo = (user?: UserInfo | null) => {
  if (!user) return null;
  return {
    ...user,
    avatar: normalizeAvatarPath(user.avatar)
  };
};

export const useUserStore = defineStore("user", {
  state: () => ({
    token: storage.get<string>(TOKEN_KEY, "") || "",
    userInfo: sanitizeUserInfo(storage.get<UserInfo | null>(USER_KEY, null))
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
      const nextUser = sanitizeUserInfo(user);
      this.userInfo = nextUser;
      storage.set(USER_KEY, nextUser);
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
    requestLogin(action?: null | (() => void | Promise<void>), _message = "请先登录") {
      pendingLoginAction = action || null;
    },
    cancelPendingLoginAction() {
      pendingLoginAction = null;
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
