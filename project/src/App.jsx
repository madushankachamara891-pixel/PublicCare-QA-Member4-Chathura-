import { useEffect } from 'react'
import './App.css'
import CreateIssue from './CreateIssue.jsx'
import MyReports from './MyReports.jsx'
import IssueDetails from './IssueDetails.jsx'
import EditIssue from './EditIssue.jsx'
import AccountPage from './AccountPage.jsx'
import AdminDashboard from './AdminDashboard.jsx'
import AdminIssues from './AdminIssues.jsx'
import AdminUpdateIssue from './AdminUpdateIssue.jsx'
import SiteHeader from './SiteHeader.jsx'
import { useAuth } from './authContext.js'
import PublicReports from './PublicReports.jsx'
import PublicReportDetails from './PublicReportDetails.jsx'
import CategoryIllustration from './CategoryIllustration.jsx'

function AdminAccessGate({ children }) {
  const { user, loading, error } = useAuth()
  if (loading) return <main className="site-shell auth-gate" aria-live="polite">Checking your PublicCare session…</main>
  if (user?.role !== 'ADMIN') return <main className="site-shell auth-gate"><section><h1>Administrator access required</h1><p>{error || 'Sign in with an administrator account to open this page.'}</p><a className="button primary" href="/login">Log in</a></section></main>
  return children
}

const highlights = [
  { value: 'Report', label: 'an issue in your area' },
  { value: 'Track', label: 'your submitted reports' },
  { value: 'Follow', label: 'progress on your reports' },
  { value: 'Transparent', label: 'Updates you can follow' },
]

const categories = [
  { title: 'Road damage', description: 'Potholes and damaged roads', tone: 'blue', illustration: 'road' },
  { title: 'Waste & garbage', description: 'Missed collections and litter', tone: 'mint', illustration: 'waste' },
  { title: 'Streetlights', description: 'Lights that are broken or off', tone: 'amber', illustration: 'streetlight' },
  { title: 'Drainage', description: 'Blocked drains and flooding', tone: 'purple', illustration: 'drain' },
]

const steps = [
  { number: '01', title: 'Report the issue', description: 'Tell us what happened and where.' },
  { number: '02', title: 'Follow progress', description: 'Check status updates at any time.' },
  { number: '03', title: 'See the outcome', description: 'Know when the issue is resolved.' },
]

function App() {
  const path = window.location.pathname
  useEffect(() => {
    const sectionId = window.location.hash.slice(1)
    if (!sectionId) return

    requestAnimationFrame(() => {
      document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }, [])

  const publicReportMatch = path.match(/^\/public-reports\/(\d+)\/?$/)
  if (publicReportMatch) return <PublicReportDetails id={publicReportMatch[1]} />
  if (path === '/admin') return <AdminAccessGate><AdminDashboard /></AdminAccessGate>
  if (path === '/admin/issues') return <AdminAccessGate><AdminIssues /></AdminAccessGate>
  if (path === '/admin/update-issue') return <AdminAccessGate><AdminUpdateIssue /></AdminAccessGate>
  if (path === '/create-issue') return <CreateIssue />
  if (path === '/my-reports') return <MyReports />
  if (path === '/issue-details') return <IssueDetails />
  if (path === '/edit-issue') return <EditIssue />
  if (path === '/login' || path === '/register') return <AccountPage mode={path.slice(1)} />

  return (
    <main className="site-shell">
      <SiteHeader active="home" />

      <section className="hero-section" id="home">
        <div className="hero-content">
          <div className="hero-copy">
            <span className="eyebrow-pill">CITIZEN ISSUE REPORTING</span>
            <h1>Let's make our<br className="desktop-break" /> communities better.</h1>
            <p className="hero-lead">Report public issues in your area and follow every update.</p>
            <p className="hero-sub">Together, we can help build cleaner, safer places across Sri Lanka.</p>
            <div className="hero-actions"><a className="button primary" href="/create-issue"><span>＋</span> Report an issue</a><a className="button secondary" href="/my-reports">Track a report <span>→</span></a></div>
            <p className="hero-note">Free to use&nbsp; • &nbsp;Simple&nbsp; • &nbsp;Transparent</p>
          </div>
          <div className="overview-card">
            <div className="overview-heading"><div><h2>Your community, in focus</h2><p>Location preview · Galle, Sri Lanka</p></div><span className="live-badge">DEMONSTRATION</span></div>
            <div className="community-overview-content">
              <div className="community-map-frame">
                <iframe
                  className="community-map"
                  title="OpenStreetMap preview centred on Galle, Sri Lanka"
                  src="https://www.openstreetmap.org/export/embed.html?bbox=80.205%2C6.020%2C80.235%2C6.045&layer=mapnik"
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
                <span className="illustrative-marker marker-one" aria-label="Illustrative road damage marker">1</span>
                <span className="illustrative-marker marker-two" aria-label="Illustrative streetlight marker">2</span>
                <span className="illustrative-marker marker-three" aria-label="Illustrative drainage marker">3</span>
                <span className="map-attribution">© OpenStreetMap contributors</span>
              </div>
              <div className="community-map-details">
                <p className="illustrative-note">Illustrative markers only. No citizen reports are shown.</p>
                <ul className="marker-key" aria-label="Illustrative marker categories">
                  <li><span>1</span> Road damage</li>
                  <li><span>2</span> Streetlight</li>
                  <li><span>3</span> Drainage</li>
                </ul>
                <article className="issue-preview">
                  <span className="example-label">EXAMPLE ISSUE</span>
                  <h3>Blocked roadside drain</h3>
                  <p className="example-location">Galle, Sri Lanka</p>
                  <span className="status-badge">Example status · Under review</span>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="stats-section" aria-label="PublicCare features">
        <div className="stats-card">{highlights.map((highlight) => <div className="stat" key={highlight.label}><strong>{highlight.value}</strong><span>{highlight.label}</span></div>)}</div>
      </section>

      <section className="categories-section" id="issue-categories">
        <p className="section-kicker">REPORT A PUBLIC ISSUE</p>
        <h2>What needs attention?</h2>
        <p className="section-description">Choose a category to help us understand the problem.</p>
        <div className="category-grid">{categories.map((category) => <a className="category-card" href="/create-issue" key={category.title}>
          <span className={`category-icon ${category.tone}`}><CategoryIllustration type={category.illustration} /></span><strong>{category.title}</strong>
          <span className="category-description">{category.description}</span><span className="category-arrow">↗</span>
        </a>)}</div>
        <p className="sample-note category-note">Example issue categories</p>
      </section>

      <PublicReports />

      <section className="process-section" id="how-it-works">
        <div className="process-intro"><p className="section-kicker">SIMPLE AND TRANSPARENT</p><h2>From report to resolution</h2></div>
        <div className="steps-grid">{steps.map((step) => <article className="step" key={step.number}><span className="step-number">{step.number}</span><h3>{step.title}</h3><p>{step.description}</p></article>)}</div>
      </section>

      <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
    </main>
  )
}

export default App
