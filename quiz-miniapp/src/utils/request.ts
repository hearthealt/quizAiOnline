import { useUserStore } from "@/stores/user";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

type ApiResponse<T> = {
  code: number;
  msg: string;
  data: T;
};

export const request = <T = any>(options: UniApp.RequestOptions): Promise<T> => {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore();
    uni.request({
      ...options,
      url: BASE_URL + options.url,
      header: {
        ...(options.header || {}),
        Authorization: userStore.token || ""
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const payload = res.data as ApiResponse<T>;
          if (payload.code === 200) {
            resolve(payload.data);
            return;
          }
          if (payload.code === 401) {
            userStore.logout();
            uni.showToast({ title: "请先登录", icon: "none" });
            reject(new Error("登录已过期"));
            return;
          }
          uni.showToast({ title: payload.msg || "请求失败", icon: "none" });
          reject(new Error(payload.msg || "请求失败"));
          return;
        }
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(new Error("网络异常"));
      },
      fail: (err) => {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      }
    });
  });
};
