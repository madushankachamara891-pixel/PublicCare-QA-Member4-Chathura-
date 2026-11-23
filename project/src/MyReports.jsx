import { useCallback, useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { useAuth } from './authContext.js'
import { getMyReports, reportStatusLabel } from './reportsApi.js'
import './MyReports.css'

function ReportHistoryState({ title, children, action, alert = false }) {
  return <section className="report-history-state" role={alert ? 'alert' : 'status'}><span className="report-history-mark" aria-hidden="true">i</span><div className="report-history-copy"><h2>{title}</h2><p>{children}</p>{action}</div></section>
}

export default function MyReports() {
  const { user, loading: authLoading, error: authError, refresh } = useAuth()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try { setData(await getMyReports({ page, size: 10, status, q: query.trim(), sort: 'createdAt', direction: 'desc' })) }
    catch (requestError) { setError(requestError.message) }
    finally { setLoading(false) }
  }, [page, query, status])

  useEffect(() => {
    let active = true
    if (user) getMyReports({ page, size: 10, status, q: query.trim(), sort: 'createdAt', direction: 'desc' })
      .then((result) => { if (active) { setData(result); setError('') } })
      .catch((requestError) => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, query, status, user])

  return <main className="site-shell reports-page">
    <SiteHeader active="reports" reportsView />
    <section className="reports-hero"><div className="reports-hero-inner">
      <p className="breadcrumbs"><a href="/">Home</a><span aria-hidden="true">/</span><span>My reports</span></p>
      <div className="reports-title-row"><div><h1>My reports</h1><p>View and follow up on issues connected to your account.</p></div><a className="new-report-button" href="/create-issue">Open report form</a></div>
    </div></section>
    <section className="reports-content" aria-live="polite">
      {authLoading && <ReportHistoryState title="Checking your account">Please wait while PublicCare checks your current session.</ReportHistoryState>}
      {!authLoading && authError && <ReportHistoryState title="We couldn’t check your account" alert action={<button className="report-retry-button" type="button" onClick={refresh}>Try again</button>}>{authError}</ReportHistoryState>}
      {!authLoading && !authError && !user && <ReportHistoryState title="Sign in to view your report history" action={<a className="report-state-link" href="/login">Log in</a>}>Your reports are linked to your PublicCare account.</ReportHistoryState>}
      {!authLoading && !authError && user && <>
        {error && <ReportHistoryState title="Could not load reports" alert action={<button className="report-retry-button" type="button" onClick={load}>Try again</button>}>{error}</ReportHistoryState>}
        <section className="reports-table-card">
          <div className="reports-section-heading"><div><h2>Your report history</h2><p>Signed in as {user.email}</p></div></div>
          <div className="reports-filters"><label>Search reports<input type="search" value={query} onChange={(event) => { setPage(0); setQuery(event.target.value) }} placeholder="Title, reference or location" /></label><label>Status<select value={status} onChange={(event) => { setPage(0); setStatus(event.target.value) }}><option value="">All statuses</option><option value="PENDING">Pending</option><option value="IN_PROGRESS">In progress</option><option value="RESOLVED">Resolved</option></select></label></div>
          {loading && <p role="status">Loading your reports…</p>}
          {!loading && data && <>
            <div className="report-counts" aria-label="Report status summary"><span>Total <strong>{data.summary.total}</strong></span><span>Pending <strong>{data.summary.pending}</strong></span><span>In progress <strong>{data.summary.inProgress}</strong></span><span>Resolved <strong>{data.summary.resolved}</strong></span></div>
            {data.content.length ? <div className="reports-list">{data.content.map((report) => <article className="report-list-item" key={report.id}>
              <div><a className="report-list-reference" href={`/issue-details?id=${report.id}`}>{report.reference}</a><h3><a href={`/issue-details?id=${report.id}`}>{report.title}</a></h3><p>{report.category} · {report.location}</p><small>Reported {new Date(report.createdAt).toLocaleDateString()}</small></div>
              <span className={`tracking-status ${reportStatusLabel(report.status).toLowerCase().replaceAll(' ', '-')}`}>{reportStatusLabel(report.status)}</span>
            </article>)}</div> : <p className="report-api-note">No reports match these filters.</p>}
            <div className="reports-pagination"><span>Showing {data.totalElements ? page * data.size + 1 : 0}–{Math.min((page + 1) * data.size, data.totalElements)} of {data.totalElements}</span><button type="button" disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>Previous</button><button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div>
          </>}
        </section>
      </>}
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
