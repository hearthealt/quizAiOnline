import { ref, reactive } from 'vue'
import type { PageResult } from '@/types'

export function useTable<T>(fetchFn: (params: any) => Promise<PageResult<T>>) {
  const loading = ref(false)
  const data = ref<T[]>([]) as ReturnType<typeof ref<T[]>>
  const pagination = reactive({
    page: 1,
    pageSize: 10,
    itemCount: 0,
    pageSizes: [10, 20, 50],
    showSizePicker: true,
    prefix: ({ itemCount }: { itemCount: number }) => `共 ${itemCount} 条`,
  })

  async function fetchData(extraParams: Record<string, any> = {}) {
    loading.value = true
    try {
      const res = await fetchFn({
        pageNum: pagination.page,
        pageSize: pagination.pageSize,
        ...extraParams,
      })
      data.value = res.list
      pagination.itemCount = res.total
    } finally {
      loading.value = false
    }
  }

  function handlePageChange(page: number) {
    pagination.page = page
    fetchData()
  }

  function handlePageSizeChange(pageSize: number) {
    pagination.pageSize = pageSize
    pagination.page = 1
    fetchData()
  }

  return { loading, data, pagination, fetchData, handlePageChange, handlePageSizeChange }
}
