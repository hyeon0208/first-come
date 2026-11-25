import { defineConfig } from '@playwright/test'

// docker compose로 띄운 환경(화면 3000번)을 대상으로 처음부터 끝까지 눌러 본다.
// 먼저 저장소 루트에서 `docker compose up -d --build`를 해 둔다.
export default defineConfig({
  testDir: './e2e',
  timeout: 120_000,
  expect: { timeout: 15_000 },
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:3000',
    viewport: { width: 1440, height: 900 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
