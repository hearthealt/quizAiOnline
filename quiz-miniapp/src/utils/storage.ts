export const storage = {
  set<T>(key: string, value: T) {
    uni.setStorageSync(key, value);
  },
  get<T>(key: string, defaultValue?: T): T | undefined {
    const value = uni.getStorageSync(key);
    if (value === undefined || value === null || value === "") {
      return defaultValue;
    }
    return value as T;
  },
  remove(key: string) {
    uni.removeStorageSync(key);
  },
  clear() {
    uni.clearStorageSync();
  }
};
