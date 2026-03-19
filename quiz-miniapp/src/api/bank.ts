import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";

export type BankItem = {
  id: number;
  categoryId?: number;
  name: string;
  description?: string;
  cover?: string;
  questionCount?: number;
  practiceCount?: number;
  examTime?: number;
  passScore?: number;
  userProgress?: number;
  userCorrectRate?: number;
  practiceTotalCount?: number;
};

export type QuestionOption = {
  label: string;
  content: string;
};

export type Question = {
  id: number;
  bankId: number;
  type: number;
  content: string;
  answer?: string;
  analysis?: string;
  difficulty?: number;
  options?: QuestionOption[];
};

export const getBankList = (categoryId?: number, pageNum = 1, pageSize = 10) =>
  request<PageResult<BankItem>>({
    url: "/api/app/bank/list",
    method: "GET",
    data: { categoryId, pageNum, pageSize }
  });

export const getBankDetail = (id: number) =>
  request<BankItem>({
    url: `/api/app/bank/${id}`,
    method: "GET"
  });

export const getBankQuestions = (id: number, pageNum = 1, pageSize = 10) =>
  request<PageResult<Question>>({
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
