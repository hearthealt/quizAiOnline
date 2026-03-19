<template>
  <div class="login-container">
    <!-- 左侧品牌区 -->
    <div class="brand-section">
      <div class="brand-content">
        <div class="brand-logo">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="64" height="64" fill="currentColor">
            <path d="M12 3L1 9l4 2.18v6L12 21l7-3.82v-6l2-1.09V17h2V9L12 3zm6.82 6L12 12.72L5.18 9L12 5.28L18.82 9zM17 15.99l-5 2.73l-5-2.73v-3.72L12 15l5-2.73v3.72z"/>
          </svg>
        </div>
        <h1 class="brand-title">在线答题系统</h1>
        <p class="brand-desc">智能化题库管理 · 高效率知识学习</p>
        <div class="brand-features">
          <div class="feature-item">
            <n-icon size="20"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19L21 7l-1.41-1.41z"/></svg></n-icon>
            <span>题库智能管理</span>
          </div>
          <div class="feature-item">
            <n-icon size="20"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19L21 7l-1.41-1.41z"/></svg></n-icon>
            <span>AI解析生成</span>
          </div>
          <div class="feature-item">
            <n-icon size="20"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19L21 7l-1.41-1.41z"/></svg></n-icon>
            <span>数据统计分析</span>
          </div>
        </div>
      </div>
      <div class="brand-footer">
        <p>© 2024 Quiz AI Online. All rights reserved.</p>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-section">
      <div class="login-form-wrap">
        <div class="login-header">
          <h2>欢迎回来</h2>
          <p>请登录您的管理员账号</p>
        </div>

        <n-form ref="formRef" :model="formValue" :rules="rules" label-placement="left" size="large">
          <n-form-item path="username">
            <n-input
              v-model:value="formValue.username"
              placeholder="请输入用户名"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4"/></svg></n-icon>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="password">
            <n-input
              v-model:value="formValue.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2m-6 9c-1.1 0-2-.9-2-2s.9-2 2-2s2 .9 2 2s-.9 2-2 2M9 8V6c0-1.66 1.34-3 3-3s3 1.34 3 3v2z"/></svg></n-icon>
              </template>
            </n-input>
          </n-form-item>

          <div class="login-options">
            <n-checkbox v-model:checked="rememberMe">记住账号密码</n-checkbox>
          </div>

          <n-button type="primary" block size="large" :loading="loading" @click="handleLogin">
            登 录
          </n-button>
        </n-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, type FormInst, type FormRules } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const REMEMBER_KEY = 'admin_remember'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInst | null>(null)
const rememberMe = ref(false)

const formValue = ref({ username: '', password: '' })

const rules: FormRules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
}

onMounted(() => {
  // 读取记住的账号
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    try {
      const { username, password } = JSON.parse(saved)
      formValue.value.username = username || ''
      formValue.value.password = password || ''
      rememberMe.value = true
    } catch {}
  }
})

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await authStore.login(formValue.value.username, formValue.value.password)
    
    // 记住账号密码
    if (rememberMe.value) {
      localStorage.setItem(REMEMBER_KEY, JSON.stringify({
        username: formValue.value.username,
        password: formValue.value.password
      }))
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    
    message.success('登录成功')
    router.push('/dashboard')
  } catch (e: any) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
}

.brand-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 40px;
  position: relative;
}

.brand-content {
  text-align: center;
  max-width: 400px;
}

.brand-logo {
  margin-bottom: 24px;
  opacity: 0.95;
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 16px;
  opacity: 0.85;
  margin: 0 0 40px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  opacity: 0.9;
}

.brand-footer {
  position: absolute;
  bottom: 24px;
  font-size: 13px;
  opacity: 0.6;
}

.login-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  padding: 40px;
}

.login-form-wrap {
  width: 100%;
  max-width: 400px;
  background: #fff;
  padding: 48px 40px;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

@media (max-width: 900px) {
  .brand-section {
    display: none;
  }
  .login-section {
    flex: 1;
  }
}
</style>
