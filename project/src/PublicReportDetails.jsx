import { useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { getPublicReport, reportStatusLabel } from './reportsApi.js'
import './PublicReportDetails.css'

const formatDate = (value) => value ? new Date(value).toLocaleDateString() : 'Date unavailable'

export default function PublicReportDetails({ id }) {
  const [state, setState] = useState({ id: null, report: null, error: '' })

  useEffect(() => {
    let active = true
    getPublicReport(id)
      .then((report) => { if (active) setState({ id, report, error: '' }) })
      .catch((error) => { if (active) setState({ id, report: null, error: error.message }) })
    return () => { active = false }
  }, [id])

  const loading = state.id !== id
  const report = loading ? null : state.report
  const error = loading ? '' : state.error
  const status = report ? reportStatusLabel(report.status) : ''

  return <main className="site-shell public-report-details-page">
    <SiteHeader active="home" />
    <section className="public-detail-banner">
      <div className="public-detail-inner">
        <p className="public-detail-breadcrumbs"><a href="/">Home</a><span>/</span><a href="/#public-reports-title">Public reports</a><span>/</span><span>Report details</span></p>
        <h1>{report?.title || 'Public report details'}</h1>
        <p>Approved community report</p>
      </div>
    </section>
    <section className="public-detail-content" aria-live="polite">
      {loading && <p className="public-detail-message" role="status">Loading report details…</p>}
      {!loading && error && <div className="public-detail-message error" role="alert"><strong>Report unavailable</strong><p>{error}</p><a href="/#public-reports-title">Back to public reports</a></div>}
      {!loading && report && <>
        <article className="public-detail-card">
          <div className="public-detail-heading">
            <div><p className="public-detail-kicker">{report.category}</p><h2>{report.title}</h2></div>
            <span className={`public-report-status ${report.status.toLowerCase().replaceAll('_', '-')}`}>{status}</span>
          </div>
          <dl className="public-detail-facts">
            <div><dt>Location</dt><dd>{report.location || 'Not provided'}</dd></div>
            <div><dt>Reported</dt><dd>{formatDate(report.reportedAt)}</dd></div>
          </dl>
          {report.description && <section className="public-detail-description"><h3>Description</h3><p>{report.description}</p></section>}
        </article>

        <section className="public-detail-card public-detail-updates">
          <h2>Public progress updates</h2>
          {report.progressUpdates.length === 0
            ? <p className="public-detail-muted">No approved progress updates are available yet.</p>
            : <ol>{report.progressUpdates.map((update, index) => <li key={`${update.createdAt}-${index}`}>
              <div className="public-update-heading"><strong>{reportStatusLabel(update.status)}</strong><time dateTime={update.createdAt}>{formatDate(update.createdAt)}</time></div>
              {update.note && <p>{update.note}</p>}
            </li>)}</ol>}
        </section>
        <a className="public-detail-back" href="/#public-reports-title">Back to public reports</a>
      </>}
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
