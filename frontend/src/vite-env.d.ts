/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Origin of the Java API, e.g. https://tradeup-api-xxxx.onrender.com
   *
   * Leave unset in development: Vite proxies /api to localhost:8080, so the
   * browser stays on a single origin. Set it for a production build, where the
   * frontend and the API are served from different hosts.
   *
   * Vite inlines this at build time, so changing it needs a rebuild.
   */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
