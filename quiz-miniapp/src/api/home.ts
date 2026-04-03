import { request } from "@/utils/request";

export type CategoryItem = {
  id: number;
  name: string;
  icon?: string;
  bankCount?: number;
};

export type BankSimple = {
  id: number;
  name: string;
  cover?: string;
  questionCount?: number;
  practiceCount?: number;
};

export type StudyStats = {
  totalDays: number;
  totalAnswered: number;
  correctRate: number;
  todayAnswered: number;
};

export type QuestionOption = {
  label: string;
  content: string;
};

export type QuestionListVO = {
  id: number;
  bankId: number;
  type: number;
  content: string;
  difficulty?: number;
};

export type QuestionVO = {
  id: number;
  bankId: number;
  type: number;
  content: string;
  answer?: string;
  analysis?: string;
  difficulty?: number;
  options?: QuestionOption[];
  isFavorite?: boolean;
  userAnswer?: string;
  isCorrect?: number;
};

export type HomeVO = {
  categories: CategoryItem[];
  hotBanks: BankSimple[];
  dailyQuestion?: QuestionListVO | null;
  studyStats?: StudyStats;
};

export const getHomeIndex = () =>
  request<HomeVO>({
    url: "/api/app/home/index",
    method: "GET"
  });

export const getDailyQuestion = () =>
  request<QuestionListVO>({
    url: "/api/app/home/daily-question",
    method: "GET"
  });
