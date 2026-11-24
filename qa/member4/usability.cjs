const { chromium } = require('playwright');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({executablePath: process.env.CHROMIUM_PATH || undefined});
  try {
    const page = await browser.newPage({viewport: {width: 390, height: 844}});
    // Deterministic anonymous session fixture. This walkthrough evaluates the UI.
    await page.route('**/api/auth/me', route => route.fulfill({status: 401, json: {error: 'Authentication required.'}}));
    await page.goto('http://127.0.0.1:5173/create-issue');
    const signIn = page.getByRole('status').getByRole('link', {name: 'sign in'});
    await signIn.waitFor();
    assert.ok(await page.getByRole('button', {name:'Submit report'}).isDisabled());
    console.log('FR-03.8 PASS: anonymous submission disabled; adjacent sign-in link available.');
    await signIn.click();
    await page.getByLabel('Email address').focus();
    await page.keyboard.press('Tab');
    assert.equal(await page.locator(':focus').getAttribute('id'), 'login-password');
    const toggle = page.getByRole('button', {name:'Show'});
    await toggle.click();
    assert.equal(await page.locator('#login-password').getAttribute('type'), 'text');
    await page.getByRole('button', {name:'Hide'}).click();
    assert.equal(await page.locator('#login-password').getAttribute('type'), 'password');
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    console.log('FR-02 PASS: keyboard moves from email to password; visibility toggle works both ways; login fits 390 CSS pixels.');
  } finally {await browser.close();}
})().catch(error => {console.error(error.message); process.exitCode=1;});
