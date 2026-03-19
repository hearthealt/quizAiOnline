import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSetting } from '@/api/setting'

export const useAppStore = defineStore('app', () => {
  const collapsed = ref(false)
  const siteName = ref('Quiz AI')
  const siteLogo = ref('')
  const siteDescription = ref('')
  const configLoaded = ref(false)

  function setSiteConfig(payload: { siteName?: string; siteLogo?: string; siteDescription?: string }) {
    if (payload.siteName !== undefined) siteName.value = payload.siteName || 'Quiz AI'
    if (payload.siteLogo !== undefined) siteLogo.value = payload.siteLogo || ''
    if (payload.siteDescription !== undefined) siteDescription.value = payload.siteDescription || ''
    configLoaded.value = true
  }

  async function loadSiteConfig(force = false) {
    if (configLoaded.value && !force) return
    const res = await getSetting() as any
    setSiteConfig({
      siteName: res?.siteName,
      siteLogo: res?.siteLogo,
      siteDescription: res?.siteDescription,
    })
  }

  return {
    collapsed,
    siteName,
    siteLogo,
    siteDescription,
    loadSiteConfig,
    setSiteConfig,
  }
})
