import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";

export type WrongItem = {
  id: number;
  questionId: number;
  bankId: number;
  bankName: string;
  content: string;
  type: number;
  answer?: string;
  analysis?: string;
  options?: Array<{
    label: string;
    content: string;
  }>;
  wrongCount: number;
  lastWrongAnswer: string;
  createTime: string;
};

export type WrongStatsItem = {
  bankId: number;
  bankName: string;
  count: number;
};

export type WrongStats = {
  totalCount: number;
  bankStats: WrongStatsItem[];
};

export const getWrongList = (bankId?: number, pageNum = 1, pageSize = 10) =>
  request<PageResult<WrongItem>>({
    url: "/api/app/wrong/list",
    method: "GET",
    data: {
      ...(typeof bankId === "number" ? { bankId } : {}),
      pageNum,
      pageSize
    }
  });

export const getWrongStats = () =>
  request<WrongStats>({
    url: "/api/app/wrong/stats",
    method: "GET"
  });

export const removeWrong = (id: number) =>
  request<void>({
    url: `/api/app/wrong/${id}`,
    method: "DELETE"
  });
