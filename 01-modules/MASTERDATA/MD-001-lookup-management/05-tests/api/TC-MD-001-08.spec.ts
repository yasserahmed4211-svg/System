/**
 * G8 — Lookup Consumption API
 * TC-048 → TC-054
 *
 * ⚠️  BUG-04 IMPACT: Redis is DOWN (docker compose shows no containers).
 *     Spring @Cacheable(cacheNames="lookupValues") fails before DB query when
 *     Redis is unreachable.  ALL /api/lookups/{key} requests will return 500.
 *     Every test in this suite is expected to FAIL due to BUG-04.
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

const BASE        = 'http://localhost:7272';
const CONSUMPTION = `${BASE}/api/lookups`;
const TENANT      = 'default';

// Seeded master lookup keys (V2__baseline_data.sql)
const KEY_VENDOR_TYPE  = 'VENDOR_TYPE';   // ID=22, has active details
const KEY_UOM          = 'UOM';           // ID=1,  has active details
const KEY_NONEXISTENT  = 'ZZZZZ_NONEXISTENT';

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

test.describe('G8 — Lookup Consumption API', () => {
  let ctx:   APIRequestContext;
  let token: string;

  test.beforeAll(async ({ playwright }) => {
    ctx   = await playwright.request.newContext({ baseURL: BASE });
    token = await getAdminToken(ctx);
  });

  test.afterAll(async () => {
    await ctx.dispose();
  });

  // ── TC-048 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → 500
  test('[TC-048] [BUG-04] GET /api/lookups/VENDOR_TYPE — expected 200 active-only; Redis DOWN returns 500', async () => {
    await test.step(`GET ${CONSUMPTION}/${KEY_VENDOR_TYPE}`, async () => {
      const resp = await ctx.get(`${CONSUMPTION}/${KEY_VENDOR_TYPE}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-048', endpoint: `GET /api/lookups/${KEY_VENDOR_TYPE}`, requestPayload: {}, response: body, statusCode: resp.status() });

      // Expected per spec: 200 with active-only values, ordered by sortOrder
      // Actual: 500 (BUG-04)
      expect(resp.status(), '[BUG-04] Redis DOWN — lookup consumption returns 500').toBe(200);
    });
  });

  // ── TC-049 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → 500
  test('[TC-049] [BUG-04] Lowercase key normalized — GET /api/lookups/vendor_type same as uppercase; Redis DOWN → 500', async () => {
    await test.step(`GET ${CONSUMPTION}/${KEY_VENDOR_TYPE.toLowerCase()}`, async () => {
      const resp = await ctx.get(`${CONSUMPTION}/${KEY_VENDOR_TYPE.toLowerCase()}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-049', endpoint: `GET /api/lookups/${KEY_VENDOR_TYPE.toLowerCase()}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status(), '[BUG-04] Redis DOWN — lowercase key normalization test returns 500').toBe(200);
    });
  });

  // ── TC-050 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → both calls return 500
  test('[TC-050] [BUG-04] Second call hits cache — same response; Redis DOWN → 500', async () => {
    await test.step('Two consecutive GET calls to same key', async () => {
      const resp1 = await ctx.get(`${CONSUMPTION}/${KEY_UOM}`, { headers: hdrs(token) });
      const body1 = await resp1.json().catch(() => ({}));

      const resp2 = await ctx.get(`${CONSUMPTION}/${KEY_UOM}`, { headers: hdrs(token) });
      const body2 = await resp2.json().catch(() => ({}));

      trace({ tcId: 'TC-050', endpoint: `GET /api/lookups/${KEY_UOM} x2`, requestPayload: {}, response: { call1: resp1.status(), call2: resp2.status() }, statusCode: resp1.status() });

      expect(resp1.status(), '[BUG-04] First call — Redis DOWN returns 500').toBe(200);
      expect(resp2.status(), '[BUG-04] Second call — Redis DOWN returns 500').toBe(200);

      // Both responses should be identical (cache)
      if (resp1.status() === 200 && resp2.status() === 200) {
        expect(JSON.stringify(body1)).toBe(JSON.stringify(body2));
      }
    });
  });

  // ── TC-051 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → 500
  test('[TC-051] [BUG-04] Inactive master lookup returns empty list; Redis DOWN → 500', async () => {
    // Deactivate UOM temporarily for this test
    const BASE_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
    const SEEDED_ID    = 1; // UOM
    const adminHdrs    = hdrs(token);

    // Deactivate master
    const deactResp = await ctx.put(`${BASE_LOOKUPS}/${SEEDED_ID}/toggle-active`, {
      data: { active: false }, headers: adminHdrs,
    });

    await test.step(`GET /api/lookups/${KEY_UOM} (master inactive)`, async () => {
      const resp = await ctx.get(`${CONSUMPTION}/${KEY_UOM}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-051', endpoint: `GET /api/lookups/${KEY_UOM} (inactive master)`, requestPayload: {}, response: body, statusCode: resp.status() });

      // Re-activate regardless of test outcome
      await ctx.put(`${BASE_LOOKUPS}/${SEEDED_ID}/toggle-active`, {
        data: { active: true }, headers: adminHdrs,
      }).catch(() => {});

      expect(resp.status(), '[BUG-04] Redis DOWN — inactive master test returns 500').toBe(200);
      if (resp.status() === 200) {
        expect(body.data).toEqual([]);
      }
    });
  });

  // ── TC-052 ─────────────────────────────────────────────────────────────────
  // No @PreAuthorize on consumption endpoint — any authenticated user can access
  // ⚠️ BUG-04: Redis DOWN → 500
  test('[TC-052] [BUG-04] No @PreAuthorize — any authenticated user can GET /api/lookups/{key}; Redis DOWN → 500', async () => {
    // Use a non-admin user: 'user' (ROLE_USER, seeded in V2)
    const userLoginResp = await ctx.post(`${BASE}/api/auth/login`, {
      data: { username: 'user', password: 'user123' },
      headers: { 'X-Tenant-Id': TENANT },
    });
    const userBody = await userLoginResp.json();
    const userToken = (userBody.data?.accessToken ?? userBody.accessToken) as string | undefined;

    if (!userToken) {
      // Fallback to admin token if 'user' credentials don't work
      console.warn('[TC-052] Could not get user token — using admin token as fallback');
    }

    const effectiveToken = userToken ?? token;

    await test.step(`GET /api/lookups/${KEY_UOM} as non-admin user`, async () => {
      const resp = await ctx.get(`${CONSUMPTION}/${KEY_UOM}`, { headers: hdrs(effectiveToken) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-052', endpoint: `GET /api/lookups/${KEY_UOM}`, requestPayload: {}, response: body, statusCode: resp.status() });

      // Must NOT return 403 (no @PreAuthorize check)
      // Expected: 200; Actual (BUG-04): 500
      expect(resp.status(), '[BUG-04] Redis DOWN — no-perm user consumption test returns 500').not.toBe(403);
      expect(resp.status(), '[BUG-04] Redis DOWN — expected 200 returns 500').toBe(200);
    });
  });

  // ── TC-053 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → 500
  test('[TC-053] [BUG-04] Results ordered by sortOrder ASC; Redis DOWN → 500', async () => {
    await test.step(`GET /api/lookups/${KEY_UOM} and verify sort order`, async () => {
      const resp = await ctx.get(`${CONSUMPTION}/${KEY_UOM}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-053', endpoint: `GET /api/lookups/${KEY_UOM}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status(), '[BUG-04] Redis DOWN — sort order verification returns 500').toBe(200);
      if (resp.status() === 200) {
        const items = body.data ?? [];
        for (let i = 1; i < items.length; i++) {
          expect(items[i].sortOrder).toBeGreaterThanOrEqual(items[i - 1].sortOrder);
        }
      }
    });
  });

  // ── TC-054 ─────────────────────────────────────────────────────────────────
  // ⚠️ BUG-04: Redis DOWN → 500 (before it can even attempt the DB lookup)
  test('[TC-054] [BUG-04] Unknown lookup key — expected empty list or 404; Redis DOWN → 500', async () => {
    await test.step(`GET /api/lookups/${KEY_NONEXISTENT}`, async () => {
      const resp = await ctx.get(`${CONSUMPTION}/${KEY_NONEXISTENT}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-054', endpoint: `GET /api/lookups/${KEY_NONEXISTENT}`, requestPayload: {}, response: body, statusCode: resp.status() });

      // Expected per spec: [] or 404
      // Actual (BUG-04): 500
      expect([200, 404]).toContain(resp.status());
    });
  });
});
