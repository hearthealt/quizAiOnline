import { useUserStore } from "@/stores/user";
import { buildRemoteUrl, ensureApiBaseUrl, missingBaseUrlMessage } from "@/utils/baseUrl";
import { traceRuntime } from "@/utils/runtimeTrace";

type ApiResponse<T> = {
  code: number;
  message?: string;
  msg: string;
  data: T;
};

interface RequestConfig extends UniApp.RequestOptions {
  /** 请求超时时间，默认20秒 */
  timeout?: number;
  /** 重试次数，默认1次 */
  retry?: number;
  /** 重试延迟，默认1秒 */
  retryDelay?: number;
  /** 是否显示loading */
  showLoading?: boolean;
  /** loading文案 */
  loadingText?: string;
  /** 是否静默（不显示错误提示） */
  silent?: boolean;
}

const DEFAULT_CONFIG: Partial<RequestConfig> = {
  timeout: 20000,
  retry: 0,
  retryDelay: 1000,
  showLoading: false,
  silent: false
};

const getRequestLabel = (url: string, method?: string) => {
  try {
    const parsed = /^https?:\/\//i.test(url) ? new URL(url) : null;
    const path = parsed ? `${parsed.pathname}${parsed.search}` : url;
    return `${(method || "GET").toUpperCase()} ${path}`;
  } catch {
    return `${(method || "GET").toUpperCase()} ${url}`;
  }
};

const isSafeRetryMethod = (method?: string) => {
  const normalized = (method || "GET").toUpperCase();
  return normalized === "GET" || normalized === "HEAD";
};

const sanitizeData = (value: any): any => {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  if (Array.isArray(value)) {
    return value
      .map((item) => sanitizeData(item))
      .filter((item) => item !== undefined);
  }
  if (typeof value === "object") {
    const result: Record<string, any> = {};
    Object.keys(value).forEach((key) => {
      const next = sanitizeData(value[key]);
      if (next !== undefined) {
        result[key] = next;
      }
    });
    return result;
  }
  return value;
};

// 请求队列，用于取消重复请求
const pendingRequests = new Map<string, UniApp.RequestTask>();

const stableSerialize = (value: any): string => {
  if (value === undefined) return "undefined";
  if (value === null) return "null";
  if (Array.isArray(value)) {
    return `[${value.map((item) => stableSerialize(item)).join(",")}]`;
  }
  if (typeof value === "object") {
    const keys = Object.keys(value).sort();
    return `{${keys.map((key) => `${key}:${stableSerialize(value[key])}`).join(",")}}`;
  }
  return String(value);
};

// 生成请求唯一标识
const generateRequestKey = (options: RequestConfig): string => {
  return `${options.method || "GET"}:${options.url}:${stableSerialize(options.data)}`;
};

// 生成请求ID用于追踪
const generateRequestId = (): string => {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

// 处理未授权
let unauthorizedHandled = false;

const handleUnauthorized = (message = "请先登录") => {
  if (unauthorizedHandled) return;
  unauthorizedHandled = true;
  const userStore = useUserStore();
  userStore.handleSessionExpired(message);
  uni.showToast({ title: message, icon: "none" });
  setTimeout(() => {
    unauthorizedHandled = false;
  }, 800);
};

// 自定义错误类
export class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
    this.name = "ApiError";
  }
}

export const request = <T = any>(options: RequestConfig): Promise<T> => {
  const config = { ...DEFAULT_CONFIG, ...options, data: sanitizeData(options.data) };
  const requestKey = generateRequestKey(config);
  const requestId = generateRequestId();
  const baseUrlCheck = ensureApiBaseUrl();
  const requestUrl = buildRemoteUrl(config.url || "");
  const startedAt = Date.now();

  return new Promise((resolve, reject) => {
    if (!baseUrlCheck.ok) {
      if (!config.silent) {
        uni.showToast({ title: "请配置小程序接口域名", icon: "none" });
      }
      reject(new ApiError(-1, missingBaseUrlMessage));
      return;
    }

    // 取消相同的进行中请求
    if (pendingRequests.has(requestKey)) {
      traceRuntime("request:abort-duplicate", {
        requestId,
        url: requestUrl,
        method: config.method || "GET"
      });
      pendingRequests.get(requestKey)?.abort();
      pendingRequests.delete(requestKey);
    }

    traceRuntime("request:start", {
      requestId,
      url: requestUrl,
      method: config.method || "GET",
      timeout: config.timeout
    });

    // 显示loading
    if (config.showLoading) {
      uni.showLoading({ title: config.loadingText || "加载中..." });
    }

    const userStore = useUserStore();

    const doRequest = (retryCount: number) => {
      let finished = false;
      let manualTimeoutTimer: ReturnType<typeof setTimeout> | null = null;

      const finishRequest = () => {
        finished = true;
        if (manualTimeoutTimer) {
          clearTimeout(manualTimeoutTimer);
          manualTimeoutTimer = null;
        }
      };

      const task = uni.request({
        ...config,
        url: requestUrl,
        timeout: config.timeout,
        header: {
          ...(config.header || {}),
          Authorization: userStore.token || "",
          "X-Request-Id": requestId
        },
        success: (res) => {
          finishRequest();
          pendingRequests.delete(requestKey);
          if (config.showLoading) uni.hideLoading();
          const duration = Date.now() - startedAt;

          if (res.statusCode === 200) {
            const payload = res.data as ApiResponse<T>;
            traceRuntime("request:success", {
              requestId,
              url: requestUrl,
              method: config.method || "GET",
              statusCode: res.statusCode,
              businessCode: payload?.code,
              duration
            });
            if (payload.code === 200) {
              resolve(payload.data);
              return;
            }
            if (payload.code === 401) {
              const message = payload.message || payload.msg || "登录已过期";
              handleUnauthorized(message);
              reject(new ApiError(401, message));
              return;
            }
            if (!config.silent) {
              uni.showToast({ title: payload.message || payload.msg || "请求失败", icon: "none" });
            }
            reject(new ApiError(payload.code, payload.message || payload.msg || "请求失败"));
            return;
          }

          const payload = (res.data || {}) as Partial<ApiResponse<T>>;
          traceRuntime("request:http-error", {
            requestId,
            url: requestUrl,
            method: config.method || "GET",
            statusCode: res.statusCode,
            businessCode: payload?.code,
            duration
          });
          if (res.statusCode === 401 || payload.code === 401) {
            const message = payload.message || payload.msg || "登录已过期";
            handleUnauthorized(message);
            reject(new ApiError(401, message));
            return;
          }

          // 服务端错误，尝试重试
          if (res.statusCode >= 500 && retryCount < (config.retry || 0)) {
            setTimeout(() => doRequest(retryCount + 1), config.retryDelay);
            return;
          }

          if (!config.silent) {
            uni.showToast({ title: payload.message || payload.msg || "服务器异常", icon: "none" });
          }
          reject(new ApiError(res.statusCode, payload.message || payload.msg || `HTTP Error: ${res.statusCode}`));
        },
        fail: (err) => {
          finishRequest();
          pendingRequests.delete(requestKey);
          if (config.showLoading) uni.hideLoading();

          const errMsg = err.errMsg || "网络异常";
          const isTimeout = /timeout/i.test(errMsg);
          const duration = Date.now() - startedAt;
          const requestLabel = getRequestLabel(requestUrl, config.method);

          // 仅对幂等请求做网络重试，避免重复提交
          if (isSafeRetryMethod(config.method) && retryCount < (config.retry || 0)) {
            setTimeout(() => doRequest(retryCount + 1), config.retryDelay);
            return;
          }

          traceRuntime("request:fail", {
            requestId,
            url: requestUrl,
            method: config.method || "GET",
            timeout: config.timeout,
            errMsg,
            duration,
            retryCount
          });

          if (!config.silent) {
            uni.showToast({ title: isTimeout ? "请求超时，请稍后重试" : "网络异常", icon: "none" });
          }
          reject(new ApiError(-1, isTimeout ? `${errMsg} [${requestLabel}]` : errMsg));
        }
      });

      pendingRequests.set(requestKey, task);

      if ((config.timeout || 0) > 0) {
        manualTimeoutTimer = setTimeout(() => {
          if (finished) return;
          traceRuntime("request:manual-timeout", {
            requestId,
            url: requestUrl,
            method: config.method || "GET",
            timeout: config.timeout
          });
          task.abort();
        }, (config.timeout || 0) + 200);
      }
    };

    doRequest(0);
  });
};

/**
 * 取消所有进行中的请求
 */
export const cancelAllRequests = () => {
  pendingRequests.forEach((task) => task.abort());
  pendingRequests.clear();
};

/**
 * 取消指定请求
 */
export const cancelRequest = (method: string, url: string) => {
  const key = `${method}:${url}`;
  const task = pendingRequests.get(key);
  if (task) {
    task.abort();
    pendingRequests.delete(key);
  }
};

/**
 * GET请求快捷方法
 */
export const get = <T = any>(url: string, data?: any, config?: Partial<RequestConfig>) =>
  request<T>({ url, method: "GET", data, ...config });

/**
 * POST请求快捷方法
 */
export const post = <T = any>(url: string, data?: any, config?: Partial<RequestConfig>) =>
  request<T>({ url, method: "POST", data, ...config });

/**
 * PUT请求快捷方法
 */
export const put = <T = any>(url: string, data?: any, config?: Partial<RequestConfig>) =>
  request<T>({ url, method: "PUT", data, ...config });

/**
 * DELETE请求快捷方法
 */
export const del = <T = any>(url: string, data?: any, config?: Partial<RequestConfig>) =>
  request<T>({ url, method: "DELETE", data, ...config });
