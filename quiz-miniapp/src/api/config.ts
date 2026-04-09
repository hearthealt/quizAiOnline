import { request } from "@/utils/request";

export type AppConfig = Record<string, string>;

let appConfigPromise: Promise<AppConfig> | null = null;

export const getAppConfig = () => {
  if (appConfigPromise) {
    return appConfigPromise;
  }

  appConfigPromise = request<AppConfig>({
    url: "/api/app/config",
    method: "GET",
    timeout: 15000,
    retry: 1,
    retryDelay: 800
  }).finally(() => {
    appConfigPromise = null;
  });

  return appConfigPromise;
};
