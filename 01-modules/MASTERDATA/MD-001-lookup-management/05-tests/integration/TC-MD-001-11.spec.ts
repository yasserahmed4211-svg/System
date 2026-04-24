/**
 * G11 — Cache Invalidation (Full-Cycle)
 * TC-066 → TC-070
 *
 * ⚠️  BUG-04: Redis is DOWN.  ALL cache-invalidation tests FAIL with HTTP 500
 *             because Spring @Cacheable requires a live Redis connection.
 *             Tests run as designed; all are expected to FAIL — failures are
 *             classified as BUG-04 in the TEST_EXECUTION_REPORT.
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API (Cache)
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

const BASE        = 'http://localhost:7272';
const API_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
const API_DETAILS = `${API_LOOKUPS}/details`;
const CONSUMPTION = `${BASE}/api/lookups`;
const TENANT      = 'default';

// Seeded lookup guaranteed present
const SEEDED_KEY = 'UOM';
const SEEDED_ID  = 1;

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

/** Attempt to read the consumption endpoint and return status + data */
async function consumeKey(ctx: APIRequestContext, key: string, token: string) {
  const resp = await ctx.get(`${CONSUMPTION}/${key}`, { headers: hdrs(token) });
  const body = await resp.json().catch(() => ({}));
  return { status: resp.status(), data: body?.data ?? null };
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G11 — Cache Invalidation', () => {
  let ctx:   APIRequestContext;
  let token: string;
  const createdIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    for (const id of createdIds) {
      await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-066 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → consumption call returns 500
  test('[TC-066] [BUG-04] Cache evicted on master create — GET /api/lookups re-hits DB; Redis DOWN → 500', async () => {
    const key = `CI66_${uid()}`;
    const payload = { lookupKey: key, lookupName: 'إخلاء الإنشاء' };

    await test.step('POST new master lookup', async () => {
      const cr   = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(token) });
      const body = await cr.json();
      if (body.data?.id) createdIds.push(body.data.id);

      trace({ tcId: 'TC-066-create', endpoint: 'POST /api/masterdata/master-lookups', requestPayload: payload, response: body, statusCode: cr.status() });
    });

    await test.step(`GET /api/lookups/${key} — should return fresh data (not stale/500)`, async () => {
      const { status, data } = await consumeKey(ctx, key.toUpperCase(), token);

      trace({ tcId: 'TC-066', endpoint: `GET /api/lookups/${key.toUpperCase()}`, requestPayload: {}, response: data, statusCode: status });

      // Expected: 200 with empty list (no details yet) — cache miss triggers fresh DB read
      // Actual (BUG-04): 500
      expect(status, '[BUG-04] Redis DOWN — cache eviction on create returns 500').toBe(200);
    });
  });

  // ── TC-067 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04 + BUG-02
  test('[TC-067] [BUG-04] Cache evicted on detail create — parent key re-hits DB; Redis DOWN → 500', async () => {
    await test.step('POST new detail under seeded lookup', async () => {
      const detailPayload = {
        masterLookupId: SEEDED_ID,
        code:           `CE67_${uid()}`,
        nameAr:         'إخلاء التفصيل',
      };
      const dr   = await ctx.post(API_DETAILS, { data: detailPayload, headers: hdrs(token) });
      const dBody = await dr.json();

      trace({ tcId: 'TC-067-create', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: detailPayload, response: dBody, statusCode: dr.status() });

      console.log(`[TC-067] Detail create returned ${dr.status()} (expected 201, BUG-02 may return 409)`);
    });

    await test.step(`GET /api/lookups/${SEEDED_KEY} — cache evicted by detail create`, async () => {
      const { status, data } = await consumeKey(ctx, SEEDED_KEY, token);

      trace({ tcId: 'TC-067', endpoint: `GET /api/lookups/${SEEDED_KEY}`, requestPayload: {}, response: data, statusCode: status });

      expect(status, '[BUG-04] Redis DOWN — cache eviction on detail create returns 500').toBe(200);
    });
  });

  // ── TC-068 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04
  test('[TC-068] [BUG-04] Cache evicted on detail update — parent key re-hits DB; Redis DOWN → 500', async () => {
    // Get any existing detail
    const searchResp = await ctx.post(`${API_DETAILS}/search`, {
      data: { page: 0, size: 1, filters: [] },
      headers: hdrs(token),
    });
    const searchBody = await searchResp.json();
    const detail = searchBody.data?.content?.[0];

    if (!detail) {
      test.skip(true, 'No existing details found — skipping TC-068');
      return;
    }

    await test.step(`PUT /api/masterdata/master-lookups/details/${detail.id}`, async () => {
      await ctx.put(`${API_DETAILS}/${detail.id}`, {
        data:    { nameAr: `محدث_${uid()}` },
        headers: hdrs(token),
      });
    });

    await test.step(`GET /api/lookups/${SEEDED_KEY} — cache evicted by detail update`, async () => {
      const { status, data } = await consumeKey(ctx, SEEDED_KEY, token);

      trace({ tcId: 'TC-068', endpoint: `GET /api/lookups/${SEEDED_KEY}`, requestPayload: {}, response: data, statusCode: status });

      expect(status, '[BUG-04] Redis DOWN — cache eviction on detail update returns 500').toBe(200);
    });
  });

  // ── TC-069 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04
  test('[TC-069] [BUG-04] Cache key is case-insensitive — lowercase + uppercase same cache entry; Redis DOWN → 500', async () => {
    await test.step('GET /api/lookups/uom (lowercase)', async () => {
      const { status: s1, data: d1 } = await consumeKey(ctx, 'uom', token);

      trace({ tcId: 'TC-069-lower', endpoint: 'GET /api/lookups/uom', requestPayload: {}, response: d1, statusCode: s1 });

      expect(s1, '[BUG-04] Redis DOWN — lowercase key test returns 500').toBe(200);
    });

    await test.step('GET /api/lookups/UOM (uppercase)', async () => {
      const { status: s2, data: d2 } = await consumeKey(ctx, 'UOM', token);

      trace({ tcId: 'TC-069-upper', endpoint: 'GET /api/lookups/UOM', requestPayload: {}, response: d2, statusCode: s2 });

      expect(s2, '[BUG-04] Redis DOWN — uppercase key test returns 500').toBe(200);
    });
  });

  // ── TC-070 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04
  test('[TC-070] [BUG-04] Cache evicted on toggle — next GET re-hits DB; Redis DOWN → 500', async () => {
    // Create a fresh lookup to toggle safely
    const key = `TOG70_${uid()}`;
    const cr  = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'تبديل الكاش' }, headers: hdrs(token) });
    const b   = await cr.json();
    const id  = b.data?.id as number;
    if (id) createdIds.push(id);

    await test.step(`PUT /api/masterdata/master-lookups/${id}/toggle-active { active: false }`, async () => {
      await ctx.put(`${API_LOOKUPS}/${id}/toggle-active`, {
        data:    { active: false },
        headers: hdrs(token),
      });
    });

    await test.step(`GET /api/lookups/${key.toUpperCase()} — cache evicted by toggle`, async () => {
      const { status, data } = await consumeKey(ctx, key.toUpperCase(), token);

      trace({ tcId: 'TC-070', endpoint: `GET /api/lookups/${key.toUpperCase()}`, requestPayload: {}, response: data, statusCode: status });

      // After toggle to inactive: expected 200 with empty list
      // Actual (BUG-04): 500
      expect(status, '[BUG-04] Redis DOWN — cache eviction on toggle returns 500').toBe(200);
    });
  });
});
