/**
 * G5 — MdMasterLookup: Delete
 * TC-029 → TC-032
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

test.describe('G5 — MdMasterLookup: Delete', () => {
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

  // ── TC-029 ─────────────────────────────────────────────────────────────────
  test('[TC-029] Delete lookup with no details — HTTP 204', async () => {
    const key = `DEL_${uid()}`;
    const cr  = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'حذف نظيف' }, headers: hdrs(token) });
    const b   = await cr.json();
    const id  = b.data?.id as number;

    await test.step(`DELETE /api/masterdata/master-lookups/${id}`, async () => {
      const resp = await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) });

      trace({ tcId: 'TC-029', endpoint: `DELETE /api/masterdata/master-lookups/${id}`, requestPayload: {}, response: {}, statusCode: resp.status() });

      expect(resp.status()).toBe(204);

      // Verify gone
      const getResp = await ctx.get(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) });
      expect(getResp.status()).toBe(404);
    });
  });

  // ── TC-030 ─────────────────────────────────────────────────────────────────
  test('[TC-030] Delete blocked by active details — HTTP 409 MASTER_LOOKUP_DETAILS_EXIST', async () => {
    const key = `BLKD_${uid()}`;
    const cr  = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'محظور الحذف' }, headers: hdrs(token) });
    const b   = await cr.json();
    const id  = b.data?.id as number;
    createdIds.push(id);

    // Create active detail
    await ctx.post(API_DETAILS, {
      data: { masterLookupId: id, code: `AC_${uid()}`, nameAr: 'تفصيل نشط', isActive: true },
      headers: hdrs(token),
    });

    await test.step(`DELETE /api/masterdata/master-lookups/${id} (active detail exists)`, async () => {
      const resp = await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-030', endpoint: `DELETE /api/masterdata/master-lookups/${id}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(409);
    });
  });

  // ── TC-031 ─────────────────────────────────────────────────────────────────
  test('[TC-031] Delete blocked even by inactive-only details — HTTP 409', async () => {
    const key = `BLKI_${uid()}`;
    const cr  = await ctx.post(API_LOOKUPS, { data: { lookupKey: key, lookupName: 'محظور بتفاصيل غير نشطة' }, headers: hdrs(token) });
    const b   = await cr.json();
    const id  = b.data?.id as number;
    createdIds.push(id);

    // Create inactive-only detail
    await ctx.post(API_DETAILS, {
      data: { masterLookupId: id, code: `IN_${uid()}`, nameAr: 'تفصيل غير نشط', isActive: false },
      headers: hdrs(token),
    });

    await test.step(`DELETE /api/masterdata/master-lookups/${id} (inactive detail exists)`, async () => {
      const resp = await ctx.delete(`${API_LOOKUPS}/${id}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-031', endpoint: `DELETE /api/masterdata/master-lookups/${id}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(409);
    });
  });

  // ── TC-032 ─────────────────────────────────────────────────────────────────
  test('[TC-032] Delete non-existent ID returns HTTP 404', async () => {
    const bogusId = 999999999;

    await test.step(`DELETE /api/masterdata/master-lookups/${bogusId}`, async () => {
      const resp = await ctx.delete(`${API_LOOKUPS}/${bogusId}`, { headers: hdrs(token) });
      const body = await resp.json().catch(() => ({}));

      trace({ tcId: 'TC-032', endpoint: `DELETE /api/masterdata/master-lookups/${bogusId}`, requestPayload: {}, response: body, statusCode: resp.status() });

      expect(resp.status()).toBe(404);
    });
  });
});
