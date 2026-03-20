import { ref } from 'vue'

/**
 * 通用加载状态管理
 */
export function useLoading(initialValue = false) {
  const loading = ref(initialValue)
  const error = ref<string | null>(null)

  const startLoading = () => {
    loading.value = true
    error.value = null
  }

  const stopLoading = () => {
    loading.value = false
  }

  const setError = (msg: string) => {
    error.value = msg
    loading.value = false
  }

  /**
   * 执行异步操作并自动管理loading状态
   */
  const withLoading = async <T>(fn: () => Promise<T>): Promise<T | null> => {
    try {
      startLoading()
      const result = await fn()
      return result
    } catch (e: any) {
      setError(e.message || '操作失败')
      return null
    } finally {
      stopLoading()
    }
  }

  return {
    loading,
    error,
    startLoading,
    stopLoading,
    setError,
    withLoading
  }
}
