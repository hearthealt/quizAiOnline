/// <reference types="vite/client" />
/// <reference types="@dcloudio/types" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly UNI_PLATFORM?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module "uview-plus" {
  const uviewPlus: any;
  export default uviewPlus;
}
