import { useState } from 'react'
import { useAuth } from './authContext.js'
import { loginAccount, registerAccount } from './authApi.js'
import './ReportDetail.css'
import PublicCareLogo from './PublicCareLogo.jsx'

function AccountHeader() {
  return <header className="account-header">
    <a className="account-brand" href="/" aria-label="PublicCare home"><PublicCareLogo className="account-brand-mark"/><span><strong>PublicCare</strong><small>A better way to report local issues</small></span></a>
    <a className="back-home-link" href="/">← Back to home</a>
  </header>
}

function AccountFooter() {
  return <footer className="account-footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
}

function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const { setUser } = useAuth()

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage('')
    const form = new FormData(event.currentTarget)
    try {
      const user = await loginAccount({ email: form.get('email'), password: form.get('password') })
      setUser(user)
      window.location.assign(user.role === 'ADMIN' ? '/admin' : '/my-reports')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return <main className="site-shell account-page login-page">
    <AccountHeader />
    <div className="login-canvas">
      <aside className="login-welcome-panel"><div>
        <PublicCareLogo className="login-mark"/><h1>Welcome back to<br/><strong>PublicCare</strong></h1>
        <p className="welcome-copy">Sign in to report local issues, manage your submissions and follow updates from your community.</p>
        <ol className="welcome-steps"><li><span>01</span><div><strong>Report public issues</strong><small>Tell your local community what needs attention.</small></div></li><li><span>02</span><div><strong>Track progress</strong><small>See updates as your report is handled.</small></div></li><li><span>03</span><div><strong>Stay informed</strong><small>Follow reports from submission to resolution.</small></div></li></ol>
      </div><div className="welcome-footer-card"><strong>A clearer path from reporting to resolution.</strong><small>PublicCare helps citizens share local issues with transparency.</small></div></aside>
      <section className="login-form-card"><div className="login-form-heading"><h2>Log in to your account</h2><p>Enter your details to continue to PublicCare.</p></div>
        <form onSubmit={handleSubmit}><label htmlFor="login-email">Email address</label><input id="login-email" name="email" type="email" autoComplete="email" placeholder="you@example.com" maxLength="254" required/><label htmlFor="login-password">Password</label>
          <div className="password-field"><input id="login-password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" required/><button type="button" aria-pressed={showPassword} onClick={() => setShowPassword((value) => !value)}>{showPassword ? 'Hide' : 'Show'}</button></div>
          <div className="login-options"><span className="remember-option">Your session uses a secure, HTTP-only cookie.</span></div>
          <button className="login-submit" type="submit" disabled={loading} aria-busy={loading}>{loading ? 'Signing in…' : 'Log in'}</button>{message && <p className="login-feedback" role="alert">{message}</p>}
        </form>
        <div className="login-divider"><span>OR</span></div><p className="create-account-prompt">Don’t have an account? <a href="/register">Create an account</a></p>
        <aside className="secure-signin"><strong>Secure sign in</strong><p>Your password is sent to the PublicCare backend and is never stored in this browser.</p></aside>
        <p className="login-terms">By continuing, you agree to use PublicCare responsibly.</p>
      </section>
    </div><AccountFooter />
  </main>
}

function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [feedback, setFeedback] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    const form = event.currentTarget
    const password = form.elements.password.value
    const confirmation = form.elements.confirmPassword
    confirmation.setCustomValidity(password === confirmation.value ? '' : 'Passwords do not match.')
    if (!form.reportValidity()) return

    setLoading(true)
    setFeedback('')
    try {
      await registerAccount({
        fullName: form.elements.fullName.value,
        email: form.elements.email.value,
        password,
      })
      setFeedback('Your Citizen account has been created. Log in to start using PublicCare.')
      form.reset()
    } catch (error) {
      setFeedback(error.message)
    } finally {
      setLoading(false)
    }
  }

  const registrationSucceeded = feedback.startsWith('Your Citizen')

  return <main className="site-shell account-page login-page register-page">
    <AccountHeader />
    <div className="login-canvas register-canvas">
      <aside className="login-welcome-panel register-welcome-panel"><div>
        <PublicCareLogo className="login-mark"/><h1>Join your community.</h1>
        <p className="welcome-copy">Create a citizen account to report public issues and follow their progress.</p>
        <ol className="welcome-steps"><li><span>01</span><div><strong>Easy reporting</strong><small>Share issue details and location.</small></div></li><li><span>02</span><div><strong>Personal report history</strong><small>Keep your submitted reports together.</small></div></li><li><span>03</span><div><strong>Transparent updates</strong><small>Follow status changes over time.</small></div></li></ol>
      </div><div className="welcome-footer-card"><strong>Citizen account</strong><small>Public registration creates a Citizen account only. Admin access is assigned separately.</small></div></aside>
      <section className="login-form-card register-form-card">
        <div className="login-form-heading"><h2>Create your account</h2><p>Join PublicCare and help improve your community.</p></div>
        <form onSubmit={handleSubmit}>
          <label htmlFor="register-name">Full name</label><input id="register-name" name="fullName" autoComplete="name" placeholder="Enter your full name" maxLength="120" required/>
          <label htmlFor="register-email">Email address</label><input id="register-email" name="email" type="email" autoComplete="email" placeholder="you@example.com" maxLength="254" required/>
          <label htmlFor="register-password">Password</label><div className="password-field"><input id="register-password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" minLength="12" maxLength="72" placeholder="Create a strong password" required/><button type="button" aria-pressed={showPassword} onClick={() => setShowPassword((value) => !value)}>{showPassword ? 'Hide' : 'Show'}</button></div>
          <p className="register-field-hint">Use at least 12 characters (maximum 72).</p>
          <label htmlFor="register-confirm-password">Confirm password</label><input id="register-confirm-password" name="confirmPassword" type={showPassword ? 'text' : 'password'} autoComplete="new-password" placeholder="Re-enter your password" required onChange={(event) => event.currentTarget.setCustomValidity('')}/>
          <label className="terms-option"><input name="terms" type="checkbox" required/><span>I agree to the Terms of Use and Privacy Notice.</span></label>
          <button className="login-submit register-submit" type="submit" disabled={loading} aria-busy={loading}>{loading ? 'Creating account…' : 'Create citizen account'}</button>
          {feedback && <p className={`register-feedback${registrationSucceeded ? ' success' : ''}`} role={registrationSucceeded ? 'status' : 'alert'}>{feedback}</p>}
        </form>
        <p className="create-account-prompt register-login-prompt">Already have an account? <a href="/login">Log in</a></p>
        <aside className="secure-signin register-security"><strong>Your details stay connected to your account</strong><p>You will only be able to edit reports submitted from your own account.</p></aside>
        <p className="register-validation-note">Your public registration creates a Citizen account. Admin access is assigned separately.</p>
        <p className="register-support-note">Need help? Contact the PublicCare support team.</p>
      </section>
    </div><AccountFooter />
  </main>
}

export default function AccountPage({ mode }) {
  return mode === 'register' ? <RegisterPage /> : <LoginPage />
}
