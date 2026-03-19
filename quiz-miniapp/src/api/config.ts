import { request } from "@/utils/request";

export type AppConfig = Record<string, string>;

export const getAppConfig = () =>
  request<AppConfig>({
    url: "/api/app/config",
    method: "GET"
  });
