/**
 * 确认对话框hook
 */
export function useConfirm() {
  /**
   * 显示确认对话框
   */
  const confirm = (
    content: string,
    title = '确认',
    options?: {
      type?: 'info' | 'success' | 'warning' | 'error'
      positiveText?: string
      negativeText?: string
    }
  ): Promise<boolean> => {
    return new Promise((resolve) => {
      window.$dialog?.warning({
        title,
        content,
        positiveText: options?.positiveText || '确定',
        negativeText: options?.negativeText || '取消',
        onPositiveClick: () => resolve(true),
        onNegativeClick: () => resolve(false),
        onClose: () => resolve(false)
      })
    })
  }

  /**
   * 删除确认
   */
  const confirmDelete = (name?: string): Promise<boolean> => {
    const content = name ? `确定要删除「${name}」吗？` : '确定要删除吗？'
    return confirm(content, '删除确认')
  }

  /**
   * 批量删除确认
   */
  const confirmBatchDelete = (count: number): Promise<boolean> => {
    return confirm(`确定要删除选中的 ${count} 条数据吗？`, '批量删除确认')
  }

  /**
   * 状态切换确认
   */
  const confirmStatusChange = (name: string, enable: boolean): Promise<boolean> => {
    const action = enable ? '启用' : '禁用'
    return confirm(`确定要${action}「${name}」吗？`, `${action}确认`)
  }

  return {
    confirm,
    confirmDelete,
    confirmBatchDelete,
    confirmStatusChange
  }
}
