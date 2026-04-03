import { useUserStore } from "@/stores/user";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

interface RequestConfig extends UniApp.RequestOptions {
  /** 请求超时时间，默认10秒 */
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
  timeout: 10000,
  retry: 1,
  retryDelay: 1000,
  showLoading: false,
  silent: false
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

// 生成请求唯一标识
const generateRequestKey = (options: RequestConfig): string => {
  return `${options.method || "GET"}:${options.url}`;
};

// 生成请求ID用于追踪
const generateRequestId = (): string => {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

// 处理未授权
const handleUnauthorized = () => {
  const userStore = useUserStore();
  userStore.logout();
  uni.showToast({ title: "请先登录", icon: "none" });
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
  const requestKey = generateRequestKey(options);
  const requestId = generateRequestId();

  return new Promise((resolve, reject) => {
    // 取消相同的进行中请求
    if (pendingRequests.has(requestKey)) {
      pendingRequests.get(requestKey)?.abort();
      pendingRequests.delete(requestKey);
    }

    // 显示loading
    if (config.showLoading) {
      uni.showLoading({ title: config.loadingText || "加载中..." });
    }

    const userStore = useUserStore();

    const doRequest = (retryCount: number) => {
      const task = uni.request({
        ...config,
        url: BASE_URL + config.url,
        timeout: config.timeout,
        header: {
          ...(config.header || {}),
          Authorization: userStore.token || "",
          "X-Request-Id": requestId
        },
        success: (res) => {
          pendingRequests.delete(requestKey);
          if (config.showLoading) uni.hideLoading();

          if (res.statusCode === 200) {
            const payload = res.data as ApiResponse<T>;
            if (payload.code === 200) {
              resolve(payload.data);
              return;
            }
            if (payload.code === 401) {
              handleUnauthorized();
              reject(new ApiError(401, "登录已过期"));
              return;
            }
            if (!config.silent) {
              uni.showToast({ title: payload.msg || "请求失败", icon: "none" });
            }
            reject(new ApiError(payload.code, payload.msg || "请求失败"));
            return;
          }

          // 服务端错误，尝试重试
          if (res.statusCode >= 500 && retryCount < (config.retry || 0)) {
            setTimeout(() => doRequest(retryCount + 1), config.retryDelay);
            return;
          }

          if (!config.silent) {
            uni.showToast({ title: "服务器异常", icon: "none" });
          }
          reject(new ApiError(res.statusCode, `HTTP Error: ${res.statusCode}`));
        },
        fail: (err) => {
          pendingRequests.delete(requestKey);
          if (config.showLoading) uni.hideLoading();

          // 网络错误重试
          if (retryCount < (config.retry || 0)) {
            setTimeout(() => doRequest(retryCount + 1), config.retryDelay);
            return;
          }

          if (!config.silent) {
            uni.showToast({ title: "网络异常", icon: "none" });
          }
          reject(new ApiError(-1, err.errMsg || "网络异常"));
        }
      });

      pendingRequests.set(requestKey, task);
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
