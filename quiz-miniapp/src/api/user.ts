import { request } from "@/utils/request";

export type UserProfile = {
  id: number;
  nickname: string;
  avatar: string;
  phone: string;
  isVip: number;
  vipExpireTime?: string;
};

export type StudyStats = {
  totalDays: number;
  totalAnswered: number;
  correctRate: number;
  todayAnswered: number;
};

export const getProfile = () =>
  request<UserProfile>({
    url: "/api/app/user/profile",
    method: "GET"
  });

export const updateProfile = (nickname?: string, avatar?: string, phone?: string) =>
  request<void>({
    url: "/api/app/user/profile",
    method: "PUT",
    data: { nickname, avatar, phone }
  });

export const getStudyStats = () =>
  request<StudyStats>({
    url: "/api/app/user/study-stats",
    method: "GET"
  });

export const updateSettings = (settings: Record<string, any>) =>
  request<void>({
    url: "/api/app/user/settings",
    method: "PUT",
    data: JSON.stringify(settings)
  });
