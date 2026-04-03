import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";
import type { QuestionListVO } from "@/api/home";

export type BankItem = {
  id: number;
  categoryId?: number;
  name: string;
  description?: string;
  cover?: string;
  questionCount?: number;
  practiceCount?: number;
  passScore?: number;
  userProgress?: number;
  userCorrectRate?: number;
  practiceTotalCount?: number;
};

export type QuestionOption = {
  label: string;
  content: string;
};

export const getBankList = (categoryId?: number, pageNum = 1, pageSize = 10) =>
  request<PageResult<BankItem>>({
    url: "/api/app/bank/list",
    method: "GET",
    data: categoryId == null ? { pageNum, pageSize } : { categoryId, pageNum, pageSize }
  });

export const getBankDetail = (id: number) =>
  request<BankItem>({
    url: `/api/app/bank/${id}`,
    method: "GET"
  });

export const getBankQuestions = (id: number, pageNum = 1, pageSize = 10) =>
  request<PageResult<QuestionListVO>>({
    url: `/api/app/bank/${id}/questions`,
    method: "GET",
    data: { pageNum, pageSize }
  });

export const getHotBanks = (limit = 10) =>
  request<BankItem[]>({
    url: "/api/app/bank/hot",
    method: "GET",
    data: { limit }
  });
