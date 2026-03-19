import { defineStore } from "pinia";
import { getAppConfig, type AppConfig } from "@/api/config";

export type AppSettings = {
  autoNext: boolean;
};

export type SiteConfig = {
  siteName?: string;
  siteDescription?: string;
  siteLogo?: string;
  copyright?: string;
  icpNumber?: string;
};

export const useAppStore = defineStore("app", {
  state: () => ({
    settings: {
      autoNext: true
    } as AppSettings,
    siteConfig: {} as SiteConfig,
    configLoaded: false
  }),
  actions: {
    setSettings(settings: Partial<AppSettings>) {
      this.settings = { ...this.settings, ...settings };
    },
    setSiteConfig(config: SiteConfig) {
      this.siteConfig = { ...this.siteConfig, ...config };
      this.configLoaded = true;
    },
    async loadSiteConfig(force = false) {
      if (this.configLoaded && !force) return;
      const data = (await getAppConfig()) as AppConfig;
      this.setSiteConfig({
        siteName: data?.siteName,
        siteDescription: data?.siteDescription,
        siteLogo: data?.siteLogo,
        copyright: data?.copyright,
        icpNumber: data?.icpNumber
      });
    }
  }
});
