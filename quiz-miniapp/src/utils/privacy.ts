import { storage } from "@/utils/storage";

export const LEGAL_CONSENT_VERSION = "2026-04-08";
const LEGAL_CONSENT_KEY = "quiz_legal_consent";

export const SERVICE_AGREEMENT_PATH = "/pages/auth/service-agreement";
export const PRIVACY_POLICY_PATH = "/pages/auth/privacy-policy";

type LegalConsentRecord = {
  version: string;
  agreedAt: string;
};

export const getLegalConsentRecord = () =>
  storage.get<LegalConsentRecord | null>(LEGAL_CONSENT_KEY, null) || null;

export const hasLegalConsent = () => getLegalConsentRecord()?.version === LEGAL_CONSENT_VERSION;

export const saveLegalConsent = () => {
  storage.set<LegalConsentRecord>(LEGAL_CONSENT_KEY, {
    version: LEGAL_CONSENT_VERSION,
    agreedAt: new Date().toISOString()
  });
};

export const clearLegalConsent = () => {
  storage.remove(LEGAL_CONSENT_KEY);
};

export const ensureLegalConsent = (message = "请先阅读并同意《用户服务协议》《隐私政策》") => {
  if (hasLegalConsent()) {
    return true;
  }
  uni.showToast({ title: message, icon: "none" });
  return false;
};

export const openAgreementPage = (type: "service" | "privacy") => {
  uni.navigateTo({
    url: type === "service" ? SERVICE_AGREEMENT_PATH : PRIVACY_POLICY_PATH
  });
};
