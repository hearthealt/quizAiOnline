import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";
import type { QuestionVO } from "@/api/home";

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

export type RecordDetailItem = {
  questionId: number;
  content?: string;
  userAnswer?: string;
  correctAnswer?: string;
  analysis?: string;
  isCorrect?: boolean | number;
  answerTime?: number;
  question?: QuestionVO;
};

export type RecordDetailRecord = {
  id: number;
  totalCount?: number;
  answerCount?: number;
  correctCount?: number;
  score?: number;
  duration?: number;
  passScore?: number;
  isPass?: number;
  createTime?: string;
};

export type RecordDetail = {
  record: RecordDetailRecord;
  details: RecordDetailItem[];
};

export const getRecordList = (type?: string, pageNum = 1, pageSize = 10) =>
  request<PageResult<RecordItem>>({
    url: "/api/app/record/list",
    method: "GET",
    data: { type, pageNum, pageSize }
  });

export const getRecordDetail = (id: number, type: string) =>
  request<RecordDetail>({
    url: `/api/app/record/${id}/detail`,
    method: "GET",
    data: { type }
  });
