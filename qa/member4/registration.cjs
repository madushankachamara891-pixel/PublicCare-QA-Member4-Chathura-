// Start the Vite app on loopback. Run with Playwright installed in Node's module path.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
(async () => {
  const browser = await chromium.launch({executablePath: process.env.CHROMIUM_PATH || undefined});
  try {
    const page = await browser.newPage({viewport: {width: 1100, height: 900}});
    await page.goto('http://127.0.0.1:5173/register');
    await page.getByLabel('Full name').fill('QA Citizen');
    await page.getByLabel('Email address').fill('qa@example.invalid');
    const first = page.locator('#register-password');
    const confirm = page.locator('#register-confirm-password');
    await first.fill('SyntheticOne123');
    await confirm.fill('SyntheticTwo123');
    await page.getByRole('checkbox').check();
    const submit = page.getByRole('button', {name: 'Create citizen account'});
    await submit.click();
    assert.equal(await confirm.evaluate(e => e.validity.customError), true);
    // Observe submit eligibility without creating a database account.
    await page.locator('form').evaluate(form => form.addEventListener('submit', event => {
      event.preventDefault(); event.stopImmediatePropagation(); form.dataset.submitted = 'yes';
    }, {capture: true}));
    await first.fill('SyntheticTwo123');
    await submit.click();
    const result = await confirm.evaluate(e => ({
      valuesMatch: e.value === e.form.elements.password.value,
      message: e.validationMessage, submitted: e.form.dataset.submitted === 'yes'
    }));
    console.log(JSON.stringify({browser: browser.version(), ...result}));
    if (process.env.SCREENSHOT) await page.screenshot({path: path.resolve(process.env.SCREENSHOT)});
    assert.equal(result.submitted, true, 'Correcting either password field must allow submission');
  } finally { await browser.close(); }
})().catch(error => {console.error(error.message); process.exitCode = 1;});
