import { useUserStore } from "@/stores/user";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

export const uploadImage = (filePath: string): Promise<string> =>
  new Promise((resolve, reject) => {
    const userStore = useUserStore();
    uni.uploadFile({
      url: `${BASE_URL}/api/app/upload/image`,
      filePath,
      name: "file",
      header: {
        Authorization: userStore.token || ""
      },
      success: (res) => {
        try {
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
      fail: reject
    });
  });
