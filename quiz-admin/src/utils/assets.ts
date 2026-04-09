const ADMIN_API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "")
  .trim()
  .replace(/\/+$/, "");

const MANAGED_ASSET_PATH_RE = /^\/?(uploads|static)\//i;
const ABSOLUTE_URL_RE = /^https?:\/\//i;
const SPECIAL_ASSET_URL_RE = /^(data:|blob:)/i;

const isManagedAssetPath = (path: string) => MANAGED_ASSET_PATH_RE.test(path);

const normalizeManagedAssetPath = (path: string) => {
  const value = path.trim();
  if (!value || SPECIAL_ASSET_URL_RE.test(value)) return value;

  if (value.startsWith("//")) {
    try {
      const url = new URL(`https:${value}`);
      if (isManagedAssetPath(url.pathname)) {
        return `${url.pathname}${url.search}${url.hash}`;
      }
    } catch {}
    return `https:${value}`;
  }

  if (ABSOLUTE_URL_RE.test(value)) {
    try {
      const url = new URL(value);
      if (isManagedAssetPath(url.pathname)) {
        return `${url.pathname}${url.search}${url.hash}`;
      }
    } catch {}
    return value;
  }

  if (isManagedAssetPath(value) && !value.startsWith("/")) {
    return `/${value}`;
  }

  return value;
};

const buildRemoteUrl = (path: string) => {
  if (!path) return "";
  if (SPECIAL_ASSET_URL_RE.test(path) || ABSOLUTE_URL_RE.test(path)) return path;
  const runtimeBaseUrl =
    ADMIN_API_BASE_URL || (typeof window !== "undefined" ? window.location.origin.replace(/\/+$/, "") : "");
  if (!runtimeBaseUrl) return path;
  return path.startsWith("/") ? `${runtimeBaseUrl}${path}` : `${runtimeBaseUrl}/${path}`;
};

export const resolveAssetUrl = (path?: string) => {
  if (!path) return "";
  return buildRemoteUrl(normalizeManagedAssetPath(path));
};
