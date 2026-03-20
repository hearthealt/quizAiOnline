<template>
  <div class="page-container">
    <div class="config-grid">
      <!-- 左侧：模型配置 -->
      <n-card :bordered="false" size="small" class="config-card" :loading="pageLoading">
        <template #header>
          <div class="card-header">
            <span class="card-title">模型配置</span>
          </div>
        </template>

        <n-form ref="formRef" :model="formData" label-placement="left" label-width="100">
          <n-form-item label="Base URL" path="baseUrl">
            <n-input v-model:value="formData.baseUrl" placeholder="https://api.openai.com/v1" />
          </n-form-item>
          <n-form-item label="API Key" path="apiKey">
            <n-input v-model:value="formData.apiKey" type="password" show-password-on="click" placeholder="请输入API Key" />
          </n-form-item>
          <n-form-item label="模型" path="model">
            <n-select
              v-model:value="formData.model"
              :options="modelOptions"
              filterable
              tag
              placeholder="选择或输入模型名称"
              :loading="modelsLoading"
            />
          </n-form-item>
          <n-grid :cols="2" :x-gap="16">
            <n-gi>
              <n-form-item label="Max Tokens" path="maxTokens" label-width="90">
                <n-input-number v-model:value="formData.maxTokens" :min="1" style="width: 100%" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="Temperature" path="temperature" label-width="90">
                <n-input-number v-model:value="formData.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
              </n-form-item>
            </n-gi>
          </n-grid>

          <n-divider style="margin: 12px 0">Prompt 模板</n-divider>

          <n-form-item label="解析Prompt" path="promptAnalysis">
            <n-input v-model:value="formData.promptAnalysis" type="textarea" :rows="2" placeholder="生成解析的提示词" />
          </n-form-item>
          <n-form-item label="答案Prompt" path="promptAnswer">
            <n-input v-model:value="formData.promptAnswer" type="textarea" :rows="2" placeholder="生成答案的提示词" />
          </n-form-item>
          <n-form-item label="综合Prompt" path="promptBoth">
            <n-input v-model:value="formData.promptBoth" type="textarea" :rows="2" placeholder="同时生成答案和解析的提示词" />
          </n-form-item>

          <n-form-item label=" ">
            <n-space>
              <n-button type="primary" :loading="saving" @click="handleSave">保存配置</n-button>
              <n-button :loading="testing" @click="handleTest">测试连通性</n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </n-card>

      <!-- 右侧：iFlow 配置 -->
      <n-card :bordered="false" size="small" class="config-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">iFlow API Key 自动管理</span>
          </div>
        </template>

        <n-form :model="formData" label-placement="left" label-width="100">
          <n-form-item label="BXAuth">
            <n-input v-model:value="formData.bxAuth" type="password" show-password-on="click" placeholder="iFlow平台BXAuth Cookie" />
          </n-form-item>
          <n-form-item label="iFlow名称">
            <n-input v-model:value="formData.iflowName" placeholder="iFlow平台用户名称" />
          </n-form-item>
          <n-form-item label="自动续期">
            <n-switch v-model:value="autoRenewEnabled">
              <template #checked>已开启</template>
              <template #unchecked>已关闭</template>
            </n-switch>
          </n-form-item>

          <div class="status-section">
            <div class="status-label">API Key 状态</div>
            <div class="status-content">
              <template v-if="iflowStatus">
                <n-tag :type="iflowStatus.hasExpired ? 'error' : 'success'" size="small" bordered>
                  {{ iflowStatus.hasExpired ? '已过期' : '有效' }}
                </n-tag>
                <span v-if="iflowStatus.expireTime" class="expire-time">
                  过期时间: {{ iflowStatus.expireTime }}
                </span>
              </template>
              <n-tag v-else type="default" size="small" bordered>未配置</n-tag>
              <div v-if="formData.lastRenewResult" class="renew-result">
                {{ formData.lastRenewResult }}
              </div>
            </div>
          </div>

          <n-form-item label=" ">
            <n-space>
              <n-button type="primary" :loading="saving" @click="handleSave">保存配置</n-button>
              <n-button type="warning" :loading="refreshing" @click="handleRefresh" :disabled="!formData.bxAuth">手动刷新</n-button>
              <n-button :loading="statusLoading" @click="loadIflowStatus" :disabled="!formData.bxAuth">查看状态</n-button>
            </n-space>
          </n-form-item>
        </n-form>

        <n-alert type="info" :bordered="false" class="info-alert">
          保存配置时会自动获取最新API Key并同步。自动续期开启后，每30分钟检查一次，过期前自动重置。
        </n-alert>
      </n-card>
    </div>

    <!-- 测试结果弹窗 -->
    <n-modal v-model:show="showTestModal" preset="card" title="连通性测试结果" style="width: 520px">
      <template v-if="testResult">
        <n-descriptions :column="1" label-placement="left" bordered size="small">
          <n-descriptions-item label="状态">
            <n-tag :type="testResult.success ? 'success' : 'error'" size="small" bordered>
              {{ testResult.success ? '连接成功' : '连接失败' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="API Key">{{ testResult.apiKey }}</n-descriptions-item>
          <n-descriptions-item label="Base URL">{{ testResult.baseUrl }}</n-descriptions-item>
          <n-descriptions-item label="模型">{{ testResult.model }}</n-descriptions-item>
          <n-descriptions-item v-if="testResult.reply" label="AI回复">{{ testResult.reply }}</n-descriptions-item>
          <n-descriptions-item v-if="testResult.error" label="错误信息">
            <span style="color: #d03050">{{ testResult.error }}</span>
          </n-descriptions-item>
        </n-descriptions>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useMessage} from 'naive-ui'
import {getConfig, getIflowModels, getIflowStatus, refreshIflowKey, testConnection, updateConfig} from '@/api/ai'

const message = useMessage()
const pageLoading = ref(false)
const saving = ref(false)
const testing = ref(false)
const refreshing = ref(false)
const statusLoading = ref(false)
const modelsLoading = ref(false)
const showTestModal = ref(false)

const formData = ref({
  baseUrl: '',
  apiKey: '',
  model: '',
  promptAnalysis: '',
  promptAnswer: '',
  promptBoth: '',
  maxTokens: 2048,
  temperature: 0.7,
  bxAuth: '',
  iflowName: '',
  autoRenew: 0,
  lastRenewResult: '',
  expireTime: '',
})

const modelOptions = ref<{ label: string; value: string }[]>([])
const iflowStatus = ref<{ hasExpired: boolean; expireTime: string; apiKey: string } | null>(null)
const testResult = ref<{ success: boolean; apiKey: string; baseUrl: string; model: string; reply?: string; error?: string } | null>(null)

const autoRenewEnabled = computed({
  get: () => formData.value.autoRenew === 1,
  set: (val: boolean) => { formData.value.autoRenew = val ? 1 : 0 },
})

async function loadConfig() {
  pageLoading.value = true
  try {
    const res = await getConfig() as any
    if (res) {
      Object.assign(formData.value, {
        ...res,
        bxAuth: res.bxAuth || '',
        iflowName: res.iflowName || '',
        autoRenew: res.autoRenew ?? 0,
        lastRenewResult: res.lastRenewResult || '',
        expireTime: res.expireTime || '',
      })
    }
  } finally {
    pageLoading.value = false
  }
}

async function loadModels() {
  modelsLoading.value = true
  try {
    const res = await getIflowModels() as any
    if (Array.isArray(res)) {
      modelOptions.value = res.map((m: any) => ({ label: `${m.showName} (${m.modelName})`, value: m.modelName }))
    }
  } catch {} finally {
    modelsLoading.value = false
  }
}

async function loadIflowStatus() {
  statusLoading.value = true
  try {
    const res = await getIflowStatus() as any
    if (res && res.success) {
      iflowStatus.value = res
    } else {
      iflowStatus.value = null
      if (res?.error) message.warning(res.error)
    }
  } catch {
    message.error('获取iFlow状态失败')
  } finally {
    statusLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateConfig({
      baseUrl: formData.value.baseUrl,
      apiKey: formData.value.apiKey,
      model: formData.value.model,
      promptAnalysis: formData.value.promptAnalysis,
      promptAnswer: formData.value.promptAnswer,
      promptBoth: formData.value.promptBoth,
      maxTokens: formData.value.maxTokens,
      temperature: formData.value.temperature,
      bxAuth: formData.value.bxAuth,
      iflowName: formData.value.iflowName,
      autoRenew: formData.value.autoRenew,
    })
    message.success('保存成功')
    await loadConfig()
    if (formData.value.bxAuth) await loadIflowStatus()
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testConnection() as any
    showTestModal.value = true
  } catch (e: any) {
    testResult.value = { success: false, apiKey: '', baseUrl: '', model: '', error: e.message || '请求异常' }
    showTestModal.value = true
  } finally {
    testing.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  try {
    const res = await refreshIflowKey() as any
    message.success('刷新完成')
    await loadConfig()
    if (res) iflowStatus.value = res
  } catch (e: any) {
    message.error(e.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

onMounted(async () => {
  await loadConfig()
  loadModels()
  if (formData.value.bxAuth) loadIflowStatus()
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.config-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.status-section {
  padding: 12px 16px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 16px;
}

.status-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.status-content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.expire-time {
  font-size: 13px;
  color: #666;
}

.renew-result {
  width: 100%;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.info-alert {
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
