import { useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { getStatusProgress } from './reportsData.js'
import { getMyProgressImage, getMyProgressUpdates, getMyReport, getMyReportImage, reportStatusLabel } from './reportsApi.js'
import './ReportDetail.css'

export default function IssueDetails() {
  const id = new URLSearchParams(window.location.search).get('id')
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [imageUrl, setImageUrl] = useState('')
  const [imageLoading, setImageLoading] = useState(false)
  const [imageError, setImageError] = useState('')
  const [imageMissing, setImageMissing] = useState(false)
  const [updates, setUpdates] = useState([])
  const [updatesLoading, setUpdatesLoading] = useState(false)
  const [updatesError, setUpdatesError] = useState('')

  useEffect(() => {
    let active = true
    const historyUrls = []
    if (!id) return () => { active = false }
    let objectUrl = ''
    getMyReport(id).then(async (data) => {
      if (!active) return
      setReport(data)
      setUpdatesLoading(true)
      try {
        const history = await getMyProgressUpdates(data.id)
        const populated = await Promise.all(history.map(async (entry) => {
          if (!entry.hasImage) return entry
          try { const imageUrl = URL.createObjectURL(await getMyProgressImage(data.id, entry.id)); historyUrls.push(imageUrl); return { ...entry, imageUrl } }
          catch { return { ...entry, imageError: true } }
        }))
        if (active) setUpdates(populated)
      } catch (requestError) { if (active) setUpdatesError(requestError.message) }
      finally { if (active) setUpdatesLoading(false) }
      setImageLoading(true)
      try {
        const image = await getMyReportImage(id)
        if (active) {
          objectUrl = URL.createObjectURL(image)
          setImageUrl(objectUrl)
        }
      } catch (requestError) {
        if (active && requestError.status === 404) setImageMissing(true)
        else if (active) setImageError(requestError.message)
      } finally {
        if (active) setImageLoading(false)
      }
    }).catch((requestError) => { if (active) { setError(requestError.message); setImageLoading(false) } }).finally(() => { if (active) setLoading(false) })
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
      historyUrls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [id])

  const visibleError = error || (!id ? 'Report reference is missing.' : '')
  const status = report && reportStatusLabel(report.status)
  const progress = report && getStatusProgress(status)
  const date = (value) => value ? new Date(value).toLocaleString() : '—'

  return <main className="site-shell detail-page issue-tracking-page">
    <SiteHeader active="reports" reportsView />
    <section className="detail-banner tracking-banner"><div className="detail-inner">
      <p className="breadcrumbs"><a href="/">Home</a><span>/</span><a href="/my-reports">My reports</a><span>/</span><span>{report?.reference || 'Report details'}</span></p>
      <div className="tracking-banner-row"><div><h1>Issue details</h1><p>View the report and its latest status.</p></div>{report && <span className="reference-pill">REFERENCE&nbsp; {report.reference}</span>}</div>
    </div></section>
    <section className="tracking-content">
      {loading && id && <aside className="demo-data-notice" role="status">Loading report…</aside>}
      {visibleError && <aside className="demo-data-notice" role="alert"><strong>Report unavailable</strong><span>{visibleError}</span><a href="/my-reports">Return to My Reports</a></aside>}
      {report && <>
        <article className="issue-summary-card">
          <h2>{report.title}</h2>
          <div className="issue-summary-meta"><span className={`tracking-status ${status.toLowerCase().replaceAll(' ', '-')}`}>{status}</span><span>Reported on {date(report.createdAt)}</span></div>
          <div className="issue-facts"><div><small>CATEGORY</small><strong>{report.category}</strong></div><div><small>LOCATION</small><strong>{report.location}</strong></div></div>
          <section className="issue-copy"><h3>DESCRIPTION</h3><p>{report.description}</p></section>
          <section className="attached-photo" aria-label="Report photo">
            <h3>REPORT PHOTO</h3>
            {imageLoading && <p className="photo-state" role="status">Loading report photo…</p>}
            {imageUrl && <img className="report-photo" src={imageUrl} alt={`Photo attached to ${report.reference}`} />}
            {imageMissing && <p className="photo-state">No photo was attached to this report.</p>}
            {imageError && <p className="photo-state photo-error" role="alert">Unable to load the report photo. {imageError}</p>}
          </section>
        </article>
        <aside className="progress-card"><h2>Report progress</h2><p className="progress-intro">Status updates are provided by PublicCare administrators.</p>
          <span className={`tracking-status ${status.toLowerCase().replaceAll(' ', '-')}`}>{status}</span>
          <ol className="progress-timeline">{progress.steps.map((step, index) => {
            const complete = report.status === 'RESOLVED' ? index < 4 : report.status === 'IN_PROGRESS' ? index < 3 : index < 2
            const current = index === (report.status === 'RESOLVED' ? 3 : report.status === 'IN_PROGRESS' ? 2 : 1)
            return <li key={step.label} className={complete ? 'done' : current ? 'active' : 'upcoming'}><span className="timeline-marker">{complete ? '✓' : ''}</span><div><strong>{step.label}</strong><small>{index === 0 ? date(report.createdAt) : current ? 'Current status' : complete ? 'Completed' : 'Not yet reached'}</small></div></li>
          })}</ol>
          <div className={`updates-note${report.adminUpdateNote ? ' has-admin-update' : ''}`}>
            {report.adminUpdateNote
              ? <><strong>Latest Update from PublicCare</strong><p>{report.adminUpdateNote}</p></>
              : <p>Updates appear here when the report status changes.</p>}
          </div>
          <section className="citizen-progress-history"><h3>Progress history</h3>{updatesLoading && <p role="status">Loading updates…</p>}{updatesError && <p className="photo-error" role="alert">Unable to load progress updates. {updatesError}</p>}{!updatesLoading && !updatesError && updates.length === 0 && <p>No administrator progress updates yet.</p>}<ol>{updates.map((entry) => <li key={entry.id}><span className={`tracking-status ${reportStatusLabel(entry.status).toLowerCase().replaceAll(' ', '-')}`}>{reportStatusLabel(entry.status)}</span><time>{date(entry.createdAt)}</time>{entry.note && <p>{entry.note}</p>}{entry.imageUrl && <img className="progress-history-photo" src={entry.imageUrl} alt="Administrator progress update" />}{entry.imageError && <small role="alert">Unable to load this progress photo.</small>}</li>)}</ol></section>
        </aside>
        <section className="update-report-card"><div><h2>Need to correct the report?</h2><p>You can update its details while it remains associated with your account.</p></div><div className="tracking-buttons"><a className="back-reports-button" href="/my-reports">Back to My Reports</a><a className="edit-my-issue-button" href={`/edit-issue?id=${report.id}`}>Edit report</a></div></section>
      </>}
      <aside className="help-card"><span className="help-icon" aria-hidden="true">i</span><div><h2>Need help?</h2><p>If you have more information about this issue, contact the relevant local authority.</p></div></aside>
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
