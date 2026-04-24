/// <reference types="node" />

import { defineConfig } from '@playwright/test';

declare const process: { env: Record<string, string | undefined> };

const baseURL = process.env['E2E_BASE_URL'] || process.env['PLAYWRIGHT_BASE_URL'];

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: 2,
  workers: undefined,
  outputDir: 'test-results',
  expect: { timeout: 10_000 },
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure'
  },
  webServer: {
    command: 'npm run start -- --port=4200 --host=127.0.0.1',
    url: baseURL || 'http://localhost:4200',
    reuseExistingServer: !process.env['CI'],
    timeout: 120_000
  },
  reporter: [['list']]
});
