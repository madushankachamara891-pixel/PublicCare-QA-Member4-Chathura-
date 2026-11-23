import { useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { getAdminReports, getAdminSummary, reportStatusLabel } from './reportsApi.js'
import './AdminDashboard.css'
import CategoryIllustration from './CategoryIllustration.jsx'
import SummaryIllustration from './SummaryIllustration.jsx'

function categoryIllustrationType(category) {
  const normalizedCategory = category.toLowerCase()
  if (normalizedCategory.includes('road')) return 'road'
  if (normalizedCategory.includes('streetlight')) return 'streetlight'
  if (normalizedCategory === 'other' || normalizedCategory.includes('other ')) return 'other'
  return null
}

function ActivityStatusIcon({ status }) {
  if (status === 'In progress') {
    return <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M3 12.5a7 7 0 0 1 14 0M2.5 13h15M10 5.5v2M5.8 7.4l1.1 1.1m7.3-1.1-1.1 1.1" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" /></svg>
  }
  if (status === 'Resolved') {
    return <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><circle cx="10" cy="10" r="7.25" stroke="currentColor" strokeWidth="1.6" /><path d="m6.7 10.1 2.1 2.1 4.5-4.5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>
  }
  return <span className="activity-dot" aria-hidden="true" />
}

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null)
  const [reports, setReports] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    Promise.all([getAdminSummary(), getAdminReports({ page: 0, size: 5, sort: 'createdAt', direction: 'desc' })])
      .then(([counts, recent]) => { if (active) { setSummary(counts); setReports(recent.content) } })
      .catch((requestError) => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const stats = summary ? [
    { label: 'Total Issues', value: summary.total, detail: 'All reports', symbol: '#', tone: 'blue' },
    { label: 'Pending', value: summary.pending, detail: 'Awaiting review', symbol: '◷', tone: 'amber' },
    { label: 'In Progress', value: summary.inProgress, detail: 'Currently being handled', symbol: '↻', tone: 'sky' },
    { label: 'Resolved', value: summary.resolved, detail: 'Successfully resolved', symbol: '✓', tone: 'green' },
  ] : []
  const categories = Object.entries(summary?.categories || {}).sort((a, b) => b[1] - a[1])

  return <main className="site-shell admin-page">
    <SiteHeader active="admin" adminView reportsView />
    <section className="admin-banner"><div className="admin-banner-inner">
      <p className="admin-breadcrumbs"><a href="/">Home</a><span>/</span><span>Admin</span><span>/</span><span>Dashboard</span></p>
      <div className="admin-banner-row"><div><h1>Admin Dashboard</h1><p>Overview of public issues and management activities.</p></div></div>
    </div></section>
    <section className="admin-content">
      {error && <p role="alert" className="update-save-error">{error}</p>}{loading && <p role="status">Loading report summary…</p>}
      <div className="admin-summary" aria-label="Issue summary">{stats.map((stat) => <article className={`admin-stat-card ${stat.tone}`} key={stat.label}><div className="admin-stat-copy"><div className="admin-stat-top"><span>{stat.label}</span></div><strong>{stat.value}</strong><small>{stat.detail}</small></div><span className={`admin-stat-illustration ${stat.tone}`}><SummaryIllustration type={stat.label === 'Total Issues' ? 'total' : stat.label.toLowerCase().replace(' ', '-')} /></span></article>)}</div>
      <div className="admin-overview-grid">
        <section className="admin-panel category-panel"><h2>Issues by Category</h2><ul className="admin-category-list">{categories.map(([category, count], index) => <li key={category}><span className={`admin-category-icon category-${index % 5}`}>{categoryIllustrationType(category) ? <CategoryIllustration type={categoryIllustrationType(category)} /> : category.slice(0, 2)}</span><span>{category}</span><strong>{count}</strong></li>)}{!categories.length && <li>No reports yet</li>}</ul></section>
        <section className="admin-panel activity-panel"><div className="admin-panel-heading"><h2>Recent Activity</h2><a href="/admin/issues">View all activity <span aria-hidden="true">→</span></a></div><ol className="activity-list">{reports.slice(0, 4).map((report) => { const status = reportStatusLabel(report.status); return <li key={report.id}><span className={`activity-marker ${status.toLowerCase().replaceAll(' ', '-')}`}><ActivityStatusIcon status={status} /></span><span className="activity-copy"><strong><a className="activity-report-link" href={`/admin/update-issue?id=${report.id}`}>{report.reference} · {report.title}</a></strong><small>{status}</small></span><time>{new Date(report.updatedAt).toLocaleDateString()}</time></li> })}</ol>{!reports.length && <p>No recent reports.</p>}</section>
      </div>
      <section className="admin-panel recent-issues"><div className="admin-panel-heading"><h2>Recent Issues</h2><a href="/admin/issues">View all issues <span aria-hidden="true">→</span></a></div><div className="admin-table-wrap"><table className="admin-table">
        <thead><tr><th scope="col">ID</th><th scope="col">Title</th><th scope="col">Category</th><th scope="col">Location</th><th scope="col">Status</th><th scope="col">Reported Date</th><th scope="col">View</th></tr></thead>
        <tbody>{reports.map((report) => <tr key={report.id}><td><a className="admin-reference" href={`/admin/update-issue?id=${report.id}`}>{report.reference}</a></td><td>{report.title}</td><td>{report.category}</td><td>{report.location}</td><td><span className={`admin-status ${reportStatusLabel(report.status).toLowerCase().replaceAll(' ', '-')}`}>{reportStatusLabel(report.status)}</span></td><td>{new Date(report.createdAt).toLocaleDateString()}</td><td><a className="admin-view-link" href={`/admin/update-issue?id=${report.id}`}>View</a></td></tr>)}{!loading && !reports.length && <tr><td colSpan="7">No reports have been submitted.</td></tr>}</tbody>
      </table></div></section>
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
