/**
 * G8 — Security
 * TC-ORG-001-30 … TC-ORG-001-31
 *
 * Source : ai-governance/01-modules/ORG/ORG-001-organization-module/05-tests/test-cases.md
 * Layer  : Backend API (Security layer)
 * Author : Playwright Expert Agent (auto-generated 2026-04-23)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

test.use({
  trace: 'retain-on-failure',
  screenshot: 'only-on-failure',
  video: 'retain-on-failure',
});

const BASE   = 'http://localhost:7272';
const TENANT = 'default';

// Protected endpoints to probe — one from each domain
const PROTECTED_ENDPOINTS = [
  { label: 'legal-entities', path: '/api/organization/legal-entities' },
  { label: 'regions',        path: '/api/organization/regions' },
  { label: 'branches',       path: '/api/organization/branches' },
];

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G8 — Security: Authentication required', () => {
  let ctx: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    ctx = await playwright.request.newContext({ baseURL: BASE });
  });

  test.afterAll(async () => {
    await ctx.dispose();
  });

  // ── TC-ORG-001-30 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-30] GET without auth token — HTTP 401', async () => {
    for (const ep of PROTECTED_ENDPOINTS) {
      await test.step(`GET ${ep.path} (no token)`, async () => {
        const resp = await ctx.get(`${BASE}${ep.path}`, {
          headers: { 'X-Tenant-Id': TENANT },
        });

        trace({
          tcId: 'TC-ORG-001-30',
          endpoint: `GET ${ep.path}`,
          requestPayload: { note: 'no Authorization header' },
          response: { status: resp.status() },
          statusCode: resp.status(),
        });

        expect(
          resp.status(),
          `${ep.label}: expected 401 without token, got ${resp.status()}`,
        ).toBe(401);
      });
    }
  });

  // ── TC-ORG-001-31 ─────────────────────────────────────────────────────────
  test('[TC-ORG-001-31] GET with invalid/expired token — HTTP 401', async () => {
    const fakeToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature';

    for (const ep of PROTECTED_ENDPOINTS) {
      await test.step(`GET ${ep.path} (invalid token)`, async () => {
        const resp = await ctx.get(`${BASE}${ep.path}`, {
          headers: {
            Authorization:  `Bearer ${fakeToken}`,
            'X-Tenant-Id':  TENANT,
          },
        });

        trace({
          tcId: 'TC-ORG-001-31',
          endpoint: `GET ${ep.path}`,
          requestPayload: { note: 'invalid Bearer token' },
          response: { status: resp.status() },
          statusCode: resp.status(),
        });

        expect(
          resp.status(),
          `${ep.label}: expected 401 for invalid token, got ${resp.status()}`,
        ).toBe(401);
      });
    }
  });
});
