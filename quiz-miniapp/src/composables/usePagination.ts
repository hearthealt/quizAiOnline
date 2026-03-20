import { ref, computed } from 'vue';
import type { PageResult, PageParams } from '@/api/types';

export interface PaginationOptions {
  pageSize?: number;
  immediate?: boolean;
}

/**
 * 分页列表管理
 */
export function usePagination<T>(
  fetchFn: (params: PageParams) => Promise<PageResult<T>>,
  options: PaginationOptions = {}
) {
  const { pageSize = 10 } = options;

  const list = ref<T[]>([]) as { value: T[] };
  const loading = ref(false);
  const refreshing = ref(false);
  const pageNum = ref(1);
  const total = ref(0);
  const error = ref<string | null>(null);

  const hasMore = computed(() => list.value.length < total.value);
  const isEmpty = computed(() => !loading.value && list.value.length === 0);

  /**
   * 加载数据
   */
  const loadData = async (isRefresh = false) => {
    if (loading.value) return;

    try {
      if (isRefresh) {
        refreshing.value = true;
        pageNum.value = 1;
      }
      loading.value = true;
      error.value = null;

      const result = await fetchFn({
        pageNum: pageNum.value,
        pageSize
      });

      if (isRefresh) {
        list.value = result.list;
      } else {
        list.value = [...list.value, ...result.list];
      }
      total.value = result.total;
      pageNum.value++;
    } catch (e: any) {
      error.value = e.message || '加载失败';
    } finally {
      loading.value = false;
      refreshing.value = false;
    }
  };

  /**
   * 刷新列表
   */
  const refresh = () => loadData(true);

  /**
   * 加载更多
   */
  const loadMore = () => {
    if (hasMore.value && !loading.value) {
      loadData(false);
    }
  };

  /**
   * 重置列表
   */
  const reset = () => {
    list.value = [];
    pageNum.value = 1;
    total.value = 0;
    error.value = null;
  };

  return {
    list,
    loading,
    refreshing,
    total,
    error,
    hasMore,
    isEmpty,
    refresh,
    loadMore,
    reset
  };
}
