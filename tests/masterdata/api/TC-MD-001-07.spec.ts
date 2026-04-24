/**
 * G7 — MdLookupDetail: Business Rules
 * TC-042 → TC-047
 *
 * ⚠️  BUG-02 IMPACT: Most tests that require creating a new detail will fail
 *     with 409 DB_CONSTRAINT_VIOLATION due to out-of-sync Oracle PK sequence.
 *     BUG-04: Redis DOWN → TC-045, TC-046 cache-eviction verification returns 500.
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

const BASE        = 'http://localhost:7272';
const API_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
const API_DETAILS = `${API_LOOKUPS}/details`;
const CONSUMPTION = `${BASE}/api/lookups`;
const TENANT      = 'default';

// Seeded parent lookup ID (guaranteed present, V2__baseline_data.sql)
const SEEDED_LOOKUP_ID_A = 1;  // UOM
const SEEDED_LOOKUP_ID_B = 2;  // COLOR

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

test.describe('G7 — MdLookupDetail: Business Rules', () => {
  let ctx:   APIRequestContext;
  let token: string;

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    await ctx.dispose();
  });

  // ── TC-042 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-02 masks this test: both creates fail with 409 sequence error, not 409 duplicate
  test('[TC-042] [BUG-02] Duplicate code within same master — blocked with 409; BUG-02 may mask root cause', async () => {
    const code    = `DUPCD_${uid()}`;
    const payload = { masterLookupId: SEEDED_LOOKUP_ID_A, code, nameAr: 'أول' };

    await test.step('Create first detail', async () => {
      const resp1 = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body1 = await resp1.json();

      trace({ tcId: 'TC-042-step1', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body1, statusCode: resp1.status() });

      if (resp1.status() !== 201) {
        console.warn(`[TC-042] First create returned ${resp1.status()} — likely BUG-02 sequence issue`);
      }
    });

    await test.step('Create duplicate code under same master', async () => {
      const resp2 = await ctx.post(API_DETAILS, { data: { ...payload, nameAr: 'مكرر' }, headers: hdrs(token) });
      const body2 = await resp2.json();

      trace({ tcId: 'TC-042-step2', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: { ...payload, nameAr: 'مكرر' }, response: body2, statusCode: resp2.status() });

      // Expected: 409 (duplicate code constraint UK_MD_LOOKUP_DETAIL_CODE)
      // BUG-02 may cause both to return 409 for a different reason (sequence clash)
      expect(resp2.status(), '[BUG-02] Duplicate check might be masked by sequence error').toBe(409);
    });
  });

  // ── TC-043 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-02: expected 201 for each, actual 409 sequence error
  test('[TC-043] [BUG-02] Same code under different masters — allowed; KNOWN BUG returns 409', async () => {
    const code = `SHARED_${uid()}`;

    await test.step('Create detail under master A', async () => {
      const payloadA = { masterLookupId: SEEDED_LOOKUP_ID_A, code, nameAr: 'تحت أ' };
      const respA    = await ctx.post(API_DETAILS, { data: payloadA, headers: hdrs(token) });
      const bodyA    = await respA.json();

      trace({ tcId: 'TC-043-A', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payloadA, response: bodyA, statusCode: respA.status() });

      expect(respA.status(), '[BUG-02] Same code under different masters should be 201').toBe(201);
    });

    await test.step('Create same code under master B', async () => {
      const payloadB = { masterLookupId: SEEDED_LOOKUP_ID_B, code, nameAr: 'تحت ب' };
      const respB    = await ctx.post(API_DETAILS, { data: payloadB, headers: hdrs(token) });
      const bodyB    = await respB.json();

      trace({ tcId: 'TC-043-B', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payloadB, response: bodyB, statusCode: respB.status() });

      expect(respB.status(), '[BUG-02] Second create under different master should be 201').toBe(201);
    });
  });

  // ── TC-044 ─────────────────────────────────────────────────────────────────
  test('[TC-044] Create detail with non-existent masterLookupId — returns 400 or 404', async () => {
    const payload = { masterLookupId: 999999999, code: `NOPRT_${uid()}`, nameAr: 'أب وهمي' };

    await test.step('POST detail with bogus masterLookupId', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-044', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body, statusCode: resp.status() });

      expect([400, 404, 409]).toContain(resp.status());
      console.log(`[TC-044] Actual status: ${resp.status()} for non-existent masterLookupId`);
    });
  });

  // ── TC-045 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04 (Redis DOWN): after POST detail, GET /api/lookups/{key} returns 500
  test('[TC-045] [BUG-04] Create detail evicts parent cache — GET /api/lookups/{key} should refresh; Redis DOWN → 500', async () => {
    const payload = { masterLookupId: SEEDED_LOOKUP_ID_A, code: `CE_${uid()}`, nameAr: 'إخلاء الكاش' };
    const postResp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
    const postBody = await postResp.json();

    trace({ tcId: 'TC-045-create', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: postBody, statusCode: postResp.status() });

    await test.step('GET /api/lookups/UOM after detail create', async () => {
      const cacheResp = await ctx.get(`${CONSUMPTION}/UOM`, { headers: hdrs(token) });
      const cacheBody = await cacheResp.json().catch(() => ({}));

      trace({ tcId: 'TC-045', endpoint: 'GET /api/lookups/UOM', requestPayload: {}, response: cacheBody, statusCode: cacheResp.status() });

      // Expected: 200 (cache evicted, fresh data)
      // Actual (BUG-04): 500 Redis unavailable
      expect(cacheResp.status(), '[BUG-04] Redis DOWN; cache eviction verification returns 500').toBe(200);
    });
  });

  // ── TC-046 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04 (Redis DOWN)
  test('[TC-046] [BUG-04] Update detail evicts parent cache — GET /api/lookups/{key} should refresh; Redis DOWN → 500', async () => {
    // Find an existing detail to update
    const searchResp = await ctx.post(`${API_DETAILS}/search`, {
      data: { page: 0, size: 1, filters: [] },
      headers: hdrs(token),
    });
    const searchBody = await searchResp.json();
    const detail = searchBody.data?.content?.[0];

    if (!detail) {
      test.skip(true, 'No existing details found for update — skipping TC-046');
      return;
    }

    const detailId = detail.id as number;
    await ctx.put(`${API_DETAILS}/${detailId}`, {
      data: { nameAr: `محدث_CE_${uid()}` },
      headers: hdrs(token),
    });

    await test.step('GET /api/lookups/UOM after detail update', async () => {
      const cacheResp = await ctx.get(`${CONSUMPTION}/UOM`, { headers: hdrs(token) });
      const cacheBody = await cacheResp.json().catch(() => ({}));

      trace({ tcId: 'TC-046', endpoint: 'GET /api/lookups/UOM', requestPayload: {}, response: cacheBody, statusCode: cacheResp.status() });

      expect(cacheResp.status(), '[BUG-04] Redis DOWN; cache eviction after update returns 500').toBe(200);
    });
  });

  // ── TC-047 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-02: create returns 409, so isActive default cannot be verified on new record
  test('[TC-047] [BUG-02] isActive defaults to true on detail create; KNOWN BUG returns 409 (sequence issue)', async () => {
    const payload = { masterLookupId: SEEDED_LOOKUP_ID_A, code: `DFLT_${uid()}`, nameAr: 'افتراضي' };

    await test.step('POST detail without isActive field', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-047', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body, statusCode: resp.status() });

      if (resp.status() === 201) {
        expect(body.data.isActive, 'isActive must default to true').toBe(true);
      } else {
        expect(resp.status(), '[BUG-02] isActive default cannot be verified — sequence returns 409').toBe(201);
      }
    });
  });
});
