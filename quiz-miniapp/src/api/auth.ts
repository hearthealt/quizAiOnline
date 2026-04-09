import { request } from "@/utils/request";

export type LoginVO = {
  token: string;
  userInfo: UserInfo;
  newUser: boolean;
};

export type UserInfo = {
  id: number;
  nickname: string;
  avatar: string;
  phone: string;
  isVip: number;
  vipExpireTime?: string;
};

export const wxLogin = (code: string, nickname?: string, avatar?: string) =>
  request<LoginVO>({
    url: "/api/app/auth/wx-login",
    method: "POST",
    data: { code, nickname, avatar }
  });

export const phoneLogin = (phone: string, password: string) =>
  request<LoginVO>({
    url: "/api/app/auth/phone-login",
    method: "POST",
    data: { phone, password }
  });

export const logout = (silent = false) =>
  request<void>({
    url: "/api/app/auth/logout",
    method: "POST",
    silent
  });

export const getInfo = () =>
  request<UserInfo>({
    url: "/api/app/auth/info",
    method: "GET"
  });
