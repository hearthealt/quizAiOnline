import { request } from "@/utils/request";
import type { QuestionVO } from "@/api/home";

export type PracticeRecord = {
  id: number;
  bankId: number;
  mode: string;
  totalCount: number;
  answerCount: number;
  correctCount: number;
  status: number;
  lastIndex: number;
};

export type PracticeResult = {
  recordId: number;
  totalCount: number;
  answerCount: number;
  correctCount: number;
  correctRate: number;
  duration: number;
};

export type PracticeProgress = {
  answerCount: number;
  totalCount: number;
  correctCount: number;
  lastIndex?: number;
};

export const startPractice = (bankId: number, mode: string, count?: number, restart?: boolean) =>
  request<PracticeRecord>({
    url: "/api/app/practice/start",
    method: "POST",
    data: { bankId, mode, count, restart }
  });

export const getPracticeQuestion = (recordId: number, index: number) =>
  request<QuestionVO>({
    url: `/api/app/practice/${recordId}/question`,
    method: "GET",
    data: { index }
  });

export const submitPracticeAnswer = (
  recordId: number,
  questionId: number,
  answer: string,
  answerTime?: number
) =>
  request<boolean>({
    url: `/api/app/practice/${recordId}/submit`,
    method: "POST",
    data: { questionId, answer, answerTime }
  });

export const finishPractice = (recordId: number) =>
  request<PracticeResult>({
    url: `/api/app/practice/${recordId}/finish`,
    method: "POST"
  });

export const getPracticeProgress = (recordId: number) =>
  request<PracticeProgress>({
    url: `/api/app/practice/${recordId}/progress`,
    method: "GET"
  });
