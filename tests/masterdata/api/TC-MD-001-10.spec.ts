/**
 * G10 — Security & Permissions
 * TC-060 → TC-065
 *
 * Permission model (from V3__seed_missing_permissions.sql / SecurityPermissions.java):
 *   MASTER_LOOKUP_CREATE / MASTER_LOOKUP_VIEW / MASTER_LOOKUP_UPDATE / MASTER_LOOKUP_DELETE
 *   are granted only to ROLE_ADMIN.  ROLE_USER has no MASTER_LOOKUP_* permissions.
 *
 * No-permission user strategy:
 *   Create a fresh user via POST /api/users → auto-assigned ROLE_USER → no MASTER_LOOKUP_*
 *   Login as that user → token with no masterdata permissions
 *
 * TC-064: Detail service reuses MASTER_LOOKUP_* permissions (not a separate LOOKUP_DETAIL_*),
 *         so a user with MASTER_LOOKUP_CREATE can POST /details.
 *         ⚠️ BUG-02 will cause 409 (sequence) even if permission check passes.
 *
 * Source: registry-masterdata.md v1.1.0 + TC-MASTERDATA.md
 * Layer : Backend API (Security)
 * Author: Playwright Expert Agent (auto-generated 2026-04-18)
 */

import { test, expect, APIRequestContext } from '@playwright/test';
import { trace } from '../traceability';

const BASE        = 'http://localhost:7272';
const API_LOOKUPS = `${BASE}/api/masterdata/master-lookups`;
const API_DETAILS = `${API_LOOKUPS}/details`;
const TENANT      = 'default';

const SEEDED_LOOKUP_ID = 1; // UOM — guaranteed present

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

/** Create a fresh user with no explicit role assignments (gets ROLE_USER default, no MASTER_LOOKUP perms) */
async function createNoPermUser(ctx: APIRequestContext, adminToken: string): Promise<string> {
  const username = `noperm_${uid()}`;
  const password = 'Pass@1234';

  const createResp = await ctx.post(`${BASE}/api/users`, {
    data: { username, password },
    headers: hdrs(adminToken),
  });
  const createBody = await createResp.json();
  console.log(`[G10] Created no-perm user: ${username}, createStatus=${createResp.status()}, id=${createBody.data?.id}`);

  const loginResp = await ctx.post(`${BASE}/api/auth/login`, {
    data: { username, password },
    headers: { 'X-Tenant-Id': TENANT },
  });
  const loginBody = await loginResp.json();
  const noPermToken = (loginBody.data?.accessToken ?? loginBody.accessToken) as string;

  if (!noPermToken) {
    throw new Error(`[G10] Failed to obtain token for no-perm user ${username}`);
  }
  return noPermToken;
}

// ─────────────────────────────────────────────────────────────────────────────

test.describe('G10 — Security & Permissions', () => {
  let ctx:          APIRequestContext;
  let adminToken:   string;
  let noPermToken:  string;
  const createdAdminIds: number[] = [];

  test.beforeAll(async ({ playwright }) => {
    ctx        = await playwright.request.newContext({ baseURL: BASE });
    adminToken = await getAdminToken(ctx);
    noPermToken = await createNoPermUser(ctx, adminToken);
  });

  test.afterAll(async () => {
    for (const id of createdAdminIds) {
      await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(adminToken) }).catch(() => {});
    }
    await ctx.dispose();
  });

  // ── TC-060 ─────────────────────────────────────────────────────────────────
  test('[TC-060] No MASTER_LOOKUP_CREATE permission — POST /master-lookups returns 403', async () => {
    const payload = { lookupKey: `NOPERM_${uid()}`, lookupName: 'غير مصرح' };

    await test.step('POST as no-permission user', async () => {
      const resp = await ctx.post(API_LOOKUPS, { data: payload, headers: hdrs(noPermToken) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-060', endpoint: 'POST /api/masterdata/master-lookups (no MASTER_LOOKUP_CREATE)', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected 403 Forbidden — no CREATE permission').toBe(403);
    });
  });

  // ── TC-061 ─────────────────────────────────────────────────────────────────
  test('[TC-061] No MASTER_LOOKUP_VIEW permission — GET /master-lookups/{id} returns 403', async () => {
    await test.step(`GET /api/masterdata/master-lookups/${SEEDED_LOOKUP_ID} as no-permission user`, async () => {
      const resp = await ctx.get(`${API_LOOKUPS}/${SEEDED_LOOKUP_ID}`, { headers: hdrs(noPermToken) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-061', endpoint: `GET /api/masterdata/master-lookups/${SEEDED_LOOKUP_ID} (no MASTER_LOOKUP_VIEW)`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected 403 Forbidden — no VIEW permission').toBe(403);
    });
  });

  // ── TC-062 ─────────────────────────────────────────────────────────────────
  test('[TC-062] No MASTER_LOOKUP_UPDATE permission — PUT /master-lookups/{id} returns 403', async () => {
    const payload = { lookupName: 'محاولة تحديث' };

    await test.step(`PUT /api/masterdata/master-lookups/${SEEDED_LOOKUP_ID} as no-permission user`, async () => {
      const resp = await ctx.put(`${API_LOOKUPS}/${SEEDED_LOOKUP_ID}`, { data: payload, headers: hdrs(noPermToken) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-062', endpoint: `PUT /api/masterdata/master-lookups/${SEEDED_LOOKUP_ID} (no MASTER_LOOKUP_UPDATE)`, requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected 403 Forbidden — no UPDATE permission').toBe(403);
    });
  });

  // ── TC-063 ─────────────────────────────────────────────────────────────────
  test('[TC-063] No MASTER_LOOKUP_DELETE permission — DELETE /master-lookups/{id} returns 403', async () => {
    // Create a disposable lookup with admin, then attempt delete as no-perm user
    const cr  = await ctx.post(API_LOOKUPS, { data: { lookupKey: `DELP_${uid()}`, lookupName: 'للحذف' }, headers: hdrs(adminToken) });
    const b   = await cr.json();
    const id  = b.data?.id as number;
    createdAdminIds.push(id); // cleanup in afterAll

    await test.step(`DELETE /api/masterdata/master-lookups/${id} as no-permission user`, async () => {
      const resp = await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(noPermToken) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-063', endpoint: `DELETE /api/masterdata/master-lookups/${id} (no MASTER_LOOKUP_DELETE)`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected 403 Forbidden — no DELETE permission').toBe(403);
    });
  });

  // ── TC-064 ─────────────────────────────────────────────────────────────────
  // Governance observation: detail service uses MASTER_LOOKUP_* permissions, not LOOKUP_DETAIL_*
  // Admin user (who has MASTER_LOOKUP_CREATE) should be able to POST /details
  // ⚠️ BUG-02: even if permission is granted, 409 expected due to sequence issue
  test('[TC-064] [BUG-02] Detail uses MASTER_LOOKUP_CREATE permission — POST /details as admin returns 201 (BUG-02 returns 409)', async () => {
    const payload = {
      masterLookupId: SEEDED_LOOKUP_ID,
      code:           `PERM_${uid()}`,
      nameAr:         'اختبار الصلاحية',
    };

    await test.step('POST /details as admin (has MASTER_LOOKUP_CREATE)', async () => {
      const resp = await ctx.post(API_DETAILS, { data: payload, headers: hdrs(adminToken) });
      const body = await resp.json();

      trace({ tcId: 'TC-064', endpoint: 'POST /api/masterdata/master-lookups/details (admin, MASTER_LOOKUP_CREATE)', requestPayload: payload, response: body, statusCode: resp.status() });

      // Must NOT be 403 (admin has the right permission)
      expect(resp.status(), 'Admin must not be blocked — 403 would mean permission misconfiguration').not.toBe(403);

      // Expected with correct sequence: 201
      // Actual (BUG-02): 409
      expect(resp.status(), '[BUG-02] sequence out of sync → detail POST returns 409').toBe(201);

      if (resp.status() === 201 && body.data?.id) {
        await ctx.delete(`${API_DETAILS}/${body.data.id}`, { headers: hdrs(adminToken) }).catch(() => {});
      }
    });
  });

  // ── TC-065 ─────────────────────────────────────────────────────────────────
  test('[TC-065] Unauthenticated request — POST /master-lookups without token returns 401', async () => {
    const payload = { lookupKey: `UNAUTH_${uid()}`, lookupName: 'غير مصادق' };

    await test.step('POST without Authorization header', async () => {
      const resp = await ctx.post(API_LOOKUPS, {
        data:    payload,
        headers: { 'X-Tenant-Id': TENANT, 'Content-Type': 'application/json' },
      });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-065', endpoint: 'POST /api/masterdata/master-lookups (no auth)', requestPayload: payload, response: body, statusCode: resp.status() });

      expect(resp.status(), 'Expected 401 Unauthorized — no Bearer token').toBe(401);
    });
  });
});
