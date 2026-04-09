<template>
  <div class="page-container">
    <div class="config-grid">
      <n-card :bordered="false" size="small" class="config-card" :loading="pageLoading">
        <template #header>
          <div class="card-header">
            <span class="card-title">AI 提供商配置</span>
          </div>
        </template>

        <n-form :model="formData" label-placement="left" label-width="110">
          <n-form-item label="提供商" path="provider">
            <n-select v-model:value="formData.provider" :options="providerOptions" />
          </n-form-item>

          <n-form-item label="Base URL" path="baseUrl">
            <n-input v-model:value="formData.baseUrl" :placeholder="currentProviderMeta.baseUrl || '请输入兼容 OpenAI 的 Base URL'" />
          </n-form-item>

          <n-form-item label="API Key" path="apiKey">
            <n-input v-model:value="formData.apiKey" type="password" show-password-on="click" placeholder="请输入 API Key" />
          </n-form-item>

          <n-form-item label="模型" path="model">
            <n-space vertical style="width: 100%" :size="8">
              <n-select
                v-model:value="formData.model"
                :options="modelOptions"
                filterable
                tag
                placeholder="选择或输入模型名称"
                :loading="modelsLoading"
              />
              <div class="model-actions">
                <span class="model-tip">{{ modelTip }}</span>
                <n-button size="small" quaternary :loading="modelsLoading" @click="fetchModels(true)">
                  重新获取
                </n-button>
              </div>
            </n-space>
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

          <n-form-item label=" ">
            <n-space>
              <n-button type="primary" :loading="saving" @click="handleSave">保存配置</n-button>
              <n-button :loading="testing" @click="handleTest">测试连通性</n-button>
              <n-button v-if="formData.provider !== 'CUSTOM'" quaternary @click="applyProviderPreset(true)">应用预设 Base URL</n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </n-card>

      <n-card :bordered="false" size="small" class="config-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">接入说明</span>
          </div>
        </template>

        <n-space vertical :size="16">
          <n-alert type="info" :bordered="false">
            当前只保留通用 AI 提供商模式。OpenAI、DeepSeek 和自定义服务都按兼容 OpenAI Chat Completions 的方式接入。
          </n-alert>

          <div class="meta-block">
            <div class="meta-title">当前提供商</div>
            <div class="meta-value">{{ currentProviderMeta.label }}</div>
            <div class="meta-desc">{{ currentProviderMeta.description }}</div>
          </div>

          <div class="meta-block">
            <div class="meta-title">建议 Base URL</div>
            <div class="meta-code">{{ currentProviderMeta.baseUrl || '自定义填写' }}</div>
          </div>

          <div class="meta-block">
            <div class="meta-title">常用模型</div>
            <div class="tag-list">
              <n-tag v-for="model in currentProviderMeta.models" :key="model" size="small" round>
                {{ model }}
              </n-tag>
              <n-tag v-if="!currentProviderMeta.models.length" size="small" round>手动填写</n-tag>
            </div>
          </div>

          <div class="meta-block">
            <div class="meta-title">Prompt 变量</div>
            <div class="tag-list">
              <n-tag size="small" round>{content}</n-tag>
              <n-tag size="small" round>{options}</n-tag>
              <n-tag size="small" round>{answer}</n-tag>
              <n-tag size="small" round>{analysis}</n-tag>
            </div>
            <div class="meta-desc">题目生成 Prompt 已迁移到“系统设置 > AI辅导设置”。</div>
          </div>
        </n-space>
      </n-card>
    </div>

    <n-modal v-model:show="showTestModal" preset="card" title="连通性测试结果" style="width: 560px">
      <template v-if="testResult">
        <n-descriptions :column="1" label-placement="left" bordered size="small">
          <n-descriptions-item label="状态">
            <n-tag :type="testResult.success ? 'success' : 'error'" size="small" bordered>
              {{ testResult.success ? '连接成功' : '连接失败' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="提供商">{{ providerLabel(testResult.provider) }}</n-descriptions-item>
          <n-descriptions-item label="API Key">{{ testResult.apiKey }}</n-descriptions-item>
          <n-descriptions-item label="Base URL">{{ testResult.baseUrl }}</n-descriptions-item>
          <n-descriptions-item label="模型">{{ testResult.model }}</n-descriptions-item>
          <n-descriptions-item v-if="testResult.mode" label="调用链路">{{ testResult.mode }}</n-descriptions-item>
          <n-descriptions-item v-if="testResult.reply" label="AI 回复">{{ testResult.reply }}</n-descriptions-item>
          <n-descriptions-item v-if="testResult.error" label="错误信息">
            <span class="error-text">{{ testResult.error }}</span>
          </n-descriptions-item>
        </n-descriptions>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { getConfig, getModels, testConnection, updateConfig } from '@/api/ai'

type ProviderType = 'OPENAI' | 'DEEPSEEK' | 'CUSTOM'

const message = useMessage()
const pageLoading = ref(false)
const saving = ref(false)
const testing = ref(false)
const modelsLoading = ref(false)
const showTestModal = ref(false)
let modelFetchTimer: ReturnType<typeof setTimeout> | null = null

const providerPresetMap: Record<ProviderType, { label: string; baseUrl: string; models: string[]; description: string }> = {
  OPENAI: {
    label: 'OpenAI',
    baseUrl: 'https://api.openai.com/v1',
    models: ['gpt-4o-mini', 'gpt-4o', 'gpt-4.1-mini'],
    description: '直接填写 OpenAI API Key，即可调用 ChatGPT 系列模型。'
  },
  DEEPSEEK: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com/v1',
    models: ['deepseek-chat', 'deepseek-reasoner'],
    description: 'DeepSeek 使用兼容 OpenAI 的接口形式，填写官方 Key 即可。'
  },
  CUSTOM: {
    label: '自定义兼容接口',
    baseUrl: '',
    models: [],
    description: '适用于自建网关或其他兼容 OpenAI Chat Completions 的服务。'
  }
}

const providerOptions = (Object.keys(providerPresetMap) as ProviderType[]).map((key) => ({
  label: providerPresetMap[key].label,
  value: key
}))

const formData = ref({
  provider: 'OPENAI' as ProviderType,
  baseUrl: '',
  apiKey: '',
  model: '',
  maxTokens: 2048,
  temperature: 0.7
})

const testResult = ref<{
  success: boolean
  provider: string
  apiKey: string
  baseUrl: string
  model: string
  mode?: string
  reply?: string
  error?: string
} | null>(null)

const currentProviderMeta = computed(() => providerPresetMap[formData.value.provider] || providerPresetMap.CUSTOM)
const fetchedModels = ref<string[]>([])
const modelOptions = computed(() => {
  const merged = Array.from(new Set([...fetchedModels.value, ...currentProviderMeta.value.models]))
  return merged.map((item) => ({ label: item, value: item }))
})
const modelTip = computed(() => {
  if (modelsLoading.value) return '正在通过 /v1/models 拉取模型列表'
  if (fetchedModels.value.length > 0) return `已自动获取 ${fetchedModels.value.length} 个模型`
  return '填写 API Key 后会自动尝试获取模型列表'
})

function providerLabel(provider: string) {
  return providerPresetMap[(provider as ProviderType) || 'CUSTOM']?.label || provider || '未知'
}

function applyProviderPreset(force = false) {
  const preset = currentProviderMeta.value
  if (formData.value.provider === 'CUSTOM') return
  if (force || !formData.value.baseUrl.trim()) {
    formData.value.baseUrl = preset.baseUrl
  }
  if (force || !formData.value.model.trim()) {
    formData.value.model = preset.models[0] || ''
  }
}

async function fetchModels(forceMessage = false) {
  if (!formData.value.apiKey.trim()) {
    fetchedModels.value = []
    return
  }
  modelsLoading.value = true
  try {
    const res = await getModels({
      provider: formData.value.provider,
      baseUrl: formData.value.baseUrl,
      apiKey: formData.value.apiKey
    }) as Array<{ id: string }>
    fetchedModels.value = Array.isArray(res) ? res.map((item) => item.id).filter(Boolean) : []
    if (!formData.value.model.trim() && fetchedModels.value.length > 0) {
      formData.value.model = fetchedModels.value[0]
    }
    if (forceMessage) {
      message.success(fetchedModels.value.length > 0 ? `已获取 ${fetchedModels.value.length} 个模型` : '未返回可用模型')
    }
  } catch (e: any) {
    fetchedModels.value = []
    if (forceMessage) {
      message.error(e.message || '获取模型列表失败')
    }
  } finally {
    modelsLoading.value = false
  }
}

function scheduleFetchModels() {
  if (modelFetchTimer) {
    clearTimeout(modelFetchTimer)
  }
  if (!formData.value.apiKey.trim()) {
    fetchedModels.value = []
    return
  }
  modelFetchTimer = setTimeout(() => {
    void fetchModels(false)
  }, 600)
}

watch(
  () => formData.value.provider,
  (next, prev) => {
    const prevPreset = prev ? providerPresetMap[prev as ProviderType] : null
    const shouldReplaceBaseUrl = !formData.value.baseUrl || (prevPreset != null && formData.value.baseUrl === prevPreset.baseUrl)
    const shouldReplaceModel = !formData.value.model || (prevPreset != null && prevPreset.models.includes(formData.value.model))
    if (next !== 'CUSTOM' && shouldReplaceBaseUrl) {
      formData.value.baseUrl = providerPresetMap[next].baseUrl
    }
    if (shouldReplaceModel) {
      formData.value.model = providerPresetMap[next].models[0] || ''
    }
    scheduleFetchModels()
  }
)

watch(
  () => [formData.value.baseUrl, formData.value.apiKey],
  () => {
    scheduleFetchModels()
  }
)

async function loadConfig() {
  pageLoading.value = true
  try {
    const res = await getConfig() as any
    if (res) {
      Object.assign(formData.value, {
        provider: (res.provider || 'OPENAI') as ProviderType,
        baseUrl: res.baseUrl || '',
        apiKey: res.apiKey || '',
        model: res.model || '',
        maxTokens: res.maxTokens ?? 2048,
        temperature: res.temperature ?? 0.7
      })
    }
    if (!formData.value.baseUrl || !formData.value.model) {
      applyProviderPreset(false)
    }
    if (formData.value.apiKey) {
      await fetchModels(false)
    }
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateConfig({
      provider: formData.value.provider,
      baseUrl: formData.value.baseUrl,
      apiKey: formData.value.apiKey,
      model: formData.value.model,
      maxTokens: formData.value.maxTokens,
      temperature: formData.value.temperature
    })
    message.success('保存成功')
    await loadConfig()
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
    testResult.value = await testConnection({
      provider: formData.value.provider,
      baseUrl: formData.value.baseUrl,
      apiKey: formData.value.apiKey,
      model: formData.value.model,
      maxTokens: formData.value.maxTokens,
      temperature: formData.value.temperature
    }) as any
  } catch (e: any) {
      testResult.value = {
        success: false,
        provider: formData.value.provider,
        apiKey: '',
        baseUrl: formData.value.baseUrl,
        model: formData.value.model,
        mode: '',
        error: e.message || '请求异常'
      }
  } finally {
    testing.value = false
    showTestModal.value = true
  }
}

onMounted(loadConfig)

onBeforeUnmount(() => {
  if (modelFetchTimer) {
    clearTimeout(modelFetchTimer)
    modelFetchTimer = null
  }
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

.meta-block {
  padding: 14px 16px;
  background: #f7f8fa;
  border-radius: 10px;
}

.meta-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.meta-value {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.meta-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.meta-code {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: #222;
  word-break: break-all;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.model-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.model-tip {
  font-size: 12px;
  color: #666;
}

.error-text {
  color: #d03050;
}

@media (max-width: 1200px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
