import { request } from "@/utils/request";

export type ChatMessage = {
  role: "user" | "assistant";
  content: string;
};

export type AiChatVO = {
  reply: string;
};

export type AiChatHistory = {
  id: number;
  userId: number;
  role: "user" | "assistant";
  content: string;
  createTime: string;
};

export const aiChat = (message: string, history: ChatMessage[] = []) =>
  request<AiChatVO>({
    url: "/api/app/ai/chat",
    method: "POST",
    data: { message, history },
    timeout: 60000
  });

export const getAiHistory = () =>
  request<AiChatHistory[]>({
    url: "/api/app/ai/history",
    method: "GET"
  });

export const clearAiHistory = () =>
  request<void>({
    url: "/api/app/ai/history",
    method: "DELETE"
  });
