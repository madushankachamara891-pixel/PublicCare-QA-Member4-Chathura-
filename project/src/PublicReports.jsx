import { useEffect, useState } from 'react'
import { getPublicReports, reportStatusLabel } from './reportsApi.js'
import './PublicReports.css'

const PAGE_SIZE = 9

export default function PublicReports() {
  const [page, setPage] = useState(0)
  const [requestState, setRequestState] = useState({ page: null, data: null, error: '' })

  useEffect(() => {
    let active = true
    getPublicReports({ page, size: PAGE_SIZE })
      .then((data) => { if (active) setRequestState({ page, data, error: '' }) })
      .catch((requestError) => { if (active) setRequestState({ page, data: null, error: requestError.message }) })
    return () => { active = false }
  }, [page])

  const loading = requestState.page !== page
  const data = loading ? null : requestState.data
  const error = loading ? '' : requestState.error

  return <section className="public-reports-section" aria-labelledby="public-reports-title">
    <div className="public-reports-heading">
      <p className="section-kicker">COMMUNITY UPDATES</p>
      <h2 id="public-reports-title">Public reports</h2>
      <p className="public-reports-intro">Reports and progress updates approved for public viewing.</p>
    </div>
    {loading && <p className="public-reports-message" role="status">Loading public reports…</p>}
    {error && <p className="public-reports-message error" role="alert">Unable to load public reports. {error}</p>}
    {!loading && !error && data?.content?.length === 0 && <p className="public-reports-message">No public reports available yet</p>}
    {!loading && !error && data?.content?.length > 0 && <>
      <div className="public-reports-grid">
        {data.content.map((report) => <a className="public-report-card" href={`/public-reports/${encodeURIComponent(report.id)}`} key={report.id}>
          <div className="public-report-topline">
            <span className="public-report-title">{report.title}</span>
            <span className={`public-report-status ${report.status.toLowerCase().replaceAll('_', '-')}`}>{reportStatusLabel(report.status)}</span>
          </div>
          <p className="public-report-category">{report.category}</p>
          <p className="public-report-date">{report.location} · Reported {new Date(report.reportedAt).toLocaleDateString()}</p>
          {report.progressUpdates.length > 0 && <div className="public-progress-list">
            <h3>Approved progress updates</h3>
            <ol>{report.progressUpdates.map((update, updateIndex) => <li key={`${update.createdAt}-${updateIndex}`}>
              <div className="public-progress-meta">
                <span>{reportStatusLabel(update.status)}</span>
                <time dateTime={update.createdAt}>{new Date(update.createdAt).toLocaleDateString()}</time>
              </div>
              {update.note && <p>{update.note}</p>}
            </li>)}</ol>
          </div>}
        </a>)}
      </div>
      {data.totalPages > 1 && <nav className="public-reports-pagination" aria-label="Public report pages">
        <button type="button" disabled={page <= 0 || loading} onClick={() => setPage((value) => value - 1)}>Previous</button>
        <span>Page {page + 1} of {data.totalPages}</span>
        <button type="button" disabled={page + 1 >= data.totalPages || loading} onClick={() => setPage((value) => value + 1)}>Next</button>
      </nav>}
    </>}
  </section>
}
