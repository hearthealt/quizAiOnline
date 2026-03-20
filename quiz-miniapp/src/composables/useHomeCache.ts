import { ref, readonly } from 'vue';
import { getHomeIndex, type HomeVO } from '@/api/home';
import { storage } from '@/utils/storage';

const HOME_CACHE_KEY = 'home_data_cache';
const HOME_CACHE_TTL = 5 * 60 * 1000; // 5分钟缓存

interface CacheData<T> {
  data: T;
  timestamp: number;
}

/**
 * 首页数据缓存管理
 * 实现策略：先展示缓存，后台刷新，数据变更后更新UI
 */
export function useHomeCache() {
  const homeData = ref<HomeVO | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const isFromCache = ref(false);

  /**
   * 从缓存读取数据
   */
  const readCache = (): HomeVO | null => {
    const cached = storage.get<CacheData<HomeVO>>(HOME_CACHE_KEY, null);
    if (!cached) return null;
    
    // 检查是否过期
    const now = Date.now();
    if (now - cached.timestamp > HOME_CACHE_TTL) {
      storage.remove(HOME_CACHE_KEY);
      return null;
    }
    return cached.data;
  };

  /**
   * 写入缓存
   */
  const writeCache = (data: HomeVO) => {
    const cacheData: CacheData<HomeVO> = {
      data,
      timestamp: Date.now()
    };
    storage.set(HOME_CACHE_KEY, cacheData);
  };

  /**
   * 加载首页数据
   * @param forceRefresh 是否强制刷新（忽略缓存）
   */
  const loadHomeData = async (forceRefresh = false): Promise<HomeVO | null> => {
    error.value = null;

    // 先尝试从缓存读取（非强制刷新时）
    if (!forceRefresh) {
      const cached = readCache();
      if (cached) {
        homeData.value = cached;
        isFromCache.value = true;
        // 后台静默刷新
        silentRefresh();
        return cached;
      }
    }

    // 从API获取
    loading.value = true;
    isFromCache.value = false;
    try {
      const data = await getHomeIndex();
      homeData.value = data;
      writeCache(data);
      return data;
    } catch (e: any) {
      error.value = e.message || '加载失败';
      // 如果请求失败，尝试使用过期缓存
      const staleCache = storage.get<CacheData<HomeVO>>(HOME_CACHE_KEY, null);
      if (staleCache) {
        homeData.value = staleCache.data;
        isFromCache.value = true;
        return staleCache.data;
      }
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 静默后台刷新
   */
  const silentRefresh = async () => {
    try {
      const data = await getHomeIndex();
      // 检查数据是否有变化
      const hasChanged = JSON.stringify(data) !== JSON.stringify(homeData.value);
      if (hasChanged) {
        homeData.value = data;
        writeCache(data);
        isFromCache.value = false;
      }
    } catch (e) {
      // 静默刷新失败时不做任何处理
    }
  };

  /**
   * 强制刷新数据
   */
  const refresh = () => loadHomeData(true);

  /**
   * 清除缓存
   */
  const clearCache = () => {
    storage.remove(HOME_CACHE_KEY);
    homeData.value = null;
  };

  /**
   * 预加载数据（可在应用启动时调用）
   */
  const preload = () => {
    // 如果已有缓存，后台静默刷新；否则加载数据
    const cached = readCache();
    if (cached) {
      homeData.value = cached;
      isFromCache.value = true;
      silentRefresh();
    } else {
      loadHomeData();
    }
  };

  return {
    homeData: readonly(homeData),
    loading: readonly(loading),
    error: readonly(error),
    isFromCache: readonly(isFromCache),
    loadHomeData,
    refresh,
    clearCache,
    preload
  };
}

// 创建单例，在多个组件间共享状态
let instance: ReturnType<typeof useHomeCache> | null = null;

export function useSharedHomeCache() {
  if (!instance) {
    instance = useHomeCache();
  }
  return instance;
}
