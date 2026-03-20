import { ref } from 'vue';
import { toggleFavorite, checkFavorite } from '@/api/favorite';
import { useAuth } from './useAuth';

/**
 * 收藏功能逻辑
 */
export function useFavorite(questionId?: number) {
  const { requireLogin } = useAuth();
  const isFavorite = ref(false);
  const loading = ref(false);

  /**
   * 检查是否已收藏
   */
  const check = async (qId?: number) => {
    const id = qId || questionId;
    if (!id) return false;
    try {
      isFavorite.value = await checkFavorite(id);
      return isFavorite.value;
    } catch (e) {
      return false;
    }
  };

  /**
   * 切换收藏状态
   */
  const toggle = async (qId?: number) => {
    const id = qId || questionId;
    if (!id) return false;
    if (!requireLogin()) return false;

    loading.value = true;
    try {
      const result = await toggleFavorite(id);
      isFavorite.value = result;
      uni.showToast({ 
        title: result ? '收藏成功' : '已取消收藏', 
        icon: 'none' 
      });
      return isFavorite.value;
    } catch (e: any) {
      uni.showToast({ title: e.message || '操作失败', icon: 'none' });
      return isFavorite.value;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 添加收藏（如果未收藏则切换）
   */
  const add = async (qId?: number) => {
    const id = qId || questionId;
    if (!id || isFavorite.value) return true;
    return await toggle(id);
  };

  /**
   * 取消收藏（如果已收藏则切换）
   */
  const remove = async (qId?: number) => {
    const id = qId || questionId;
    if (!id || !isFavorite.value) return true;
    await toggle(id);
    return !isFavorite.value;
  };

  /**
   * 设置收藏状态（不调用API）
   */
  const setFavorite = (value: boolean) => {
    isFavorite.value = value;
  };

  return {
    isFavorite,
    loading,
    check,
    toggle,
    add,
    remove,
    setFavorite
  };
}
