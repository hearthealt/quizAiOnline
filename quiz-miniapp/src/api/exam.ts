import { request } from "@/utils/request";

export type ExamQuestionOption = {
  label: string;
  content: string;
};

export type ExamQuestion = {
  id: number;
  type: number;
  content: string;
  options: ExamQuestionOption[];
};

export type StartExamResult = {
  examId: number;
  totalCount: number;
  examTime: number;
  leftSeconds?: number;
  questions: ExamQuestion[];
  answers?: ExamAnswerTemp[];
};

export type ExamAnswerTemp = {
  questionId: number;
  answer: string;
};

export type ExamOngoing = {
  exists: boolean;
  expired: boolean;
  examId?: number;
  leftSeconds?: number;
};

export type SubmitExamAnswer = {
  questionId: number;
  answer: string;
};

export type ExamAnswerDetail = {
  questionId: number;
  content: string;
  userAnswer: string;
  correctAnswer: string;
  analysis?: string;
  isCorrect: boolean;
};

export type ExamResult = {
  examId: number;
  score: number;
  totalCount: number;
  correctCount: number;
  correctRate: number;
  duration: number;
  passScore: number;
  isPass: boolean;
  details: ExamAnswerDetail[];
};

export type RecordItem = {
  id: number;
  bankName: string;
  mode: string;
  totalCount: number;
  correctCount: number;
  correctRate: number;
  createTime: string;
  type: string;
};

export type PageResult<T> = {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
};

export const startExam = (bankId: number, restart?: boolean) =>
  request<StartExamResult>({
    url: "/api/app/exam/start",
    method: "POST",
    data: { bankId, restart }
  });

export const getExamOngoing = (bankId: number) =>
  request<ExamOngoing>({
    url: "/api/app/exam/ongoing",
    method: "GET",
    data: { bankId }
  });

export const getExamSession = (examId: number) =>
  request<StartExamResult>({
    url: `/api/app/exam/${examId}/session`,
    method: "GET"
  });

export const saveExamAnswer = (examId: number, questionId: number, answer: string) =>
  request<boolean>({
    url: `/api/app/exam/${examId}/answer`,
    method: "POST",
    data: { questionId, answer }
  });

export const submitExam = (examId: number, answers: SubmitExamAnswer[]) =>
  request<ExamResult>({
    url: `/api/app/exam/${examId}/submit`,
    method: "POST",
    data: { answers }
  });

export const getExamResult = (examId: number) =>
  request<ExamResult>({
    url: `/api/app/exam/${examId}/result`,
    method: "GET"
  });

export const getExamRecords = (pageNum = 1, pageSize = 10) =>
  request<PageResult<RecordItem>>({
    url: "/api/app/exam/records",
    method: "GET",
    data: { pageNum, pageSize }
  });
