import { buildRemoteUrl, ensureApiBaseUrl } from "@/utils/baseUrl";

export const normalizeAvatarPath = (path?: string) => {
  return path?.trim() || "";
};

export const resolveAssetUrl = (path?: string) => {
  const value = normalizeAvatarPath(path);
  if (!value) return "";
  if (/^https?:\/\//i.test(value)) return value;
  if (value.startsWith("//")) return `https:${value}`;
  ensureApiBaseUrl();
  return buildRemoteUrl(value);
};
