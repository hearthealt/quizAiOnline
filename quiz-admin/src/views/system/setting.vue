<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">系统设置</span>
        </div>
      </template>

      <n-tabs type="line" animated>
        <!-- 站点设置 -->
        <n-tab-pane name="site" tab="站点设置">
          <n-form label-placement="left" label-width="100" class="setting-form">
            <n-form-item label="站点名称">
              <n-input v-model:value="formData.siteName" placeholder="Quiz AI 在线答题系统" />
            </n-form-item>
            <n-form-item label="站点描述">
              <n-input v-model:value="formData.siteDescription" type="textarea" :rows="2" placeholder="请输入站点描述" />
            </n-form-item>
            <n-form-item label="开放注册">
              <n-switch v-model:value="registerEnabled">
                <template #checked>开放</template>
                <template #unchecked>关闭</template>
              </n-switch>
            </n-form-item>
            <n-form-item label="站点Logo">
              <div class="logo-upload">
                <n-upload
                  :max="1"
                  accept="image/*"
                  :default-upload="false"
                  :show-file-list="false"
                  @change="(opt: any) => handleLogoUpload(opt.file?.file)"
                >
                  <div class="upload-trigger" :class="{ 'has-image': formData.siteLogo }">
                    <n-image v-if="formData.siteLogo" :src="formData.siteLogo" width="120" height="40" object-fit="contain" preview-disabled />
                    <template v-else>
                      <n-icon size="20" color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon>
                      <span>上传Logo</span>
                    </template>
                  </div>
                </n-upload>
                <n-button v-if="formData.siteLogo" text type="error" size="small" @click="formData.siteLogo = ''">移除</n-button>
              </div>
            </n-form-item>
            <n-form-item label="版权信息">
              <n-input v-model:value="formData.copyright" placeholder="© 2025 Quiz AI" />
            </n-form-item>
            <n-form-item label="备案号">
              <n-input v-model:value="formData.icpNumber" placeholder="京ICP备xxxxxxxx号" />
            </n-form-item>
            <n-form-item label="练习管理员">
              <n-input
                v-model:value="formData.practiceManagerContact"
                type="textarea"
                :rows="2"
                placeholder="例如：微信号 quiz_admin / 手机 138xxxx / 其它联系说明"
              />
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="saving" @click="handleSave">保存配置</n-button>
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 小程序设置 -->
        <n-tab-pane name="miniapp" tab="小程序设置">
          <n-form label-placement="left" label-width="100" class="setting-form">
            <n-form-item label="AppID">
              <n-input v-model:value="formData.wxAppId" placeholder="微信小程序AppID" />
            </n-form-item>
            <n-form-item label="AppSecret">
              <n-input v-model:value="formData.wxAppSecret" type="password" show-password-on="click" placeholder="微信小程序AppSecret" />
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="saving" @click="handleSave">保存配置</n-button>
            </n-form-item>
          </n-form>
          <n-alert type="info" :bordered="false" class="setting-alert">
            配置微信小程序的AppID和AppSecret后，小程序端微信登录功能将生效。
          </n-alert>
        </n-tab-pane>

        <!-- AI辅导设置 -->
        <n-tab-pane name="ai" tab="AI辅导设置">
          <n-form label-placement="left" label-width="100" class="setting-form">
            <n-form-item label="人物设定">
              <n-input
                v-model:value="formData.aiChatPersona"
                type="textarea"
                :rows="4"
                placeholder="例如：你是AI智能导师，回答简洁清晰，必要时分点说明。"
              />
            </n-form-item>
            <n-form-item label="默认问候语">
              <n-input
                v-model:value="formData.aiChatGreeting"
                type="textarea"
                :rows="3"
                placeholder="例如：你好！我是你的 AI 智能导师……"
              />
            </n-form-item>
            <n-divider style="margin: 8px 0 16px">题目生成模板</n-divider>
            <n-form-item label="解析模板">
              <n-input
                v-model:value="formData.aiPromptAnalysis"
                type="textarea"
                :rows="4"
                placeholder="生成解析时使用的 Prompt"
              />
            </n-form-item>
            <n-form-item label="答案模板">
              <n-input
                v-model:value="formData.aiPromptAnswer"
                type="textarea"
                :rows="4"
                placeholder="生成答案时使用的 Prompt"
              />
            </n-form-item>
            <n-form-item label="综合模板">
              <n-input
                v-model:value="formData.aiPromptBoth"
                type="textarea"
                :rows="4"
                placeholder="同时生成答案和解析时使用的 Prompt"
              />
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="saving" @click="handleSave">保存配置</n-button>
            </n-form-item>
          </n-form>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { getSetting, updateSetting } from '@/api/setting'
import { uploadImage } from '@/api/upload'
import { useAppStore } from '@/stores/app'

const message = useMessage()
const appStore = useAppStore()
const loading = ref(false)
const saving = ref(false)

const formData = ref<Record<string, string>>({
  siteName: '',
  siteDescription: '',
  siteLogo: '',
  copyright: '',
  icpNumber: '',
  practiceManagerContact: '',
  wxAppId: '',
  wxAppSecret: '',
  registerEnabled: '1',
  aiChatPersona: '你是AI智能导师，回答简洁清晰，必要时分点说明。',
  aiChatGreeting: '你好！我是你的 AI 智能导师。遇到不懂的题目、需要解释的知识点，或者想要制定学习计划，都可以随时问我哦！',
  aiPromptAnalysis: '题目：{content}\n选项：{options}\n正确答案：{answer}\n\n请用简洁的语言输出解析：\n1. 一句话说明为什么选{answer}\n2. 简要指出其他选项的错误\n不要啰嗦，直接输出解析内容。',
  aiPromptAnswer: '题目：{content}\n选项：{options}\n解析：{analysis}\n\n请直接输出答案选项字母，多选题用逗号分隔。',
  aiPromptBoth: '题目：{content}\n选项：{options}\n\n请按格式输出：\n答案：[选项字母]\n解析：[简洁说明正确答案的原因，并简要指出其他选项的错误]',
})

const registerEnabled = computed({
  get: () => formData.value.registerEnabled === '1',
  set: (val: boolean) => { formData.value.registerEnabled = val ? '1' : '0' },
})

async function handleLogoUpload(file: File | null) {
  if (!file) return
  try {
    const url = await uploadImage(file) as unknown as string
    formData.value.siteLogo = url
    message.success('上传成功')
  } catch {
    message.error('上传失败')
  }
}

async function loadSetting() {
  loading.value = true
  try {
    const res = await getSetting() as any
    if (res && typeof res === 'object') {
      for (const key of Object.keys(formData.value)) {
        if (res[key] !== undefined && res[key] !== null) {
          formData.value[key] = res[key]
        }
      }
    }
    appStore.setSiteConfig({
      siteName: formData.value.siteName,
      siteLogo: formData.value.siteLogo,
      siteDescription: formData.value.siteDescription,
    })
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSetting(formData.value)
    appStore.setSiteConfig({
      siteName: formData.value.siteName,
      siteLogo: formData.value.siteLogo,
      siteDescription: formData.value.siteDescription,
    })
    message.success('保存成功')
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadSetting())
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.main-card {
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

.setting-form {
  max-width: 600px;
  padding-top: 12px;
}

.logo-upload {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.upload-trigger {
  width: 140px;
  height: 50px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fafafa;
  overflow: hidden;
}

.upload-trigger:hover {
  border-color: #667eea;
}

.upload-trigger.has-image {
  border-style: solid;
  background: #fff;
  padding: 4px;
}

.upload-trigger span {
  font-size: 12px;
  color: #999;
}

.setting-alert {
  max-width: 600px;
  margin-top: 12px;
}
</style>
