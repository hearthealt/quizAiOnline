<template>
  <div class="admin-data-table">
    <n-data-table class="admin-data-table__table" v-bind="$attrs" :columns="columns" :scroll-x="resolvedScrollX" />

    <div class="admin-data-table__cards">
      <div v-if="mobileLoading" class="mobile-table-state">
        <n-spin size="small" />
      </div>

      <n-empty v-else-if="!mobileRows.length" class="mobile-table-state" description="暂无数据" size="small" />

      <template v-else>
        <div v-if="hasSelection" class="mobile-table-selection-bar">
          <span>已选 {{ checkedRowKeys.length }} 项</span>
          <div class="mobile-table-selection-actions">
            <n-button size="tiny" tertiary @click="toggleCurrentPageChecked">
              {{ allCurrentPageChecked ? '取消本页' : '全选本页' }}
            </n-button>
            <n-button v-if="checkedRowKeys.length" size="tiny" text @click="clearCheckedRows">清空</n-button>
          </div>
        </div>

        <article
          v-for="(row, rowIndex) in mobileRows"
          :key="getRowKey(row, rowIndex)"
          v-bind="getMobileRowProps(row, rowIndex)"
          :class="getMobileRowClass(row, rowIndex)"
        >
          <header class="mobile-table-card__head" :class="{ 'mobile-table-card__head--plain': !hasSelection }">
            <label v-if="hasSelection" class="mobile-table-check" @click.stop>
              <n-checkbox :checked="isRowChecked(row, rowIndex)" @update:checked="(checked: boolean) => updateRowChecked(row, rowIndex, checked)" />
            </label>

            <div v-if="mobilePrimaryColumn" class="mobile-table-primary">
              <div class="mobile-table-label">{{ getColumnTitle(mobilePrimaryColumn) }}</div>
              <div class="mobile-table-primary-value">
                <CellRenderer :column="mobilePrimaryColumn" :row="row" :row-index="rowIndex" />
              </div>
            </div>
          </header>

          <dl v-if="mobileDetailColumns.length" class="mobile-table-card__content">
            <div v-for="column in mobileDetailColumns" :key="getColumnKey(column)" class="mobile-table-field">
              <dt class="mobile-table-label">{{ getColumnTitle(column) }}</dt>
              <dd class="mobile-table-value">
                <CellRenderer :column="column" :row="row" :row-index="rowIndex" />
              </dd>
            </div>
          </dl>

          <footer v-if="mobileActionColumns.length" class="mobile-table-actions">
            <CellRenderer v-for="column in mobileActionColumns" :key="getColumnKey(column)" :column="column" :row="row" :row-index="rowIndex" />
          </footer>
        </article>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, ref, useAttrs } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { getTableScrollX } from '@/utils/table-scroll'

defineOptions({
  inheritAttrs: false
})

const props = defineProps<{
  columns: DataTableColumns
  scrollX?: number | string
}>()

type RowData = Record<string, any>
type RowKey = string | number
type TableColumn = DataTableColumns[number] & {
  children?: TableColumn[]
  key?: string | number
  render?: (row: RowData, rowIndex: number) => any
  title?: unknown
  type?: string
}

const attrs = useAttrs()
const resolvedScrollX = computed(() => props.scrollX ?? getTableScrollX(props.columns))
const localCheckedRowKeys = ref<RowKey[]>([])

const mobileRows = computed<RowData[]>(() => {
  const data = attrs.data
  return Array.isArray(data) ? data as RowData[] : []
})

const mobileLoading = computed(() => attrs.loading === true)

const leafColumns = computed<TableColumn[]>(() => flattenColumns(props.columns as TableColumn[]))
const mobileColumns = computed(() => leafColumns.value.filter((column) => column.type !== 'selection' && column.type !== 'expand'))
const mobilePrimaryColumn = computed(() => resolveMobilePrimaryColumn(mobileColumns.value))
const mobileActionColumns = computed(() => mobileColumns.value.filter((column) => isActionColumn(column)))
const mobileDetailColumns = computed(() => mobileColumns.value.filter((column) => column !== mobilePrimaryColumn.value && !isActionColumn(column)))
const hasSelection = computed(() => leafColumns.value.some((column) => column.type === 'selection'))
const hasControlledCheckedRowKeys = computed(() => getCheckedRowKeysAttr() !== undefined)

const checkedRowKeys = computed<RowKey[]>(() => {
  const value = getCheckedRowKeysAttr()
  return Array.isArray(value) ? value as RowKey[] : localCheckedRowKeys.value
})

const currentPageRowKeys = computed(() => mobileRows.value.map((row, index) => getRowKey(row, index)))
const allCurrentPageChecked = computed(() => {
  return currentPageRowKeys.value.length > 0 && currentPageRowKeys.value.every((key) => hasRowKey(checkedRowKeys.value, key))
})

const CellRenderer = defineComponent({
  name: 'AdminDataTableCellRenderer',
  props: {
    column: {
      type: Object,
      required: true
    },
    row: {
      type: Object,
      required: true
    },
    rowIndex: {
      type: Number,
      required: true
    }
  },
  setup(cellProps) {
    return () => renderCell(cellProps.column as TableColumn, cellProps.row as RowData, cellProps.rowIndex)
  }
})

function flattenColumns(columns: TableColumn[]): TableColumn[] {
  return columns.flatMap((column) => column.children?.length ? flattenColumns(column.children) : [column])
}

function getRowKey(row: RowData, rowIndex: number): RowKey {
  const rowKey = attrs.rowKey ?? attrs['row-key']
  if (typeof rowKey === 'function') {
    return rowKey(row)
  }
  return row.id ?? row.key ?? rowIndex
}

function getColumnKey(column: TableColumn) {
  if (column.type) return column.type
  return column.key ?? String(column.title ?? '')
}

function getColumnTitle(column: TableColumn) {
  return typeof column.title === 'string' ? column.title : String(column.key ?? '')
}

function isActionColumn(column: TableColumn) {
  return column.key === 'action' || column.key === 'actions' || column.title === '操作'
}

function resolveMobilePrimaryColumn(columns: TableColumn[]) {
  const candidates = columns.filter((column) => !isActionColumn(column))
  if (!candidates.length) return undefined

  return candidates.reduce((best, column) => {
    return getPrimaryColumnScore(column) > getPrimaryColumnScore(best) ? column : best
  }, candidates[0])
}

function getPrimaryColumnScore(column: TableColumn) {
  const title = getColumnTitle(column)
  const key = String(column.key ?? '')
  const text = `${title} ${key}`.toLowerCase()

  if (/(用户|管理员|nickname|username|phone|avatar)/i.test(text)) return 100
  if (/(题目内容|content)/i.test(text)) return 96
  if (/(套餐名称|名称|name|title)/i.test(text)) return 92
  if (/(题库|bank)/i.test(text)) return 84
  if (/(订单号|orderno)/i.test(text)) return 78
  if (/(任务id|task|job|id)/i.test(text)) return 64
  if (/(时间|create|date|time)/i.test(text)) return 48
  if (/(状态|status|sort|排序|封面|cover)/i.test(text)) return 10

  return 40
}

function renderCell(column: TableColumn, row: RowData, rowIndex: number) {
  if (typeof column.render === 'function') {
    return column.render(row, rowIndex)
  }

  if (column.key !== undefined) {
    const value = row[column.key]
    return value === null || value === undefined || value === '' ? '-' : value
  }

  return '-'
}

function getMobileRowProps(row: RowData, rowIndex: number) {
  const rowProps = attrs.rowProps
  if (typeof rowProps !== 'function') {
    return {}
  }

  const props = rowProps(row, rowIndex) || {}
  return {
    ...props,
    style: props.style
  }
}

function getMobileRowClass(row: RowData, rowIndex: number) {
  const rowProps = attrs.rowProps
  const props = typeof rowProps === 'function' ? rowProps(row, rowIndex) || {} : {}
  return [
    'mobile-table-card',
    props.class,
    {
      'mobile-table-card--checked': hasSelection.value && isRowChecked(row, rowIndex)
    }
  ]
}

function isRowChecked(row: RowData, rowIndex: number) {
  return hasRowKey(checkedRowKeys.value, getRowKey(row, rowIndex))
}

function updateRowChecked(row: RowData, rowIndex: number, checked: boolean) {
  const rowKey = getRowKey(row, rowIndex)
  const nextKeys = checked
    ? uniqueRowKeys([...checkedRowKeys.value, rowKey])
    : checkedRowKeys.value.filter((key) => !isSameRowKey(key, rowKey))

  updateCheckedRows(nextKeys, { row, action: checked ? 'check' : 'uncheck' })
}

function toggleCurrentPageChecked() {
  const nextKeys = allCurrentPageChecked.value
    ? checkedRowKeys.value.filter((key) => !hasRowKey(currentPageRowKeys.value, key))
    : uniqueRowKeys([...checkedRowKeys.value, ...currentPageRowKeys.value])

  updateCheckedRows(nextKeys, { row: null, action: allCurrentPageChecked.value ? 'uncheckAll' : 'checkAll' })
}

function clearCheckedRows() {
  updateCheckedRows([], { row: null, action: 'uncheckAll' })
}

function updateCheckedRows(nextKeys: RowKey[], meta: { row: RowData | null, action: string }) {
  if (!hasControlledCheckedRowKeys.value) {
    localCheckedRowKeys.value = nextKeys
  }

  const nextRows = mobileRows.value.filter((item, index) => hasRowKey(nextKeys, getRowKey(item, index)))
  const handler = getCheckedRowKeysHandler()

  if (Array.isArray(handler)) {
    handler.forEach((fn) => typeof fn === 'function' && fn(nextKeys, nextRows, meta))
    return
  }

  if (typeof handler === 'function') {
    handler(nextKeys, nextRows, meta)
  }
}

function getCheckedRowKeysAttr() {
  return attrs.checkedRowKeys ?? attrs['checked-row-keys']
}

function getCheckedRowKeysHandler() {
  return attrs['onUpdate:checkedRowKeys'] ?? attrs['onUpdate:checked-row-keys'] ?? attrs.onUpdateCheckedRowKeys
}

function hasRowKey(keys: RowKey[], rowKey: RowKey) {
  return keys.some((key) => isSameRowKey(key, rowKey))
}

function isSameRowKey(left: RowKey, right: RowKey) {
  return left === right || String(left) === String(right)
}

function uniqueRowKeys(keys: RowKey[]) {
  return keys.reduce<RowKey[]>((result, key) => {
    if (!hasRowKey(result, key)) {
      result.push(key)
    }
    return result
  }, [])
}
</script>

<style scoped>
.admin-data-table__cards {
  display: none;
}

@media (max-width: 768px) {
  .admin-data-table__table {
    display: none;
  }

  .admin-data-table__cards {
    display: grid;
    gap: 8px;
  }

  .mobile-table-selection-bar {
    position: sticky;
    top: var(--admin-mobile-selection-top, 180px);
    z-index: 30;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 8px 10px;
    border: 1px solid rgba(95, 68, 47, 0.1);
    border-radius: 10px;
    background: rgba(255, 253, 250, 0.94);
    color: var(--color-text-secondary);
    font-size: 12px;
    font-weight: 600;
    box-shadow: 0 8px 22px rgba(59, 32, 18, 0.07);
    backdrop-filter: blur(12px);
  }

  .mobile-table-selection-actions {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    flex: 0 0 auto;
  }

  .mobile-table-state {
    min-height: 112px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--border-light);
    border-radius: 10px;
    background: rgba(255, 253, 250, 0.92);
  }

  .mobile-table-card {
    position: relative;
    display: block;
    overflow: hidden;
    padding: 0;
    border: 1px solid var(--border-light);
    border-radius: 10px;
    background: rgba(255, 253, 250, 0.96);
    box-shadow: 0 8px 24px rgba(59, 32, 18, 0.06);
  }

  .mobile-table-card--checked {
    border-color: rgba(182, 64, 44, 0.36);
    box-shadow: 0 0 0 2px rgba(182, 64, 44, 0.08);
  }

  .mobile-table-card__head {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    align-items: center;
    gap: 9px;
    padding: 10px 12px;
    border-bottom: 1px solid rgba(95, 68, 47, 0.08);
    background: rgba(255, 255, 255, 0.54);
  }

  .mobile-table-card__head--plain {
    grid-template-columns: minmax(0, 1fr);
  }

  .mobile-table-check {
    flex: 0 0 auto;
  }

  .mobile-table-primary {
    min-width: 0;
    display: grid;
    grid-template-columns: minmax(70px, 28%) minmax(0, 1fr);
    align-items: center;
    gap: 10px;
  }

  .mobile-table-primary-value {
    min-width: 0;
    margin: 0;
    overflow: hidden;
    color: var(--color-text);
    font-size: 14px;
    font-weight: 650;
    line-height: 1.45;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-table-card__content {
    display: grid;
    gap: 0;
    margin: 0;
    padding: 0;
  }

  .mobile-table-field {
    display: grid;
    grid-template-columns: minmax(70px, 28%) minmax(0, 1fr);
    align-items: center;
    gap: 10px;
    min-width: 0;
    min-height: 38px;
    padding: 7px 12px;
    border-bottom: 1px solid rgba(95, 68, 47, 0.08);
  }

  .mobile-table-field:last-child {
    border-bottom: 0;
  }

  .mobile-table-label {
    min-width: 0;
    color: var(--color-text-muted);
    font-size: 12px;
    font-weight: 600;
    line-height: 1.55;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-table-value {
    min-width: 0;
    margin: 0;
    overflow: hidden;
    color: var(--color-text);
    font-size: 13px;
    line-height: 1.55;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-table-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 6px;
    padding: 9px 12px 11px;
    border-top: 1px solid rgba(95, 68, 47, 0.08);
    background: rgba(251, 248, 243, 0.76);
  }

  .mobile-table-actions :deep(.n-space) {
    width: 100%;
    flex-wrap: wrap;
    justify-content: center;
    gap: 6px !important;
  }

  .mobile-table-actions :deep(.n-button) {
    min-height: 28px;
    flex: 0 0 auto;
    padding: 0 8px;
    border-radius: 8px;
  }

  .mobile-table-actions :deep(.n-button--text) {
    background: rgba(255, 255, 255, 0.7);
    border: 1px solid rgba(95, 68, 47, 0.08);
  }

  .mobile-table-primary-value :deep(.n-ellipsis),
  .mobile-table-value :deep(.n-ellipsis) {
    max-width: 100%;
    white-space: nowrap !important;
  }

  .mobile-table-primary-value :deep(*),
  .mobile-table-value :deep(*) {
    min-width: 0;
    max-width: 100%;
  }

  .mobile-table-primary-value :deep(div),
  .mobile-table-primary-value :deep(span),
  .mobile-table-value :deep(div),
  .mobile-table-value :deep(span) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-table-primary-value :deep([style*="display:flex"]),
  .mobile-table-primary-value :deep([style*="display: flex"]),
  .mobile-table-value :deep([style*="display:flex"]),
  .mobile-table-value :deep([style*="display: flex"]) {
    display: inline-flex !important;
    max-width: 100%;
    vertical-align: middle;
  }

  .mobile-table-primary-value :deep(.n-tag),
  .mobile-table-value :deep(.n-tag) {
    max-width: 100%;
    vertical-align: middle;
  }

  .mobile-table-primary-value :deep(img),
  .mobile-table-value :deep(img) {
    max-width: 100%;
    max-height: 34px;
    object-fit: cover;
    vertical-align: middle;
  }

  .mobile-table-primary-value :deep(.n-switch),
  .mobile-table-value :deep(.n-switch) {
    display: inline-flex !important;
    width: var(--n-width);
    min-width: var(--n-width) !important;
    max-width: none;
    overflow: visible;
    vertical-align: middle;
  }

  .mobile-table-primary-value :deep(.n-switch__rail),
  .mobile-table-value :deep(.n-switch__rail) {
    min-width: var(--n-rail-width) !important;
    max-width: none;
    overflow: hidden;
  }

  .mobile-table-primary-value :deep(.n-switch__button),
  .mobile-table-value :deep(.n-switch__button) {
    max-width: var(--n-button-width) !important;
    overflow: visible;
    text-overflow: clip;
  }

  .mobile-table-value :deep(.n-space) {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  @media (max-width: 480px) {
    .mobile-table-field {
      grid-template-columns: minmax(64px, 32%) minmax(0, 1fr);
      gap: 8px;
    }
  }
}
</style>
