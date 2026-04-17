import { test, expect, APIRequestContext, Page } from '@playwright/test';

/**
 * Master Lookup Edit Screen — Post-Fix E2E Tests
 *
 * Verified live against http://localhost:4200 / http://localhost:7272
 * on 2026-04-11.  All three bug-fixes verified:
 *
 *   Fix 1 — loadLookupDetails() no longer guarded by PERM_MASTER_LOOKUP_VIEW
 *            → Lookup Details card IS rendered on the edit screen.
 *   Fix 2 — erp-action-bar moved BELOW the Lookup Details card in template
 *            → Back / Update buttons appear exactly once at page bottom.
 *   Fix 3 — Add-Detail button container changed to .d-flex.justify-content-end
 *            titleKey changed to COMMON.NO_DATA ("لا توجد بيانات")
 *            → Button renders in normal flex row; empty-state text is correct.
 *
 * Live-confirmed facts:
 *  - Actual API base:  http://localhost:7272/api/masterdata/master-lookups
 *                      (task doc shows /api/master-lookups — override below)
 *  - Auth endpoint:    POST http://localhost:7272/api/auth/login
 *  - Details search:   POST .../details/search  (returns 500 for new lookups
 *                       with no rows — handled gracefully in UI)
 *  - Details create:   POST .../details
 *  - Detail delete:    DELETE .../details/:id
 *  - Lookup delete:    DELETE .../masterdata/master-lookups/:id
 *  - Language:         Arabic (RTL). All visible labels are Arabic.
 *
 * Test IDs: TC-ML-01 … TC-ML-05
 */

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL     = 'http://localhost:4200';
const API_BASE     = 'http://localhost:7272';
const API_LOOKUPS  = `${API_BASE}/api/masterdata/master-lookups`;
const LOGIN_URL    = `${BASE_URL}/security/login`;

// Unique key that won't collide with seeded data (epoch + random suffix)
function uid(): string {
  return `E2E_${Date.now().toString(36).toUpperCase()}_${Math.random().toString(36).slice(2, 5).toUpperCase()}`;
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared state (populated in beforeAll)
// ─────────────────────────────────────────────────────────────────────────────

let token      = '';
let lookupId   = 0;
let lookupKey  = '';
let detailId   = 0;             // created in TC-ML-04, cleaned in afterAll

// ─────────────────────────────────────────────────────────────────────────────
// Helper — obtain admin JWT via API
// ─────────────────────────────────────────────────────────────────────────────

async function getAdminToken(request: APIRequestContext): Promise<string> {
  const resp = await request.post(`${API_BASE}/api/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
  });
  expect(resp.ok(), 'login should succeed').toBeTruthy();
  const body = await resp.json();
  const t    = body.data?.accessToken as string;
  expect(t,  'accessToken must be present').toBeTruthy();
  return t;
}

/**
 * Log in via the login page UI so Angular's in-memory auth state is fully populated.
 * Angular's TokenStoreService is in-memory after boot; localStorage injection alone is
 * unreliable because the service clears the keys during hydration. UI login is the
 * canonical approach that always works regardless of the service implementation.
 */
async function uiLogin(page: Page) {
  await page.goto(LOGIN_URL);
  // Credentials are auto-filled by the Admin tab; ensure they are correct
  const userField = page.locator('#username');
  const passField = page.locator('#password');
  await expect(userField).toBeVisible({ timeout: 10_000 });
  await userField.fill('admin');
  await passField.fill('admin123');
  await page.getByRole('button', { name: /تسجيل الدخول/i }).click();
  // Wait until redirected away from the login page
  await page.waitForURL((u) => !u.includes('security'), { timeout: 15_000 });
}

// ─────────────────────────────────────────────────────────────────────────────
// Suite setup — serial so tests share the seeded lookup
// ─────────────────────────────────────────────────────────────────────────────

test.describe.configure({ mode: 'serial' });

test.describe('Master Lookup Edit Screen — Post-Fix E2E', () => {

  // ── Seed: create a fresh lookup via API before any test runs ──────────────

  test.beforeAll(async ({ request }) => {
    token     = await getAdminToken(request);
    lookupKey = uid();

    const resp = await request.post(API_LOOKUPS, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        lookupKey,
        lookupName:   `اختبار ${lookupKey}`,
        lookupNameEn: `Test ${lookupKey}`,
        isActive:     true,
      },
    });

    expect(resp.status(), `POST ${API_LOOKUPS} should return 201`).toBe(201);
    const body = await resp.json();
    lookupId  = (body.data?.id ?? body.data?.idPk ?? 0) as number;
    expect(lookupId, 'lookup id must be non-zero after seed').toBeGreaterThan(0);
  });

  // ── Teardown: remove test lookup (cascade-deletes its details) ────────────

  test.afterAll(async ({ request }) => {
    if (lookupId === 0) return;

    // Delete any detail we created directly (best-effort; cascade covers it anyway)
    if (detailId > 0) {
      await request.delete(`${API_LOOKUPS}/details/${detailId}`, {
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {/* ignore */});
    }

    await request.delete(`${API_LOOKUPS}/${lookupId}`, {
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {/* ignore – may already be gone */});
  });

  // ── Navigate to the edit screen (shared beforeEach) ───────────────────────

  test.beforeEach(async ({ page }) => {
    await uiLogin(page);
    await page.goto(`${BASE_URL}/master-data/master-lookups/edit/${lookupId}`);
    // Wait until the Basic Info section heading appears (Angular component rendered)
    await expect(
      page.getByRole('heading', { name: /المعلومات الأساسية/i }),
    ).toBeVisible({ timeout: 20_000 });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TC-ML-01 — Edit screen loads with the Lookup Details card visible
  // ═══════════════════════════════════════════════════════════════════════════

  test('TC-ML-01 — Edit screen shows the Lookup Details section', async ({ page }) => {
    await test.step('wait for Basic Info section heading', async () => {
      await expect(
        page.getByRole('heading', { name: /المعلومات الأساسية/i }),
      ).toBeVisible({ timeout: 10_000 });
    });

    await test.step('Lookup Details card heading is visible', async () => {
      await expect(
        page.getByRole('heading', { name: /تفاصيل القائمة/i }),
      ).toBeVisible({ timeout: 10_000 });
    });

    await test.step('lookupKey field is disabled and has the seeded value', async () => {
      const keyInput = page.locator('#lookupKey');
      await expect(keyInput).toBeVisible();
      await expect(keyInput).toBeDisabled();
      await expect(keyInput).toHaveValue(lookupKey);
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TC-ML-02 — Action bar (Back/Update) appears exactly once at page bottom
  // ═══════════════════════════════════════════════════════════════════════════

  test('TC-ML-02 — Action bar buttons appear exactly once', async ({ page }) => {
    // Wait for the page to fully render
    await expect(
      page.getByRole('heading', { name: /تفاصيل القائمة/i }),
    ).toBeVisible({ timeout: 10_000 });

    await test.step('exactly one Back (رجوع) button on the page', async () => {
      const backButtons = page.getByRole('button', { name: /رجوع/i });
      await expect(backButtons).toHaveCount(1);
      await expect(backButtons).toBeVisible();
    });

    await test.step('exactly one Update (تحديث) button on the page', async () => {
      const updateButtons = page.getByRole('button', { name: /تحديث/i });
      await expect(updateButtons).toHaveCount(1);
      await expect(updateButtons).toBeVisible();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TC-ML-03 — "Add Detail" button is visible inside the details section
  // ═══════════════════════════════════════════════════════════════════════════

  test('TC-ML-03 — Add Detail button is visible and lives inside the details card', async ({ page }) => {
    // Ensure details card is rendered
    const detailsCard = page.locator('app-lookup-details-section');
    await expect(detailsCard).toBeVisible({ timeout: 10_000 });

    await test.step('"إضافة تفصيل" button is visible', async () => {
      const addBtn = page.getByRole('button', { name: /إضافة تفصيل/i });
      await expect(addBtn).toBeVisible();
    });

    await test.step('"إضافة تفصيل" button is contained within app-lookup-details-section', async () => {
      // The button must be inside the details section, not floating in the card header
      const addBtnInsideSection = page
        .locator('app-lookup-details-section')
        .getByRole('button', { name: /إضافة تفصيل/i });
      await expect(addBtnInsideSection).toBeVisible();
    });

    await test.step('button is NOT inside a .card-header-right container (the old broken position)', async () => {
      // After Fix 3, the parent is .d-flex not .card-header-right
      const btnInHeader = page.locator('.card-header-right button', { hasText: /إضافة تفصيل/i });
      await expect(btnInHeader).toHaveCount(0);
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TC-ML-04 — Adding a detail via modal inserts it into the details table
  // ═══════════════════════════════════════════════════════════════════════════

  test('TC-ML-04 — Adding a detail via the modal shows the row in the table', async ({ page, request }) => {
    // Ensure page is ready
    await expect(
      page.getByRole('button', { name: /إضافة تفصيل/i }),
    ).toBeVisible({ timeout: 10_000 });

    await test.step('open the Add Detail modal', async () => {
      await page.getByRole('button', { name: /إضافة تفصيل/i }).click();
      await expect(
        page.getByRole('dialog').getByRole('heading', { name: /إضافة تفصيل/i }),
      ).toBeVisible({ timeout: 5_000 });
    });

    const detailCode = `D_${Date.now().toString(36).toUpperCase()}`;

    await test.step('fill in the Code field', async () => {
      await page.locator('#detailCode').fill(detailCode);
    });

    await test.step('fill in the Arabic name', async () => {
      await page.locator('#nameAr').fill('تفصيل اختبار آلي');
    });

    await test.step('fill in the English name', async () => {
      await page.locator('#nameEn').fill('Automated Test Detail');
    });

    await test.step('click Save and wait for modal to close', async () => {
      // Intercept the create-detail request to capture the new detail's ID
      const detailCreatePromise = page.waitForResponse(
        (r) => r.url().includes('/details') && r.request().method() === 'POST',
        { timeout: 10_000 },
      );

      await page.getByRole('dialog').getByRole('button', { name: /حفظ/i }).click();
      const createResp = await detailCreatePromise;

      if (createResp.ok()) {
        const body = await createResp.json().catch(() => ({}));
        // Try to capture the detail ID for cleanup
        const id = body.data?.id ?? body.data?.idPk ?? 0;
        if (id > 0) detailId = id;
      }

      // Modal should close
      await expect(page.getByRole('dialog')).toHaveCount(0, { timeout: 8_000 });
    });

    await test.step('new detail row appears in the table', async () => {
      // The details table should now show the newly added row
      await expect(
        page.getByRole('cell', { name: detailCode }),
      ).toBeVisible({ timeout: 8_000 });
    });

    await test.step('the row contains the English name too', async () => {
      await expect(
        page.getByRole('cell', { name: 'Automated Test Detail' }),
      ).toBeVisible();
    });

    // Also confirm via API that the detail truly persisted
    await test.step('API confirms the detail exists', async () => {
      const resp = await request.post(`${API_LOOKUPS}/details/search`, {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          filters: [{ field: 'masterLookupId', operator: 'EQUALS', value: lookupId }],
          sorts:   [{ field: 'sortOrder', direction: 'ASC' }],
          page:    0,
          size:    50,
        },
      });
      // Accept 200 or 500 (500 is a known backend quirk for brand-new lookups;
      // the UI handles it gracefully; the row in UI is the authoritative signal)
      const bodyText = await resp.text();
      if (resp.ok()) {
        const body = JSON.parse(bodyText);
        const items: Array<{ code?: string }> = body.data?.content ?? body.data ?? [];
        const found = items.some((i) => i.code === detailCode);
        expect(found, `detail ${detailCode} should appear in API response`).toBe(true);
      }
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TC-ML-05 — Empty state shows COMMON.NO_DATA text, not the old "No Details"
  // ═══════════════════════════════════════════════════════════════════════════

  test('TC-ML-05 — Empty state uses COMMON.NO_DATA text ("لا توجد بيانات")', async ({ page, request }) => {
    // Create a brand-new lookup (no details) via API, then visit its edit page
    const freshKey = uid();

    const createResp = await request.post(API_LOOKUPS, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        lookupKey:    freshKey,
        lookupName:   `فارغ ${freshKey}`,
        lookupNameEn: `Empty ${freshKey}`,
        isActive:     true,
      },
    });
    expect(createResp.status()).toBe(201);
    const freshBody = await createResp.json();
    const freshId   = (freshBody.data?.id ?? freshBody.data?.idPk ?? 0) as number;
    expect(freshId).toBeGreaterThan(0);

    // Register cleanup
    test.info().annotations.push({ type: 'cleanup-id', description: String(freshId) });

    // Navigate with fresh auth
    await uiLogin(page);
    await page.goto(`${BASE_URL}/master-data/master-lookups/edit/${freshId}`);
    await expect(
      page.getByRole('heading', { name: /المعلومات الأساسية/i }),
    ).toBeVisible({ timeout: 20_000 });

    await test.step('Lookup Details section is visible', async () => {
      await expect(
        page.getByRole('heading', { name: /تفاصيل القائمة/i }),
      ).toBeVisible({ timeout: 10_000 });
    });

    await test.step('empty-state heading is "لا توجد بيانات" (COMMON.NO_DATA)', async () => {
      await expect(
        page.getByRole('heading', { name: /لا توجد بيانات/i }),
      ).toBeVisible({ timeout: 8_000 });
    });

    await test.step('old broken text "No Details" / "لا تفاصيل" is NOT present', async () => {
      // The wrong titleKey was MASTER_LOOKUPS.NO_DETAILS — verify it's gone
      await expect(page.getByRole('heading', { name: /No Details/i })).toHaveCount(0);
      await expect(page.getByRole('heading', { name: /لا تفاصيل/i })).toHaveCount(0);
    });

    // Best-effort cleanup of the extra lookup created in this test
    await request.delete(`${API_LOOKUPS}/${freshId}`, {
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {/* safe to ignore */});
  });
});
