<template>
  <div class="convert-container">
    <!-- 左侧：上传和原始内容 -->
    <div class="panel left-panel">
      <div class="panel-header">
        <span class="panel-title">原始内容</span>
        <n-text v-if="originalContent" depth="3">{{ originalContent.length }} 字符</n-text>
      </div>
      
      <n-upload
        v-if="!originalContent"
        :max="1"
        accept=".xlsx,.xls,.csv,.pdf,.txt"
        :default-upload="false"
        @change="handleFileChange"
        class="upload-area"
      >
        <n-upload-dragger>
          <div class="upload-icon">
            <n-icon size="48" color="#999">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M9 16h6v-6h4l-7-7l-7 7h4zm-4 2h14v2H5z"/></svg>
            </n-icon>
          </div>
          <div class="upload-text">点击或拖拽文件到此处</div>
          <div class="upload-hint">支持 xlsx、csv、pdf、txt</div>
        </n-upload-dragger>
      </n-upload>

      <template v-else>
        <n-input
          v-model:value="originalContent"
          type="textarea"
          placeholder="可编辑或截取部分内容..."
          class="content-textarea"
        />
        <div class="panel-footer">
          <n-button size="small" @click="originalContent = ''">清空重选</n-button>
        </div>
      </template>
    </div>

    <!-- 中间：操作区 -->
    <div class="action-area">
      <n-button
        type="primary"
        size="large"
        :loading="parsing"
        :disabled="!originalContent"
        @click="handleParse"
        class="transform-btn"
      >
        <template #icon>
          <n-icon>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="m14 12l-4-4v3H2v2h8v3m8-10v14c0 1.1-.9 2-2 2H6c-1.1 0-2-.9-2-2v-2h2v2h12V5H6v2H4V5c0-1.1.9-2 2-2h12c1.1 0 2 .9 2 2"/></svg>
          </n-icon>
        </template>
        开始转换
      </n-button>
    </div>

    <!-- 右侧：转换结果 -->
    <div class="panel right-panel">
      <div class="panel-header">
        <span class="panel-title">转换结果</span>
        <n-space v-if="parsedQuestions.length" :size="8">
          <n-text depth="3">{{ parsedQuestions.length }} 道题</n-text>
          <n-button text type="success" size="small" @click="downloadResult">下载 Excel</n-button>
        </n-space>
      </div>
      
      <div v-if="!parsedQuestions.length && !parsing" class="empty-state">
        <n-icon size="48" color="#ccc">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8zm4 18H6V4h7v5h5zm-5-2c.6 0 1-.4 1-1s-.4-1-1-1s-1 .4-1 1s.4 1 1 1m2-4H9v2h6zm0-4H9v2h6z"/></svg>
        </n-icon>
        <div class="empty-text">转换结果将显示在这里</div>
      </div>
      
      <div v-else-if="parsing" class="loading-state">
        <n-spin size="large" />
        <div class="loading-text">正在解析中...</div>
      </div>
      
      <n-input
        v-else
        :value="JSON.stringify(parsedQuestions, null, 2)"
        type="textarea"
        readonly
        class="content-textarea"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useMessage } from 'naive-ui'
import * as XLSX from 'xlsx'
import * as aiApi from '@/api/ai'

const message = useMessage()

const originalContent = ref('')
const parsedQuestions = ref<any[]>([])
const parsing = ref(false)

async function handleFileChange(opt: any) {
  const file = opt.file?.file
  if (!file) return

  try {
    message.loading('正在解析文件...')
    const res = await aiApi.parseFile(file) as any
    originalContent.value = res
    parsedQuestions.value = []
    message.success('文件解析完成')
  } catch (e: any) {
    message.error(e.message || '文件解析失败')
  }
}

async function handleParse() {
  const content = originalContent.value.trim()
  if (!content) {
    message.warning('请先上传文件')
    return
  }

  parsing.value = true
  parsedQuestions.value = []
  
  try {
    const res = await aiApi.ruleParse(content) as unknown as any[]
    parsedQuestions.value = res
    message.success(`解析完成，共 ${res.length} 道题`)
  } catch (e: any) {
    message.error(e.message || '解析失败')
  } finally {
    parsing.value = false
  }
}

function downloadResult() {
  if (parsedQuestions.value.length === 0) {
    message.warning('没有可导出的题目')
    return
  }
  
  const data: any[][] = []
  
  const maxOptions = Math.max(4, ...parsedQuestions.value.map(q => q.options?.length || 0))
  const headers = ['题目内容', '题型', '正确答案', '解析', '难度']
  for (let i = 0; i < maxOptions; i++) {
    headers.push(`选项${String.fromCharCode(65 + i)}`)
  }
  data.push(headers)
  
  for (const q of parsedQuestions.value) {
    const row = [
      q.content || '',
      q.type || '',
      q.answer || '',
      q.analysis || '',
      q.difficulty || ''
    ]
    const options = q.options || []
    for (let i = 0; i < maxOptions; i++) {
      row.push(options[i] || '')
    }
    data.push(row)
  }
  
  const ws = XLSX.utils.aoa_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '题目')
  XLSX.writeFile(wb, '转换结果.xlsx')
}
</script>

<style scoped>
.convert-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
  min-height: 500px;
}

.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
}

.panel-footer {
  padding: 8px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.upload-area {
  flex: 1;
  display: flex;
  padding: 16px;
}

.upload-area :deep(.n-upload-dragger) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.upload-icon {
  margin-bottom: 16px;
}

.upload-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 13px;
  color: #999;
}

.content-textarea {
  flex: 1;
  margin: 0;
}

.content-textarea :deep(.n-input) {
  height: 100%;
}

.content-textarea :deep(textarea) {
  height: 100% !important;
  resize: none;
}

.action-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 0 8px;
}

.transform-btn {
  writing-mode: vertical-lr;
  padding: 24px 12px;
  height: auto;
}

.empty-state,
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.empty-text,
.loading-text {
  font-size: 14px;
  color: #999;
}
</style>
