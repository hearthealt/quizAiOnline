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
  practiceManagerContact?: string;
};

let siteConfigPromise: Promise<void> | null = null;

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
      if (siteConfigPromise) return siteConfigPromise;

      siteConfigPromise = getAppConfig()
        .then((data) => {
          const config = data as AppConfig;
          this.setSiteConfig({
            siteName: config?.siteName,
            siteDescription: config?.siteDescription,
            siteLogo: config?.siteLogo,
            copyright: config?.copyright,
            icpNumber: config?.icpNumber,
            practiceManagerContact: config?.practiceManagerContact
          });
        })
        .finally(() => {
          siteConfigPromise = null;
        });

      return siteConfigPromise;
    }
  }
});
