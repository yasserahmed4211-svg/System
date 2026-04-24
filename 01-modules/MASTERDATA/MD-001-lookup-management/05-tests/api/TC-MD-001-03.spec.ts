/**
 * G3 — MdMasterLookup: Search & Filter
 * TC-019 → TC-023
 *
 * Known issues documented:
 *   BUG-01 — TC-021: isActive Boolean filter crashes SpecBuilder (500)
 *   OBS-01 — TC-022: invalid sortField returns 422 (not 400 or 200)
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

const BASE        = 'http://localhost:7272';
const API_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
const TENANT      = 'default';

function uid(): string {
  return `T${Date.now().toString(36).toUpperCase()}_${Math.random().toString(36).slice(2, 5).toUpperCase()}`;
}

function hdrs(token: string): Record<string, string> {
  return {
    Authorization:  `Bearer ${token}`,
    'X-Tenant-Id':  TENANT,
    'Content-Type': 'application/json',
  };
}

async function getAdminToken(ctx: APIRequestContext): Promise<string> {
  const resp = await ctx.post(`${BASE}/api/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
    headers: { 'X-Tenant-Id': TENANT },
  });
  const body = await resp.json();
  return (body.data?.accessToken ?? body.accessToken) as string;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G3 — MdMasterLookup: Search & Filter', () => {
  let ctx:   APIRequestContext;
  let token: string;
  const createdIds: number[] = [];

  // Unique prefix to isolate our test records from seed data
  const SEARCH_PREFIX = `SRCH_${Date.now().toString(36).toUpperCase()}`;

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);

    // Seed 10 records for TC-023 pagination test
    for (let i = 0; i < 10; i++) {
      const resp = await ctx.post(API_LOOKUPS, {
        data: { lookupKey: `${SEARCH_PREFIX}_${i}`, lookupName: `بحث ${i}` },
        headers: hdrs(token),
      });
      const body = await resp.json();
      if (body.data?.id) createdIds.push(body.data.id);
    }
  });

  test.afterAll(async () => {
    for (const id of createdIds) {
      await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-019 ─────────────────────────────────────────────────────────────────
  test('[TC-019] Search by lookupKey EQUALS — returns only matching record', async () => {
    const targetKey = `${SEARCH_PREFIX}_0`;
    const payload   = {
      page: 0, size: 20, sortField: 'createdAt', sortDirection: 'DESC',
      filters: [{ field: 'lookupKey', operator: 'EQUALS', value: targetKey }],
    };

    await test.step('POST /api/masterdata/master-lookups/search', async () => {
      const resp = await ctx.post(`${API_LOOKUPS}/search`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-019', endpoint: 'POST /api/masterdata/master-lookups/search', requestPayload: payload, response: { totalElements: body.data?.totalElements, firstKey: body.data?.content?.[0]?.lookupKey }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      const content = body.data?.content ?? [];
      expect(content.length).toBeGreaterThanOrEqual(1);
      expect(content.every((r: { lookupKey: string }) => r.lookupKey === targetKey.toUpperCase())).toBe(true);
    });
  });

  // ── TC-020 ─────────────────────────────────────────────────────────────────
  test('[TC-020] Search by lookupKey CONTAINS — returns all keys containing substring', async () => {
    const payload = {
      page: 0, size: 50, sortField: 'createdAt', sortDirection: 'DESC',
      filters: [{ field: 'lookupKey', operator: 'CONTAINS', value: SEARCH_PREFIX }],
    };

    await test.step('POST /api/masterdata/master-lookups/search with CONTAINS', async () => {
      const resp = await ctx.post(`${API_LOOKUPS}/search`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-020', endpoint: 'POST /api/masterdata/master-lookups/search', requestPayload: payload, response: { totalElements: body.data?.totalElements }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      const content = body.data?.content ?? [];
      expect(content.length).toBeGreaterThanOrEqual(10);
      content.forEach((r: { lookupKey: string }) => {
        expect(r.lookupKey).toContain(SEARCH_PREFIX);
      });
    });
  });

  // ── TC-021 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-01: SpecBuilder cannot handle Boolean field — will return HTTP 500
  test('[TC-021] [BUG-01] Search filter isActive EQUALS false — expected 200; KNOWN BUG returns 500', async () => {
    const payload = {
      page: 0, size: 20,
      filters: [{ field: 'isActive', operator: 'EQUALS', value: 'false' }],
    };

    await test.step('POST search with isActive filter', async () => {
      const resp = await ctx.post(`${API_LOOKUPS}/search`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-021', endpoint: 'POST /api/masterdata/master-lookups/search', requestPayload: payload, response: body, statusCode: resp.status() });

      // Expected per spec: 200
      // Actual per BUG-01: 500 (SpecBuilder Boolean cast error)
      expect(resp.status(), '[BUG-01] isActive filter returns 500 due to SpecBuilder Boolean type mismatch').toBe(200);
    });
  });

  // ── TC-022 ─────────────────────────────────────────────────────────────────
  // OBS-01: invalid sortField returns 422 — test documents actual behavior
  test('[TC-022] [OBS-01] Search with unsupported filter field — expected 400/422 (actual: 422)', async () => {
    const payload = {
      page: 0, size: 20,
      sortField:     'nonExistentField',
      sortDirection: 'ASC',
      filters:       [],
    };

    await test.step('POST search with invalid sortField', async () => {
      const resp = await ctx.post(`${API_LOOKUPS}/search`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-022', endpoint: 'POST /api/masterdata/master-lookups/search', requestPayload: payload, response: body, statusCode: resp.status() });

      // OBS-01: actual returns 422
      // Documenting actual behavior; assertion allows 400 or 422
      expect([400, 422]).toContain(resp.status());
    });
  });

  // ── TC-023 ─────────────────────────────────────────────────────────────────
  test('[TC-023] Search returns paginated result with totalElements field', async () => {
    const payload = {
      page: 0, size: 10, sortField: 'createdAt', sortDirection: 'DESC',
      filters: [{ field: 'lookupKey', operator: 'CONTAINS', value: SEARCH_PREFIX }],
    };

    await test.step('POST search with page=0, size=10', async () => {
      const resp = await ctx.post(`${API_LOOKUPS}/search`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-023', endpoint: 'POST /api/masterdata/master-lookups/search', requestPayload: payload, response: { totalElements: body.data?.totalElements, pageSize: body.data?.content?.length }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(typeof body.data.totalElements).toBe('number');
      expect(body.data.totalElements).toBeGreaterThanOrEqual(10);
      expect(body.data.content.length).toBeLessThanOrEqual(10);
    });
  });
});
