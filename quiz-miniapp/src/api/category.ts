import { request } from "@/utils/request";

export type Category = {
  id: number;
  name: string;
  icon?: string;
};

export type PageResult<T> = {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export type BankItem = {
  id: number;
  name: string;
  description?: string;
  cover?: string;
  questionCount?: number;
  practiceCount?: number;
  examTime?: number;
  passScore?: number;
};

export const getCategories = () =>
  request<Category[]>({
    url: "/api/app/category/list",
    method: "GET"
  });

export const getBanksByCategory = (id: number, pageNum = 1, pageSize = 10) =>
  request<PageResult<BankItem>>({
    url: `/api/app/category/${id}/banks`,
    method: "GET",
    data: { pageNum, pageSize }
  });
