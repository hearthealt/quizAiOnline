import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";

export type FavoriteItem = {
  id: number;
  questionId: number;
  content: string;
  type: number;
  bankName: string;
  answer?: string;
  analysis?: string;
  options?: Array<{
    label: string;
    content: string;
  }>;
  createTime: string;
};

export const toggleFavorite = (questionId: number) =>
  request<boolean>({
    url: `/api/app/favorite/toggle?questionId=${questionId}`,
    method: "POST",
    data: {}
  });

export const getFavoriteList = (pageNum = 1, pageSize = 10) =>
  request<PageResult<FavoriteItem>>({
    url: "/api/app/favorite/list",
    method: "GET",
    data: { pageNum, pageSize }
  });

export const checkFavorite = (questionId: number) =>
  request<boolean>({
    url: `/api/app/favorite/check/${questionId}`,
    method: "GET"
  });

export const batchRemoveFavorite = (questionIds: number[]) =>
  request<void>({
    url: "/api/app/favorite/batch",
    method: "DELETE",
    data: { questionIds }
  });
