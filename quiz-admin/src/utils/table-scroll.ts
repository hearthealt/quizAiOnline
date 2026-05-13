import type { DataTableColumns } from 'naive-ui'

type TableColumn = DataTableColumns[number] & {
  children?: TableColumn[]
  key?: string | number
  minWidth?: string | number
  title?: unknown
  type?: string
  width?: string | number
}

const DEFAULT_COLUMN_WIDTH = 160
const DEFAULT_ELLIPSIS_WIDTH = 240
const ACTION_COLUMN_WIDTH = 140
const SELECTION_COLUMN_WIDTH = 48
const MIN_TABLE_SCROLL_X = 720

function toPixelWidth(value: string | number | undefined): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value !== 'string') {
    return undefined
  }

  const matched = value.trim().match(/^(\d+(?:\.\d+)?)px$/)
  return matched ? Number(matched[1]) : undefined
}

function isActionColumn(column: TableColumn) {
  return column.key === 'action' || column.key === 'actions' || column.title === '操作'
}

function getColumnWidth(column: TableColumn): number {
  if (column.children?.length) {
    return column.children.reduce((sum, child) => sum + getColumnWidth(child), 0)
  }

  const explicitWidth = toPixelWidth(column.width) ?? toPixelWidth(column.minWidth)
  if (explicitWidth) {
    return explicitWidth
  }

  if (column.type === 'selection' || column.type === 'expand') {
    return SELECTION_COLUMN_WIDTH
  }

  if (isActionColumn(column)) {
    return ACTION_COLUMN_WIDTH
  }

  if ('ellipsis' in column && column.ellipsis) {
    return DEFAULT_ELLIPSIS_WIDTH
  }

  return DEFAULT_COLUMN_WIDTH
}

export function getTableScrollX(columns: DataTableColumns, minWidth = MIN_TABLE_SCROLL_X) {
  const contentWidth = columns.reduce((sum, column) => sum + getColumnWidth(column as TableColumn), 0)
  return Math.max(minWidth, contentWidth)
}
