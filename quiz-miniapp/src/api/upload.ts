import { useUserStore } from "@/stores/user";
import { buildRemoteUrl, ensureApiBaseUrl, missingBaseUrlMessage } from "@/utils/baseUrl";
import { traceRuntime } from "@/utils/runtimeTrace";

export const uploadImage = (filePath: string): Promise<string> =>
  new Promise((resolve, reject) => {
    const baseUrlCheck = ensureApiBaseUrl();
    if (!baseUrlCheck.ok) {
      uni.showToast({ title: "请配置小程序接口域名", icon: "none" });
      reject(new Error(missingBaseUrlMessage));
      return;
    }

    const userStore = useUserStore();
    const requestUrl = buildRemoteUrl("/api/app/upload/image");
    const startedAt = Date.now();

    traceRuntime("upload:start", {
      url: requestUrl,
      timeout: 60000
    });

    uni.uploadFile({
      url: requestUrl,
      filePath,
      name: "file",
      timeout: 60000,
      header: {
        Authorization: userStore.token || ""
      },
      success: (res) => {
        try {
          traceRuntime("upload:success", {
            url: requestUrl,
            statusCode: res.statusCode,
            duration: Date.now() - startedAt
          });
          const payload = JSON.parse(res.data as unknown as string);
          if (res.statusCode === 200 && payload.code === 200) {
            resolve(payload.data);
            return;
          }
          if (res.statusCode === 401 || payload.code === 401) {
            const message = payload.message || payload.msg || "登录已过期";
            userStore.handleSessionExpired(message);
            uni.showToast({ title: message, icon: "none" });
            reject(new Error(message));
            return;
          }
          reject(new Error(payload.message || payload.msg || "上传失败"));
        } catch (err) {
          reject(err);
        }
      },
      fail: (err) => {
        traceRuntime("upload:fail", {
          url: requestUrl,
          errMsg: err.errMsg || "upload fail",
          duration: Date.now() - startedAt
        });
        reject(err);
      }
    });
  });
