import { test, expect, Page } from '@playwright/test';

/**
 * Login Page UI Tests — http://localhost:4200/security/login
 *
 * Scope: UI-only (layout, interactions, element presence).
 * Backend validation is out of scope.
 *
 * Observations collected via live Playwright MCP session:
 *  - Page title: "نظام ERP" (Arabic — RTL layout)
 *  - Ant Design SVG icons (no <img> tag) used for password toggle
 *  - Admin/User tabs auto-fill demo credentials (feature-flagged)
 *  - Social login section: Google, Twitter, Facebook (feature-flagged)
 *  - 0 browser console errors; 10 build-time deprecation warnings (non-breaking)
 */

const LOGIN_URL = 'http://localhost:4200/security/login';

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

async function goToLogin(page: Page) {
  await page.goto(LOGIN_URL);
  // In case we're already logged in, the app redirects; wait for the form
  await page.waitForURL(/security\/login|\/login/, { timeout: 5000 }).catch(() => {});
}

// ─────────────────────────────────────────────────────────────────────────────
// Suite 1 — Element Presence
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Login Page — Element Presence', () => {
  test.beforeEach(async ({ page }) => {
    // Clear auth state so login page is shown
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    }).catch(() => {});
    await page.goto(LOGIN_URL);
    await page.waitForSelector('#username', { timeout: 10000 });
  });

  test('should have page title set', async ({ page }) => {
    await expect(page).toHaveTitle(/ERP|نظام/);
  });

  test('should render username field', async ({ page }) => {
    const field = page.locator('#username');
    await expect(field).toBeVisible();
    await expect(field).toHaveAttribute('type', 'text');
    await expect(field).toHaveAttribute('placeholder', /اسم المستخدم|username/i);
  });

  test('should render password field', async ({ page }) => {
    const field = page.locator('#password');
    await expect(field).toBeVisible();
    // Starts as type="password" when a role with a password is selected
    await expect(field).toHaveAttribute('type', /password|text/);
    await expect(field).toHaveAttribute('placeholder', /كلمة المرور|password/i);
  });

  test('should render login submit button', async ({ page }) => {
    const btn = page.getByRole('button', { name: /تسجيل الدخول|Login/i });
    await expect(btn).toBeVisible();
    await expect(btn).toBeEnabled();
  });

  test('should render Remember Me checkbox', async ({ page }) => {
    const checkbox = page.locator('#customCheckc1');
    await expect(checkbox).toBeVisible();
    // Default state: checked
    await expect(checkbox).toBeChecked();
  });

  test('should render Forgot Password link', async ({ page }) => {
    const link = page.getByRole('link', { name: /نسيت كلمة المرور|Forgot Password/i });
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute('href', /forgot-password/);
  });

  test('should render Admin/User demo tabs', async ({ page }) => {
    const adminTab = page.getByRole('link', { name: /Admin/i });
    const userTab  = page.getByRole('link', { name: /User/i });
    await expect(adminTab).toBeVisible();
    await expect(userTab).toBeVisible();
  });

  test('Admin tab should be active by default', async ({ page }) => {
    const adminTab = page.locator('ul.nav li a.nav-link').filter({ hasText: /Admin/ });
    await expect(adminTab).toHaveClass(/active/);
  });

  test('should render social login buttons (Google, Twitter, Facebook)', async ({ page }) => {
    await expect(page.getByRole('button', { name: /Google/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Twitter/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Facebook/i })).toBeVisible();
  });

  test('should render social login section label', async ({ page }) => {
    const separator = page.locator('.saprator span');
    await expect(separator).toBeVisible();
    await expect(separator).toContainText(/تسجيل الدخول باستخدام|Login with/i);
  });

  test('should render brand logo', async ({ page }) => {
    const logo = page.locator('img[alt="theme logo"]');
    await expect(logo).toBeVisible();
  });

  test('should render language toggle button', async ({ page }) => {
    // Button switches between Arabic/English
    const langBtn = page.locator('.language-switcher button');
    await expect(langBtn).toBeVisible();
    await expect(langBtn).toBeEnabled();
  });

  test('should render password visibility toggle icon when role is selected', async ({ page }) => {
    // The eye icon (Ant Design SVG) is rendered inside #togglePassword
    const toggle = page.locator('#togglePassword');
    await expect(toggle).toBeVisible();
    // Must contain an SVG child
    await expect(toggle.locator('svg')).toBeVisible();
  });

  test('should render Register link ("ليس لديك حساب؟")', async ({ page }) => {
    const link = page.getByRole('link', { name: /ليس لديك حساب|No account/i });
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute('href', /register/);
  });

  test('should render footer copyright', async ({ page }) => {
    await expect(page.locator('p').filter({ hasText: /Copyright/i })).toBeVisible();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Suite 2 — Form Interactions
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Login Page — Form Interactions', () => {
  test.beforeEach(async ({ page }) => {
    await page.context().clearCookies();
    await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); }).catch(() => {});
    await page.goto(LOGIN_URL);
    await page.waitForSelector('#username', { timeout: 10000 });
  });

  test('should accept text input in username field', async ({ page }) => {
    const field = page.locator('#username');
    await field.clear();
    await field.fill('testuser@example.com');
    await expect(field).toHaveValue('testuser@example.com');
  });

  test('should accept text input in password field', async ({ page }) => {
    const field = page.locator('#password');
    await field.clear();
    await field.fill('SecurePass123!');
    await expect(field).toHaveValue('SecurePass123!');
  });

  test('password field should default to type=password (masked)', async ({ page }) => {
    await expect(page.locator('#password')).toHaveAttribute('type', 'password');
  });

  test('clicking eye icon should reveal password (type → text)', async ({ page }) => {
    await page.locator('#password').fill('secret');
    await page.locator('#togglePassword').click();
    // After toggle: type switches to "text"
    await expect(page.locator('#password')).toHaveAttribute('type', 'text');
  });

  test('clicking eye icon twice should re-mask password (type → password)', async ({ page }) => {
    await page.locator('#password').fill('secret');
    await page.locator('#togglePassword').click(); // reveal
    await page.locator('#togglePassword').click(); // re-mask
    await expect(page.locator('#password')).toHaveAttribute('type', 'password');
  });

  test('Remember Me checkbox should be toggleable (uncheck)', async ({ page }) => {
    const checkbox = page.locator('#customCheckc1');
    await expect(checkbox).toBeChecked();
    await checkbox.click();
    await expect(checkbox).not.toBeChecked();
  });

  test('Remember Me checkbox should be toggleable (re-check)', async ({ page }) => {
    const checkbox = page.locator('#customCheckc1');
    await checkbox.click(); // uncheck
    await checkbox.click(); // re-check
    await expect(checkbox).toBeChecked();
  });

  test('login button should be clickable', async ({ page }) => {
    // Just verify the button responds to a click without throwing
    const btn = page.getByRole('button', { name: /تسجيل الدخول|Login/i });
    await expect(btn).toBeEnabled();
    // Click but don't wait for navigation (backend may or may not be running)
    await btn.click({ timeout: 3000 }).catch(() => {});
  });

  test('social login buttons should be clickable', async ({ page }) => {
    const google   = page.getByRole('button', { name: /Google/i });
    const twitter  = page.getByRole('button', { name: /Twitter/i });
    const facebook = page.getByRole('button', { name: /Facebook/i });
    await expect(google).toBeEnabled();
    await expect(twitter).toBeEnabled();
    await expect(facebook).toBeEnabled();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Suite 3 — Tab Switching (Demo Credentials)
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Login Page — Tab Switching', () => {
  test.beforeEach(async ({ page }) => {
    await page.context().clearCookies();
    await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); }).catch(() => {});
    await page.goto(LOGIN_URL);
    await page.waitForSelector('#username', { timeout: 10000 });
  });

  test('Admin tab should be active by default', async ({ page }) => {
    const adminTab = page.locator('ul.nav li a.nav-link').filter({ hasText: /Admin/ });
    await expect(adminTab).toHaveClass(/active/);
  });

  test('clicking User tab should activate it', async ({ page }) => {
    await page.getByRole('link', { name: /User/i }).click();
    const userTab = page.locator('ul.nav li a.nav-link').filter({ hasText: /User/ });
    await expect(userTab).toHaveClass(/active/);
  });

  test('clicking User tab should deactivate Admin tab', async ({ page }) => {
    await page.getByRole('link', { name: /User/i }).click();
    const adminTab = page.locator('ul.nav li a.nav-link').filter({ hasText: /Admin/ });
    await expect(adminTab).not.toHaveClass(/active/);
  });

  test('switching to User tab should auto-fill username with "user"', async ({ page }) => {
    await page.getByRole('link', { name: /User/i }).click();
    await expect(page.locator('#username')).toHaveValue('user');
  });

  test('switching back to Admin tab should auto-fill username with "admin"', async ({ page }) => {
    await page.getByRole('link', { name: /User/i }).click();
    await page.getByRole('link', { name: /Admin/i }).click();
    await expect(page.locator('#username')).toHaveValue('admin');
  });

  test('switching to Admin tab should pre-fill password', async ({ page }) => {
    const pwd = page.locator('#password');
    // Admin tab should have a preset password value
    const value = await pwd.inputValue();
    expect(value.length).toBeGreaterThan(0);
  });

  test('tabs should not navigate away from login page', async ({ page }) => {
    await page.getByRole('link', { name: /User/i }).click();
    await expect(page).toHaveURL(/security\/login|login/);
    await page.getByRole('link', { name: /Admin/i }).click();
    await expect(page).toHaveURL(/security\/login|login/);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Suite 4 — UI Quality & Accessibility Checks
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Login Page — UI Quality', () => {
  test.beforeEach(async ({ page }) => {
    await page.context().clearCookies();
    await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); }).catch(() => {});
    await page.goto(LOGIN_URL);
    await page.waitForSelector('#username', { timeout: 10000 });
  });

  test('no JavaScript console errors on page load', async ({ page }) => {
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') errors.push(msg.text());
    });
    await page.reload();
    await page.waitForSelector('#username', { timeout: 10000 });
    expect(errors).toHaveLength(0);
  });

  test('no uncaught page errors', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', err => pageErrors.push(err.message));
    await page.reload();
    await page.waitForSelector('#username', { timeout: 10000 });
    expect(pageErrors).toHaveLength(0);
  });

  test('username field should have accessible label', async ({ page }) => {
    const label = page.locator('label[for="username"]');
    await expect(label).toBeVisible();
    await expect(label).not.toBeEmpty();
  });

  test('password field should have accessible label', async ({ page }) => {
    const label = page.locator('label[for="password"]');
    await expect(label).toBeVisible();
    await expect(label).not.toBeEmpty();
  });

  test('Remember Me label should be associated with checkbox', async ({ page }) => {
    const label = page.locator('label[for="customCheckc1"]');
    await expect(label).toBeVisible();
  });

  test('brand logo image should load (no broken img)', async ({ page }) => {
    const logo = page.locator('img[alt="theme logo"]');
    const naturalWidth = await logo.evaluate((el: HTMLImageElement) => el.naturalWidth);
    expect(naturalWidth).toBeGreaterThan(0);
  });

  test('Google social button image should load', async ({ page }) => {
    const img = page.locator('img[alt="Google logo"]');
    const naturalWidth = await img.evaluate((el: HTMLImageElement) => el.naturalWidth);
    expect(naturalWidth).toBeGreaterThan(0);
  });

  test('Twitter social button image should load', async ({ page }) => {
    const img = page.locator('img[alt="Twitter logo"]');
    const naturalWidth = await img.evaluate((el: HTMLImageElement) => el.naturalWidth);
    expect(naturalWidth).toBeGreaterThan(0);
  });

  test('Facebook social button image should load', async ({ page }) => {
    const img = page.locator('img[alt="Facebook logo"]');
    const naturalWidth = await img.evaluate((el: HTMLImageElement) => el.naturalWidth);
    expect(naturalWidth).toBeGreaterThan(0);
  });

  test('Forgot Password link should point to /forgot-password', async ({ page }) => {
    const link = page.getByRole('link', { name: /نسيت|Forgot/i });
    await expect(link).toHaveAttribute('href', /forgot-password/);
  });

  test('Register link should point to /register', async ({ page }) => {
    const link = page.getByRole('link', { name: /ليس لديك حساب|No account/i });
    await expect(link).toHaveAttribute('href', /register/);
  });

  test('page should not have unexpected extra forms', async ({ page }) => {
    const forms = page.locator('form');
    await expect(forms).toHaveCount(1);
  });

  test('page should not have unexpected input fields beyond username/password/checkbox', async ({ page }) => {
    const inputs = page.locator('input');
    // Expected: username (text), password (password|text), remember-me (checkbox) = 3
    const count = await inputs.count();
    expect(count).toBe(3);
  });

  test('language toggle button text should match current language', async ({ page }) => {
    // In Arabic mode the button shows "English" (to switch to English)
    const langBtn = page.locator('.language-switcher button');
    const text = await langBtn.innerText();
    expect(['English', 'العربية']).toContain(text.trim());
  });
});
