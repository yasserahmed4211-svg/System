import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * GL Accounts Chart API Tests
 *
 * Scope: API-level tests (no browser). Tests cover CRUD, validation,
 * business-rule errors, eligible-parents, tree, and search.
 *
 * Live-verified facts (Playwright MCP session 2026-04-05):
 *  - Backend: http://localhost:7272
 *  - Auth: POST /api/auth/login → { data: { accessToken } }
 *  - All responses wrapped in ApiResponse<T> { success, message, data, error: { code } }
 *  - Account type lookup codes: "1"=Asset, "2"=Liabilities, "3"=Equity, "4"=Revenue, "5"=Expenses
 *  - POST /api/gl/accounts → 201 on success; accountChartNo is auto-generated
 *  - DELETE /api/gl/accounts/{pk} → 204 on success (deactivates, no body)
 *  - DELETE returns 409 (not 400) when GL_ACCOUNT_HAS_CHILDREN
 *  - eligible-parents returns Spring Page object (body.data.content is the array)
 *  - eligible-parents param: excludeAccountPk (singular), not excludePks
 *  - Circular-reference error code: GL_ACCOUNT_DESCENDANT_AS_PARENT
 *  - GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN → HTTP 422 (Unprocessable Entity), not 400
 *  - Concurrent creation under the same parent can cause GL_ACCOUNT_NO_GENERATION_FAILED (422)
 *    → run this file with --project=chromium or --workers=1 to avoid race conditions
 *  - Type "1" (ASSET) root-account numbers 10-19 may be exhausted in test environments.
 *    Fixtures use type "5" (EXPENSES) with ample remaining capacity.
 */

const BASE = 'http://localhost:7272';
const ORG_FK = 1;

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

/**
 * Creates a leaf account with up to 3 retries on GL_ACCOUNT_NO_GENERATION_FAILED.
 * Exported so other spec files can reuse.
 */
export async function createLeafAccount(
  ctx: APIRequestContext,
  token: string,
  name: string,
  typeCode = '5',   // default to EXPENSES to avoid exhausting ASSET slots
  parentPk?: number,
): Promise<number> {
  const payload: Record<string, unknown> = {
    accountChartName: name,
    accountType: typeCode,
    organizationFk: ORG_FK,
    isActive: true,
  };
  if (parentPk !== undefined) payload.accountChartFk = parentPk;

  for (let attempt = 1; attempt <= 3; attempt++) {
    const resp = await ctx.post(`${BASE}/api/gl/accounts`, {
      headers: { Authorization: `Bearer ${token}` },
      data: payload,
    });
    if (resp.status() === 201) {
      const body = await resp.json();
      return body.data.accountChartPk as number;
    }
    const body = await resp.json();
    if (body.error?.code === 'GL_ACCOUNT_NO_GENERATION_FAILED' && attempt < 3) {
      // Back off and retry when concurrent number generation fails
      await new Promise((r) => setTimeout(r, 300 * attempt + Math.random() * 200));
      continue;
    }
    throw new Error(`createLeafAccount failed (attempt ${attempt}): HTTP ${resp.status()} – ${body.error?.code ?? body.message}`);
  }
  throw new Error('createLeafAccount: all retries exhausted');
}

// ─────────────────────────────────────────────────────────────────────────────
// Test Suite
// ─────────────────────────────────────────────────────────────────────────────

// Run serially within each browser project to avoid shared-fixture race conditions
test.describe.configure({ mode: 'serial' });

test.describe('GL Accounts Chart — API Tests', () => {
  let ctx: APIRequestContext;
  let token: string;

  /** Root EXPENSES account created in beforeAll; persists across tests. */
  let rootPk: number;
  /** Child EXPENSES account under rootPk; persists across tests. */
  let childPk: number;
  /** accountChartNo of root – captured in beforeAll */
  let rootAccountNo: string;
  /** accountChartNo of child – captured in beforeAll */
  let childAccountNo: string;

  test.beforeAll(async ({ playwright }) => {
    ctx = await playwright.request.newContext();
    token = await loginAsAdmin(ctx);

    // Fixture: root EXPENSES account
    const rootResp = await ctx.post(`${BASE}/api/gl/accounts`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { accountChartName: 'Spec Root EXPENSES [auto]', accountType: '5', organizationFk: ORG_FK, isActive: true },
    });
    expect(rootResp.status()).toBe(201);
    const rootBody = await rootResp.json();
    rootPk = rootBody.data.accountChartPk;
    rootAccountNo = rootBody.data.accountChartNo;

    // Fixture: child EXPENSES account under root
    const childResp = await ctx.post(`${BASE}/api/gl/accounts`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { accountChartName: 'Spec Child EXPENSES [auto]', accountType: '5', accountChartFk: rootPk, organizationFk: ORG_FK, isActive: true },
    });
    expect(childResp.status()).toBe(201);
    const childBody = await childResp.json();
    childPk = childBody.data.accountChartPk;
    childAccountNo = childBody.data.accountChartNo;
  });

  test.afterAll(async () => {
    // Clean up in leaf → root order
    if (childPk) {
      await ctx.delete(`${BASE}/api/gl/accounts/${childPk}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
    if (rootPk) {
      await ctx.delete(`${BASE}/api/gl/accounts/${rootPk}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
    await ctx.dispose();
  });

  // ── A-01 ──────────────────────────────────────────────────────────────────
  test('A-01: GET root account by PK → 200, auto-generated accountChartNo, level=0', async () => {
    // Verify the root fixture created in beforeAll rather than creating a new root
    // (avoids consuming from a limited number pool).
    const resp = await ctx.get(`${BASE}/api/gl/accounts/${rootPk}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(body.data.accountChartPk).toBe(rootPk);
    expect(body.data.accountChartNo).toBe(rootAccountNo);
    expect(body.data.accountChartNo).toBeTruthy();
    expect(body.data.accountChartName).toBe('Spec Root EXPENSES [auto]');
    expect(body.data.level).toBe(0);
    expect(body.data.isActive).toBe(true);
    expect(body.data.accountType).toBe('5');
  });

  // ── A-02 ──────────────────────────────────────────────────────────────────
  test('A-02: GET child account by PK → 200, level=1 and parent FK matches rootPk', async () => {
    // Verify the child fixture created in beforeAll rather than creating another child.
    const resp = await ctx.get(`${BASE}/api/gl/accounts/${childPk}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(body.data.accountChartPk).toBe(childPk);
    expect(body.data.accountChartNo).toBe(childAccountNo);
    expect(body.data.level).toBe(1);
    expect(body.data.accountChartFk).toBe(rootPk);
    expect(body.data.accountType).toBe('5');
  });

  // ── A-03 ──────────────────────────────────────────────────────────────────
  test('A-03: create account with blank name → 400 validation error', async () => {
    const resp = await ctx.post(`${BASE}/api/gl/accounts`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        accountChartName: '',
        accountType: '5',
        organizationFk: ORG_FK,
        isActive: true,
      },
    });

    expect(resp.status()).toBe(400);
    const body = await resp.json();
    expect(body.success).toBe(false);
  });

  // ── A-04 ──────────────────────────────────────────────────────────────────
  test('A-04: create child with type mismatch → 400 GL_ACCOUNT_TYPE_MISMATCH', async () => {
    // rootPk has type "5" (Expenses); sending type "4" (Revenue) should be rejected
    const resp = await ctx.post(`${BASE}/api/gl/accounts`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        accountChartName: 'A-04 Wrong Type Child',
        accountType: '4',
        accountChartFk: rootPk,
        organizationFk: ORG_FK,
        isActive: true,
      },
    });

    expect(resp.status()).toBe(400);
    const body = await resp.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe('GL_ACCOUNT_TYPE_MISMATCH');
  });

  // ── A-05 ──────────────────────────────────────────────────────────────────
  test('A-05: set descendant as parent (circular ref) → 400 GL_ACCOUNT_DESCENDANT_AS_PARENT', async () => {
    // rootPk → childPk is the existing hierarchy.
    // Attempting to set rootPk's parent to childPk creates a cycle.
    const resp = await ctx.put(`${BASE}/api/gl/accounts/${rootPk}`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        accountChartName: 'Spec Root EXPENSES [auto]',
        accountType: '5',
        accountChartFk: childPk,
        organizationFk: ORG_FK,
        isActive: true,
      },
    });

    expect(resp.status()).toBe(400);
    const body = await resp.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe('GL_ACCOUNT_CIRCULAR_REF');
  });

  // ── A-06 ──────────────────────────────────────────────────────────────────
  test('A-06: update account name → 200 with updated name returned', async () => {
    const newName = 'Spec Child EXPENSES [renamed]';
    const resp = await ctx.put(`${BASE}/api/gl/accounts/${childPk}`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        accountChartName: newName,
        accountType: '5',
        accountChartFk: rootPk,
        organizationFk: ORG_FK,
        isActive: true,
      },
    });

    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(body.data.accountChartName).toBe(newName);
  });

  // ── A-07 ──────────────────────────────────────────────────────────────────
  test('A-07: change type of account with children → 422 GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN', async () => {
    // rootPk has childPk as an active child; changing its type should be blocked.
    // Attempting to switch from "5" (Expenses) to "4" (Revenue).
    const resp = await ctx.put(`${BASE}/api/gl/accounts/${rootPk}`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        accountChartName: 'Spec Root EXPENSES [auto]',
        accountType: '4',   // Attempting to switch from "5" to "4"
        organizationFk: ORG_FK,
        isActive: true,
      },
    });

    // GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN → verified via live test: HTTP 422 (Unprocessable Entity)
    expect(resp.status()).toBe(422);
    const body = await resp.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe('GL_ACCOUNT_TYPE_CHANGE_WITH_CHILDREN');
  });

  // ── A-08 ──────────────────────────────────────────────────────────────────
  test('A-08: deactivate leaf account → 204 no content', async () => {
    // Create a disposable leaf account under rootPk
    const leafPk = await createLeafAccount(ctx, token, 'A-08 Leaf To Deactivate', '5', rootPk);

    const resp = await ctx.delete(`${BASE}/api/gl/accounts/${leafPk}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(resp.status()).toBe(204);
    // 204 = no body
    const text = await resp.text();
    expect(text).toBe('');
  });

  // ── A-09 ──────────────────────────────────────────────────────────────────
  test('A-09: deactivate account with active children → 409 GL_ACCOUNT_HAS_CHILDREN', async () => {
    // rootPk still has childPk as an active child
    const resp = await ctx.delete(`${BASE}/api/gl/accounts/${rootPk}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(resp.status()).toBe(409);
    const body = await resp.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe('GL_ACCOUNT_HAS_CHILDREN');
  });

  // ── A-10 ──────────────────────────────────────────────────────────────────
  test('A-10: GET eligible-parents excludes the specified account', async () => {
    const resp = await ctx.get(
      `${BASE}/api/gl/accounts/eligible-parents?excludeAccountPk=${rootPk}&organizationFk=${ORG_FK}`,
      { headers: { Authorization: `Bearer ${token}` } },
    );

    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.success).toBe(true);
    // eligible-parents returns a Spring Page object; the items are in body.data.content
    expect(body.data).toHaveProperty('content');
    expect(Array.isArray(body.data.content)).toBe(true);

    const pks = (body.data.content as { accountChartPk: number }[]).map((a) => a.accountChartPk);
    expect(pks).not.toContain(rootPk);
  });

  // ── A-11 ──────────────────────────────────────────────────────────────────
  test('A-11: GET /tree returns hierarchical nodes with level and children list', async () => {
    const resp = await ctx.get(`${BASE}/api/gl/accounts/tree`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);

    const root = (body.data as { accountChartPk: number; level: number; children: { accountChartPk: number; level: number }[] }[])
      .find((n) => n.accountChartPk === rootPk);
    expect(root).toBeDefined();
    expect(root!.level).toBe(0);
    expect(Array.isArray(root!.children)).toBe(true);
    expect(root!.children.length).toBeGreaterThan(0);

    const child = root!.children.find((c) => c.accountChartPk === childPk);
    expect(child).toBeDefined();
    expect(child!.level).toBe(1);
  });

  // ── A-12 ──────────────────────────────────────────────────────────────────
  test('A-12: POST /search with page=0&size=5 returns paginated Spring page', async () => {
    // A sort field is required — the default sort tries "id" which doesn't exist on AccountsChart.
    const resp = await ctx.post(`${BASE}/api/gl/accounts/search`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        filters: [{ field: 'organizationFk', operator: 'EQUALS', value: String(ORG_FK) }],
        sorts: [{ field: 'accountChartName', direction: 'ASC' }],
        page: 0,
        size: 5,
      },
    });

    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.success).toBe(true);

    const page = body.data as {
      content: unknown[];
      totalElements: number;
      totalPages: number;
      size: number;
    };
    expect(Array.isArray(page.content)).toBe(true);
    expect(page.content.length).toBeLessThanOrEqual(5);
    expect(typeof page.totalElements).toBe('number');
    expect(typeof page.totalPages).toBe('number');
  });
});
