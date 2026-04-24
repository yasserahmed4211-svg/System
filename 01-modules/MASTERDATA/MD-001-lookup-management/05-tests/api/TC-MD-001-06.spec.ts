/**
 * G6 — MdLookupDetail: CRUD
 * TC-033 → TC-041
 *
 * ⚠️  BUG-02 IMPACT: MD_LOOKUP_DETAIL_SEQ is out of sync.
 *     ALL POST /details requests will return 409 DB_CONSTRAINT_VIOLATION
 *     until the Oracle sequence is reset.
 *     Tests TC-033, TC-034 validation pass, TC-036, TC-037 (update) may work if
 *     seed details exist; creation tests are expected to fail with 409.
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';
import { validateInsert, validateUpdate } from '../db/db.helper';

const BASE        = 'http://localhost:7272';
const API_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
const API_DETAILS = `${API_LOOKUPS}/details`;
const TENANT      = 'default';

// Seeded master lookup from V2__baseline_data.sql
// ID=1  LOOKUP_KEY='UOM'   — guaranteed to exist
// ID=22 LOOKUP_KEY='GL_ACCOUNT_TYPE' — guaranteed to exist
const SEEDED_LOOKUP_ID     = 1;
const SEEDED_LOOKUP_KEY    = 'UOM';

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

test.describe('G6 — MdLookupDetail: CRUD', () => {
  let ctx:       APIRequestContext;
  let token:     string;
  let parentId:  number;
  const createdDetailIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);

    // Create a dedicated parent lookup for this test suite
    const cr = await ctx.post(API_LOOKUPS, {
      data: { lookupKey: `G6P_${uid()}`, lookupName: 'أب G6' },
      headers: hdrs(token),
    });
    const b = await cr.json();
    parentId = b.data?.id as number;
  });

  test.afterAll(async () => {
    // Try to delete created details then parent
    for (const did of createdDetailIds) {
      await ctx.delete(`${API_DETAILS}/${did}`, { headers: hdrs(token) }).catch(() => {});
    }
    if (parentId) {
      await ctx.delete(`${API_LOOKUPS}/${parentId}`, { headers: hdrs(token) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-033 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-02: expected 201, actual 409 (sequence out of sync)
  test('[TC-033] [BUG-02] Create lookup detail — expected 201; KNOWN BUG returns 409 (sequence out of sync)', async () => {
    const payload = {
      masterLookupId: parentId,
      code:           `CODE_${uid()}`,
      nameAr:         'تفصيل جديد',
      isActive:       true,
    };

    await test.step('POST /api/masterdata/master-lookups/details', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-033', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body, statusCode: resp.status() });

      if (resp.status() === 201) {
        expect(body.data.sortOrder).toBe(0);
        createdDetailIds.push(body.data.id);
      } else {
        // BUG-02: document the actual outcome
        expect(resp.status(), '[BUG-02] sequence out of sync — detail POST returns 409').toBe(201);
      }
    });
  });

  // ── TC-034 ─────────────────────────────────────────────────────────────────
  test('[TC-034] Create detail — missing masterLookupId returns HTTP 400', async () => {
    const payload = { code: `NOMID_${uid()}`, nameAr: 'بدون معرف' };

    await test.step('POST detail without masterLookupId', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-034', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(400);
    });
  });

  // ── TC-035 ─────────────────────────────────────────────────────────────────
  test('[TC-035] Create detail — missing nameAr returns HTTP 400', async () => {
    const payload = { masterLookupId: parentId, code: `NONAME_${uid()}` };

    await test.step('POST detail without nameAr', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-035', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(400);
    });
  });

  // ── TC-036 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-02: expected 201 with sortOrder=0, actual 409
  test('[TC-036] [BUG-02] Create detail — sortOrder defaults to 0; KNOWN BUG returns 409', async () => {
    const payload = { masterLookupId: parentId, code: `SO_${uid()}`, nameAr: 'ترتيب افتراضي' };

    await test.step('POST detail without sortOrder', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-036', endpoint: 'POST /api/masterdata/master-lookups/details', requestPayload: payload, response: body, statusCode: resp.status() });

      if (resp.status() === 201) {
        expect(body.data.sortOrder).toBe(0);
        createdDetailIds.push(body.data.id);
      } else {
        expect(resp.status(), '[BUG-02] sequence out of sync — sortOrder default test returns 409').toBe(201);
      }
    });
  });

  // ── TC-037 ─────────────────────────────────────────────────────────────────
  test('[TC-037] Update detail — nameAr and sortOrder updated; HTTP 200', async () => {
    // Find an existing seeded detail in UOM (ID=1)
    const searchResp = await ctx.post(`${API_DETAILS}/search`, {
      data: { page: 0, size: 5, filters: [{ field: 'masterLookupId', operator: 'EQUALS', value: String(SEEDED_LOOKUP_ID) }] },
      headers: hdrs(token),
    });
    const searchBody = await searchResp.json();
    const seededDetail = searchBody.data?.content?.[0];

    if (!seededDetail) {
      test.skip(true, `No seeded details found for masterLookupId=${SEEDED_LOOKUP_ID} — skipping TC-037`);
      return;
    }

    const detailId = seededDetail.id as number;
    const updatePayload = { nameAr: `محدث_${uid()}`, sortOrder: 99 };

    await test.step(`PUT /api/masterdata/master-lookups/details/${detailId}`, async () => {
      const resp = await ctx.put(`${API_DETAILS}/${detailId}`, { data: updatePayload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-037', endpoint: `PUT /api/masterdata/master-lookups/details/${detailId}`, requestPayload: updatePayload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.nameAr).toBe(updatePayload.nameAr);
      expect(body.data.sortOrder).toBe(updatePayload.sortOrder);

      const dbResult = await validateUpdate(ctx, API_DETAILS, detailId, { nameAr: updatePayload.nameAr, sortOrder: 99 }, hdrs(token));
      expect(dbResult.validated, `DB update validation: ${JSON.stringify(dbResult.evidence)}`).toBe(true);
    });
  });

  // ── TC-038 ─────────────────────────────────────────────────────────────────
  test('[TC-038] Update detail — code field is immutable (unchanged after PUT)', async () => {
    const searchResp = await ctx.post(`${API_DETAILS}/search`, {
      data: { page: 0, size: 5, filters: [{ field: 'masterLookupId', operator: 'EQUALS', value: String(SEEDED_LOOKUP_ID) }] },
      headers: hdrs(token),
    });
    const searchBody = await searchResp.json();
    const seededDetail = searchBody.data?.content?.[0];

    if (!seededDetail) {
      test.skip(true, `No seeded details for masterLookupId=${SEEDED_LOOKUP_ID} — skipping TC-038`);
      return;
    }

    const detailId    = seededDetail.id as number;
    const originalCode = seededDetail.code as string;
    const payload     = { code: `CHANGED_${uid()}`, nameAr: seededDetail.nameAr };

    await test.step(`PUT /api/masterdata/master-lookups/details/${detailId} with changed code`, async () => {
      const resp = await ctx.put(`${API_DETAILS}/${detailId}`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-038', endpoint: `PUT /api/masterdata/master-lookups/details/${detailId}`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data.code).toBe(originalCode); // code must not change
    });
  });

  // ── TC-039 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-03: SpecBuilder cannot filter by relationship field masterLookupId → 500
  test('[TC-039] [BUG-03] Search details — default sort sortOrder ASC; filter by masterLookupId may fail (BUG-03)', async () => {
    const payload = {
      page: 0, size: 20,
      sortField:     'sortOrder',
      sortDirection: 'ASC',
      filters:       [{ field: 'masterLookupId', operator: 'EQUALS', value: String(SEEDED_LOOKUP_ID) }],
    };

    await test.step('POST /api/masterdata/master-lookups/details/search with masterLookupId filter', async () => {
      const resp = await ctx.post(`${API_DETAILS}/search`, { data: payload, headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-039', endpoint: 'POST /api/masterdata/master-lookups/details/search', requestPayload: payload, response: { status: resp.status(), totalElements: body.data?.totalElements }, statusCode: resp.status() });

      if (resp.status() === 500) {
        // BUG-03: document and fail
        expect(resp.status(), '[BUG-03] SpecBuilder cannot filter by masterLookupId relationship field → 500').toBe(200);
      } else {
        expect(resp.status()).toBe(200);
        // Verify sort order is ascending
        const content = body.data?.content ?? [];
        for (let i = 1; i < content.length; i++) {
          expect(content[i].sortOrder).toBeGreaterThanOrEqual(content[i - 1].sortOrder);
        }
      }
    });
  });

  // ── TC-040 ─────────────────────────────────────────────────────────────────
  test('[TC-040] Get detail options by lookupKey (active=true) — returns active-only details', async () => {
    await test.step(`GET /api/masterdata/master-lookups/details/options/${SEEDED_LOOKUP_KEY}?active=true`, async () => {
      const resp = await ctx.get(`${API_DETAILS}/options/${SEEDED_LOOKUP_KEY}?active=true`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-040', endpoint: `GET /api/masterdata/master-lookups/details/options/${SEEDED_LOOKUP_KEY}?active=true`, requestPayload: {}, response: { count: body.data?.length ?? 0 }, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      const items = body.data ?? [];
      items.forEach((item: { isActive: boolean }) => {
        expect(item.isActive, 'All returned options must be active').toBe(true);
      });
    });
  });

  // ── TC-041 ─────────────────────────────────────────────────────────────────
  test('[TC-041] Get detail usage info — returns usage fields', async () => {
    const searchResp = await ctx.post(`${API_DETAILS}/search`, {
      data: { page: 0, size: 1, filters: [] },
      headers: hdrs(token),
    });
    const searchBody = await searchResp.json();
    const someDetail = searchBody.data?.content?.[0];

    if (!someDetail) {
      test.skip(true, 'No details available to test usage endpoint — skipping TC-041');
      return;
    }

    const detailId = someDetail.id as number;

    await test.step(`GET /api/masterdata/master-lookups/details/${detailId}/usage`, async () => {
      const resp = await ctx.get(`${API_DETAILS}/${detailId}/usage`, { headers: hdrs(token) });
      const body = await resp.json();

      trace({ tcId: 'TC-041', endpoint: `GET /api/masterdata/master-lookups/details/${detailId}/usage`, requestPayload: {}, response: body.data, statusCode: resp.status() });

      expect(resp.status()).toBe(200);
      expect(body.data).toBeDefined();
    });
  });
});
