import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:4200',
    trace: 'on-first-retry'
  },
  webServer: {
    command: 'npm run start -- --port=4200 --host=127.0.0.1',
    url: process.env.E2E_BASE_URL || 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000
  },
  reporter: [['list']]
});
