import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";
import type { QuestionListVO } from "@/api/home";

export const searchQuestions = (keyword: string, pageNum = 1, pageSize = 10) =>
  request<PageResult<QuestionListVO>>({
    url: "/api/app/search",
    method: "GET",
    data: { keyword, pageNum, pageSize }
  });

export const getHotKeywords = () =>
  request<string[]>({
    url: "/api/app/search/hot",
    method: "GET"
  });
