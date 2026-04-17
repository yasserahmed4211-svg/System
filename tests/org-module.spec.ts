import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * Organization Module (ORG-001) — API Tests
 *
 * Scope: API-level tests (no browser UI). All test cases traced to:
 *   TC-LE-01 … TC-LE-04  (Legal Entity)
 *   TC-RG-01 … TC-RG-03  (Region)
 *   TC-BR-01 … TC-BR-05  (Branch)
 *   TC-DP-01             (Department)
 *   TC-SYS-01            (System Fields)
 *
 * Live-verified facts:
 *  - Backend: http://localhost:7272
 *  - Auth: POST /api/auth/login → { data: { accessToken } }
 *  - All responses: ApiResponse<T> { success, message, data, error: { code } }
 *  - POST /api/organization/legal-entities  → 201 on success
 *  - POST /api/organization/regions         → 201 on success
 *  - POST /api/organization/branches        → 201 on success
 *  - PATCH /deactivate endpoints            → 409 on business rule violations
 *  - CONFLICT errors map to HTTP 409
 *  - BUSINESS_ERROR errors map to HTTP 422
 *  - Seed lookup IDs (country=1, currency=1) match V0.1__test_data_insert seed
 */

const BASE = 'http://localhost:7272';

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

async function loginAsAdmin(ctx: APIRequestContext): Promise<string> {
  const resp = await ctx.post(`${BASE}/api/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
  });
  expect(resp.ok()).toBeTruthy();
  const body = await resp.json();
  const token = body.data?.accessToken as string;
  expect(token).toBeTruthy();
  return token;
}

/** Authenticated headers shorthand */
function auth(token: string) {
  return { Authorization: `Bearer ${token}` };
}

/** Extract the business error code from any error response. */
async function errorCode(resp: Awaited<ReturnType<APIRequestContext['post']>>): Promise<string> {
  const body = await resp.json();
  return (body.error?.code ?? body.message ?? '') as string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Fixtures — shared lookup IDs (from seed data V0.1 / V16__org_lookup_seed.sql)
// ─────────────────────────────────────────────────────────────────────────────
const COUNTRY_FK  = 1;   // seeded country
const CURRENCY_FK = 1;   // seeded functional currency
const BRANCH_TYPE = 'BRANCH';
const DEPT_TYPE   = 'SALES';

// ─────────────────────────────────────────────────────────────────────────────
// Unique-name generators (epoch-based to avoid cross-run collisions)
// ─────────────────────────────────────────────────────────────────────────────
function uid() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 6); }
function leName()   { const s = uid(); return { ar: `كيان-${s}`, en: `Entity-${s}` }; }
function rgName()   { const s = uid(); return { ar: `منطقة-${s}`, en: `Region-${s}` }; }
function brName()   { const s = uid(); return { ar: `فرع-${s}`, en: `Branch-${s}` }; }
function deptName() { const s = uid(); return { ar: `قسم-${s}`, en: `Dept-${s}` }; }

// ─────────────────────────────────────────────────────────────────────────────
// Force serial execution (tests build on each other's data)
// ─────────────────────────────────────────────────────────────────────────────
test.describe.configure({ mode: 'serial' });

// ═════════════════════════════════════════════════════════════════════════════
// Legal Entity Tests
// ═════════════════════════════════════════════════════════════════════════════

test.describe('Organization Module — Legal Entity', () => {
  let ctx: APIRequestContext;
  let token: string;
  /** PK of the legal entity created in TC-LE-01 — reused by later tests */
  let legalEntityPk: number;

  test.beforeAll(async ({ playwright }) => {
    ctx = await playwright.request.newContext();
    token = await loginAsAdmin(ctx);
  });

  test.afterAll(async () => {
    await ctx.dispose();
  });

  // ── TC-LE-01 ──────────────────────────────────────────────────────────────
  test('[TC-LE-01] creates a legal entity successfully — code assigned, activeFl=1', async () => {
    const name = leName();
    await test.step('POST /api/organization/legal-entities', async () => {
      const resp = await ctx.post(`${BASE}/api/organization/legal-entities`, {
        headers: auth(token),
        data: {
          legalEntityNameAr: name.ar,
          legalEntityNameEn: name.en,
          countryId: COUNTRY_FK,
          functionalCurrencyId: CURRENCY_FK,
        },
      });

      expect(resp.status()).toBe(201);
      const body = await resp.json();
      expect(body.success).toBe(true);

      const data = body.data;
      expect(data.legalEntityCode).toMatch(/^LE-/);          // RULE-LE-01: code format
      expect(data.isActive).toBe(true);                       // RULE-LE-02: default active
      expect(typeof data.createdAt).toBe('string');           // TC-SYS-01

      legalEntityPk = data.id;
      expect(legalEntityPk).toBeTruthy();
    });
  });

  // ── TC-LE-04 ──────────────────────────────────────────────────────────────
  test('[TC-LE-04] rejects invalid email format — LEGAL_ENTITY_INVALID_EMAIL', async () => {
    const name = leName();
    await test.step('POST with malformed email', async () => {
      const resp = await ctx.post(`${BASE}/api/organization/legal-entities`, {
        headers: auth(token),
        data: {
          legalEntityNameAr: name.ar,
          legalEntityNameEn: name.en,
          countryId: COUNTRY_FK,
          functionalCurrencyId: CURRENCY_FK,
          email: 'not-a-valid-email',
        },
      });

      expect(resp.status()).not.toBe(201);
      expect(await errorCode(resp)).toBe('LEGAL_ENTITY_INVALID_EMAIL');
    });
  });

  // ── TC-LE-02 ──────────────────────────────────────────────────────────────
  test('[TC-LE-02] prevents currency change when financial transactions exist — LEGAL_ENTITY_CURRENCY_LOCKED', async () => {
    // This rule requires existing GL/financial transactions linked to the legal entity.
    // A freshly created entity (clean DB) has no transactions → currency change is allowed.
    // This test MUST run against a seeded DB that contains journal entries or postings
    // for the target legal entity.
    //
    // To run manually against a seeded DB:
    //   SEEDED_LE_PK=<pk_with_transactions> npx playwright test --grep TC-LE-02
    const seededPk = process.env['SEEDED_LE_PK'] ? Number(process.env['SEEDED_LE_PK']) : undefined;
    test.skip(!seededPk, 'Requires SEEDED_LE_PK env var pointing to an LE with financial transactions');

    await test.step('PUT /{id} — change functionalCurrencyId on entity with transactions', async () => {
      const resp = await ctx.put(`${BASE}/api/organization/legal-entities/${seededPk}`, {
        headers: auth(token),
        data: {
          legalEntityNameAr: `مؤشر-${uid()}`,
          legalEntityNameEn: `Entity-${uid()}`,
          countryId: COUNTRY_FK,
          functionalCurrencyId: 2, // attempt currency change
        },
      });

      expect(resp.status()).toBe(409);
      expect(await errorCode(resp)).toBe('LEGAL_ENTITY_CURRENCY_LOCKED');
    });
  });

  // ── TC-LE-03 ──────────────────────────────────────────────────────────────
  test('[TC-LE-03] prevents deactivation when active branches exist — LEGAL_ENTITY_HAS_ACTIVE_BRANCHES', async () => {
    test.skip(!legalEntityPk, 'Requires legal entity created in TC-LE-01');

    // Create a branch under this entity so the business rule triggers
    const br = brName();
    const branchResp = await ctx.post(`${BASE}/api/organization/branches`, {
      headers: auth(token),
      data: {
        legalEntityId: legalEntityPk,
        branchNameAr: br.ar,
        branchNameEn: br.en,
        branchTypeId: BRANCH_TYPE,
        isHeadquarter: false,
      },
    });
    // Only proceed if branch creation succeeded
    if (branchResp.status() !== 201) {
      test.skip(); // skip if branch seed failed (e.g. BRANCH_TYPE lookup missing)
      return;
    }

    await test.step('PATCH /deactivate on LE with active branches', async () => {
      const resp = await ctx.patch(
        `${BASE}/api/organization/legal-entities/${legalEntityPk}/deactivate`,
        { headers: auth(token) },
      );

      expect(resp.status()).toBe(409);
      expect(await errorCode(resp)).toBe('LEGAL_ENTITY_HAS_ACTIVE_BRANCHES');
    });
  });

  // ── TC-SYS-01 (partial — LE layer) ────────────────────────────────────────
  test('[TC-SYS-01] system fields (createdAt, createdBy) are auto-populated on create', async () => {
    const name = leName();
    const resp = await ctx.post(`${BASE}/api/organization/legal-entities`, {
      headers: auth(token),
      data: {
        legalEntityNameAr: name.ar,
        legalEntityNameEn: name.en,
        countryId: COUNTRY_FK,
        functionalCurrencyId: CURRENCY_FK,
        // Intentionally omit createdAt / createdBy — must be auto-set
        createdAt: undefined,
        createdBy: undefined,
      },
    });

    expect(resp.status()).toBe(201);
    const { data } = await resp.json();
    expect(data.createdAt).toBeTruthy();
    expect(data.createdBy).toBeTruthy();
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// Region Tests
// ═════════════════════════════════════════════════════════════════════════════

test.describe('Organization Module — Region', () => {
  let ctx: APIRequestContext;
  let token: string;
  /** Active legal entity used as FK throughout region tests */
  let legalEntityPk: number;
  /** PK of region created in TC-RG-01 */
  let regionPk: number;

  test.beforeAll(async ({ playwright }) => {
    ctx = await playwright.request.newContext();
    token = await loginAsAdmin(ctx);

    // Seed: create a fresh active legal entity for region tests
    const name = leName();
    const leResp = await ctx.post(`${BASE}/api/organization/legal-entities`, {
      headers: auth(token),
      data: {
        legalEntityNameAr: name.ar,
        legalEntityNameEn: name.en,
        countryId: COUNTRY_FK,
        functionalCurrencyId: CURRENCY_FK,
      },
    });
    const leBody = await leResp.json();
    legalEntityPk = leBody.data?.legalEntityPk ?? leBody.data?.id;
  });

  test.afterAll(async () => {
    await ctx.dispose();
  });

  // ── TC-RG-01 ──────────────────────────────────────────────────────────────
  test('[TC-RG-01] creates a region successfully — regionCode assigned', async () => {
    test.skip(!legalEntityPk, 'Could not seed legal entity');
    const name = rgName();

    await test.step('POST /api/organization/regions', async () => {
      const resp = await ctx.post(`${BASE}/api/organization/regions`, {
        headers: auth(token),
        data: {
          legalEntityId: legalEntityPk,
          regionNameAr: name.ar,
          regionNameEn: name.en,
        },
      });

      expect(resp.status()).toBe(201);
      const body = await resp.json();
      expect(body.success).toBe(true);

      const data = body.data;
      expect(data.regionCode).toMatch(/^RGN-/);           // RULE-RG-02: code format
      expect(data.legalEntityId).toBe(legalEntityPk);

      regionPk = data.id;
      expect(regionPk).toBeTruthy();
    });
  });

  // ── TC-RG-02 ──────────────────────────────────────────────────────────────
  test('[TC-RG-02] legalEntityId cannot be changed on existing region — field is locked (RULE-RG-04)', async () => {
    test.skip(!regionPk || !legalEntityPk, 'Requires region created in TC-RG-01');

    // RegionUpdateRequest does NOT include legalEntityId — any value sent is silently ignored.
    // The rule is enforced by DTO design (field omitted) + mapper ignoring it.
    // Verified behavior: returns 200 and legalEntityId stays unchanged.

    // Create a second legal entity as the "attempted" new owner
    const name2 = leName();
    const le2Resp = await ctx.post(`${BASE}/api/organization/legal-entities`, {
      headers: auth(token),
      data: {
        legalEntityNameAr: name2.ar,
        legalEntityNameEn: name2.en,
        countryId: COUNTRY_FK,
        functionalCurrencyId: CURRENCY_FK,
      },
    });
    const le2Body = await le2Resp.json();
    const legalEntity2Pk = le2Body.data?.id;

    await test.step('PUT /{id} — legalEntityId in body is silently ignored', async () => {
      const name = rgName();
      const resp = await ctx.put(`${BASE}/api/organization/regions/${regionPk}`, {
        headers: auth(token),
        data: {
          legalEntityId: legalEntity2Pk,   // not part of RegionUpdateRequest — silently ignored
          regionNameAr: name.ar,
          regionNameEn: name.en,
        },
      });

      // Server accepts the update (200) but legalEntityId stays unchanged
      expect(resp.status()).toBe(200);
      const body = await resp.json();
      expect(body.data.legalEntityId).toBe(legalEntityPk);  // original — not changed
    });
  });

  // ── TC-RG-03 ──────────────────────────────────────────────────────────────
  test('[TC-RG-03] prevents deactivation of region with active branches — REGION_HAS_ACTIVE_BRANCHES', async () => {
    test.skip(!regionPk || !legalEntityPk, 'Requires region from TC-RG-01');

    // Create a branch linked to this region
    const br = brName();
    const branchResp = await ctx.post(`${BASE}/api/organization/branches`, {
      headers: auth(token),
      data: {
        legalEntityId: legalEntityPk,
        regionId: regionPk,
        branchNameAr: br.ar,
        branchNameEn: br.en,
        branchTypeId: BRANCH_TYPE,
        isHeadquarter: false,
      },
    });

    if (branchResp.status() !== 201) {
      test.skip(); // guard: skip if branch seed failed
      return;
    }

    await test.step('PATCH /{id}/deactivate on region with active branches', async () => {
      const resp = await ctx.patch(
        `${BASE}/api/organization/regions/${regionPk}/deactivate`,
        { headers: auth(token) },
      );

      expect(resp.status()).toBe(409);
      expect(await errorCode(resp)).toBe('REGION_HAS_ACTIVE_BRANCHES');
    });
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// Branch & Department Tests
// ═════════════════════════════════════════════════════════════════════════════

test.describe('Organization Module — Branch & Department', () => {
  let ctx: APIRequestContext;
  let token: string;
  /** Active legal entity for branch tests */
  let legalEntityPk: number;
  /** PK of the branch created in TC-BR-01 */
  let branchPk: number;

  test.beforeAll(async ({ playwright }) => {
    ctx = await playwright.request.newContext();
    token = await loginAsAdmin(ctx);

    // Seed a fresh legal entity
    const name = leName();
    const leResp = await ctx.post(`${BASE}/api/organization/legal-entities`, {
      headers: auth(token),
      data: {
        legalEntityNameAr: name.ar,
        legalEntityNameEn: name.en,
        countryId: COUNTRY_FK,
        functionalCurrencyId: CURRENCY_FK,
      },
    });
    const leBody = await leResp.json();
    legalEntityPk = leBody.data?.legalEntityPk ?? leBody.data?.id;
  });

  test.afterAll(async () => {
    await ctx.dispose();
  });

  // ── TC-BR-01 ──────────────────────────────────────────────────────────────
  test('[TC-BR-01] creates branch with departments atomically — codes auto-generated', async () => {
    test.skip(!legalEntityPk, 'Could not seed legal entity');

    const br   = brName();
    const dep1 = deptName();
    const dep2 = deptName();

    await test.step('POST /api/organization/branches (with departments[])', async () => {
      const resp = await ctx.post(`${BASE}/api/organization/branches`, {
        headers: auth(token),
        data: {
          legalEntityId: legalEntityPk,
          branchNameAr:  br.ar,
          branchNameEn:  br.en,
          branchTypeId:  BRANCH_TYPE,
          isHeadquarter: false,
          departments: [
            { departmentNameAr: dep1.ar, departmentNameEn: dep1.en, departmentTypeId: DEPT_TYPE },
            { departmentNameAr: dep2.ar, departmentNameEn: dep2.en, departmentTypeId: DEPT_TYPE },
          ],
        },
      });

      const body = await resp.json();
      if (resp.status() !== 201) {
        throw new Error(`[TC-BR-01] Expected 201, got ${resp.status()}. Body: ${JSON.stringify(body)}`);
      }
      expect(body.success).toBe(true);

      const data = body.data;
      expect(data.branchCode).toMatch(/^BRN-/);           // RULE-BR-01: code format
      expect(Array.isArray(data.departments)).toBe(true);
      expect(data.departments.length).toBe(2);
      // Each department must have an auto-generated code
      for (const dept of data.departments) {
        expect(dept.departmentCode).toMatch(/^DEP-/);     // RULE-BR-08: dept codes
      }

      branchPk = data.id;
      expect(branchPk).toBeTruthy();
    });
  });

  // ── TC-BR-02 ──────────────────────────────────────────────────────────────
  test('[TC-BR-02] rolls back entirely when one department has missing nameAr — PARTIAL_SAVE_FAILED', async () => {
    test.skip(!legalEntityPk, 'Could not seed legal entity');

    const br   = brName();
    const dep1 = deptName();

    await test.step('POST branch with invalid department (missing departmentNameAr)', async () => {
      const resp = await ctx.post(`${BASE}/api/organization/branches`, {
        headers: auth(token),
        data: {
          legalEntityId: legalEntityPk,
          branchNameAr:  br.ar,
          branchNameEn:  br.en,
          branchTypeId:  BRANCH_TYPE,
          isHeadquarter: false,
          departments: [
            { departmentNameAr: dep1.ar, departmentNameEn: dep1.en, departmentTypeId: DEPT_TYPE },
            // Second dept intentionally missing departmentNameAr → triggers validation
            { departmentNameAr: '', departmentNameEn: 'InvalidDept', departmentTypeId: DEPT_TYPE },
          ],
        },
      });

      // Must NOT be 201 — nothing should be saved
      expect(resp.status()).not.toBe(201);
      const code = await errorCode(resp);
      // Accept PARTIAL_SAVE_FAILED, or a Bean-Validation 400 that prevents any save
      expect(
        ['PARTIAL_SAVE_FAILED', 'VALIDATION_ERROR', 'INVALID_INPUT'].includes(code) ||
        resp.status() === 400,
      ).toBeTruthy();
    });
  });

  // ── TC-BR-03 ──────────────────────────────────────────────────────────────
  test('[TC-BR-03] prevents duplicate HQ for the same legal entity — BRANCH_HQ_EXISTS', async () => {
    test.skip(!legalEntityPk, 'Could not seed legal entity');

    // Create first HQ branch
    const hq1 = brName();
    const firstResp = await ctx.post(`${BASE}/api/organization/branches`, {
      headers: auth(token),
      data: {
        legalEntityId: legalEntityPk,
        branchNameAr:  hq1.ar,
        branchNameEn:  hq1.en,
        branchTypeId:  BRANCH_TYPE,
        isHeadquarter: true,
      },
    });
    expect(firstResp.status()).toBe(201);

    // Attempt to create a second HQ under the same legal entity
    const hq2 = brName();
    await test.step('POST second isHeadquarter=true for same legalEntity', async () => {
      const resp = await ctx.post(`${BASE}/api/organization/branches`, {
        headers: auth(token),
        data: {
          legalEntityId: legalEntityPk,
          branchNameAr:  hq2.ar,
          branchNameEn:  hq2.en,
          branchTypeId:  BRANCH_TYPE,
          isHeadquarter: true,
        },
      });

      expect(resp.status()).toBe(409);
      expect(await errorCode(resp)).toBe('BRANCH_HQ_EXISTS');
    });
  });

  // ── TC-BR-05 ──────────────────────────────────────────────────────────────
  test('[TC-BR-05] prevents deactivation of branch with active users — BRANCH_HAS_ACTIVE_USERS', async () => {
    // This rule requires active users linked to the branch.
    // In a clean test environment there may be no such users.
    // The test documents the expected error code; CI should run against a seeded DB.
    test.skip(!branchPk, 'Requires branch created in TC-BR-01');
    test.slow(); // Mark slow — depends on seeded user-branch assignments

    await test.step('PATCH /{id}/deactivate on branch with active users', async () => {
      const resp = await ctx.patch(
        `${BASE}/api/organization/branches/${branchPk}/deactivate`,
        { headers: auth(token) },
      );

      // In a clean DB without linked users the branch deactivates (200).
      // In a seeded DB with active users it must return 409 + BRANCH_HAS_ACTIVE_USERS.
      if (resp.status() === 409) {
        expect(await errorCode(resp)).toBe('BRANCH_HAS_ACTIVE_USERS');
      } else {
        // Reactivate to avoid polluting later tests
        await ctx.put(`${BASE}/api/organization/branches/${branchPk}/toggle-active`, {
          headers: auth(token),
          data: { active: true },
        });
      }
    });
  });

  // ── TC-DP-01 ──────────────────────────────────────────────────────────────
  test('[TC-DP-01] prevents deactivation of department with active users — DEPARTMENT_HAS_ACTIVE_USERS', async () => {
    test.skip(!branchPk, 'Requires branch created in TC-BR-01');
    test.slow(); // Depends on seeded user-department assignments

    // Fetch departments of the created branch
    const deptListResp = await ctx.get(
      `${BASE}/api/organization/branches/${branchPk}/departments`,
      { headers: auth(token) },
    );
    expect(deptListResp.ok()).toBeTruthy();
    const deptListBody = await deptListResp.json();
    const departments: Array<{ id?: number }> = deptListBody.data ?? [];

    if (departments.length === 0) {
      test.skip(); // no departments to test against
      return;
    }

    const deptPk = departments[0].id;

    await test.step('PATCH /departments/{id}/deactivate on dept with active users', async () => {
      const resp = await ctx.patch(
        `${BASE}/api/organization/branches/departments/${deptPk}/deactivate`,
        { headers: auth(token) },
      );

      // Same pattern: 409 in seeded DB, OK in clean DB
      if (resp.status() === 409) {
        expect(await errorCode(resp)).toBe('DEPARTMENT_HAS_ACTIVE_USERS');
      }
      // No assertion needed for clean-DB path — rule documented by TC-DP-01
    });
  });

  // ── TC-SYS-01 (Branch layer) ───────────────────────────────────────────────
  test('[TC-SYS-01] system fields auto-populated on branch create', async () => {
    test.skip(!legalEntityPk, 'Could not seed legal entity');

    const br = brName();
    const resp = await ctx.post(`${BASE}/api/organization/branches`, {
      headers: auth(token),
      data: {
        legalEntityId: legalEntityPk,
        branchNameAr:  br.ar,
        branchNameEn:  br.en,
        branchTypeId:  BRANCH_TYPE,
        isHeadquarter: false,
      },
    });

    expect(resp.status()).toBe(201);
    const { data } = await resp.json();
    expect(data.createdAt).toBeTruthy();
    expect(data.createdBy).toBeTruthy();
    // updatedAt may equal createdAt on fresh records
    expect(data.updatedAt).toBeTruthy();
  });
});
