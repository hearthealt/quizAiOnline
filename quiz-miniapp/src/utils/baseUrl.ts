const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").trim().replace(/\/+$/, "");
const UNI_PLATFORM = import.meta.env.UNI_PLATFORM || "";

export const requiresAbsoluteBaseUrl = UNI_PLATFORM.startsWith("mp-");

export const missingBaseUrlMessage =
  "小程序未配置 VITE_API_BASE_URL，请在 quiz-miniapp/.env.production 中填写服务器 HTTPS 域名根地址，例如 https://example.com";

let warnedMissingBaseUrl = false;

const isAbsoluteUrl = (url: string) => /^https?:\/\//i.test(url);

export const ensureApiBaseUrl = () => {
  if (!requiresAbsoluteBaseUrl || API_BASE_URL) {
    return { ok: true as const, baseUrl: API_BASE_URL };
  }

  if (!warnedMissingBaseUrl) {
    warnedMissingBaseUrl = true;
  }

  return { ok: false as const, message: missingBaseUrlMessage };
};

export const buildRemoteUrl = (path: string) => {
  if (!path) return API_BASE_URL;
  if (isAbsoluteUrl(path)) return path;
  if (path.startsWith("//")) return `https:${path}`;
  if (!API_BASE_URL) return path;
  if (path.startsWith("/")) return `${API_BASE_URL}${path}`;
  return `${API_BASE_URL}/${path}`;
};
