import { useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { getMyReport, updateMyReport } from './reportsApi.js'
import './ReportDetail.css'

export default function EditIssue() {
  const id = new URLSearchParams(window.location.search).get('id')
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    if (!id) return () => { active = false }
    getMyReport(id).then((data) => { if (active) setReport(data) }).catch((requestError) => { if (active) setError(requestError.message) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  async function saveChanges(event) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setSaving(true)
    setError('')
    try {
      await updateMyReport(id, { title: form.get('title'), category: form.get('category'), location: form.get('location'), description: form.get('description') })
      window.location.assign(`/issue-details?id=${encodeURIComponent(id)}`)
    } catch (requestError) { setError(requestError.message) }
    finally { setSaving(false) }
  }

  return <main className="site-shell detail-page edit-issue-page">
    <SiteHeader active="reports" reportsView />
    <section className="detail-banner edit-issue-banner"><div className="detail-inner">
      <p className="breadcrumbs"><a href="/">Home</a><span>/</span><a href="/my-reports">My reports</a><span>/</span>{report ? <a href={`/issue-details?id=${report.id}`}>{report.reference}</a> : <span>Report</span>}<span>/</span><span>Edit</span></p>
      <div className="edit-banner-row"><div><h1>Edit report</h1><p>Update the details for your public issue.</p></div>{report && <span className="reference-pill">REFERENCE&nbsp; {report.reference}</span>}</div>
    </div></section>
    <section className="edit-issue-layout">
      {loading && id && <aside className="demo-data-notice" role="status">Loading report…</aside>}
      {(error || (!id && 'Report reference is missing.')) && <aside className="demo-data-notice" role="alert"><strong>Report unavailable</strong><span>{error || 'Report reference is missing.'}</span><a href="/my-reports">Return to My Reports</a></aside>}
      {report && <form className="edit-issue-form" onSubmit={saveChanges}>
        <div className="edit-form-heading"><h2>Issue details</h2><p>Fields marked with * are required.</p></div>
        <label htmlFor="edit-title">Issue title *</label><input id="edit-title" name="title" defaultValue={report.title} maxLength="160" required />
        <label htmlFor="edit-category">Issue category *</label><select id="edit-category" name="category" defaultValue={report.category} maxLength="80" required><option>Road damage</option><option>Waste &amp; garbage</option><option>Streetlights</option><option>Drainage</option><option>Water supply</option><option>Other public issue</option></select>
        <label htmlFor="edit-description">Description *</label><textarea id="edit-description" name="description" defaultValue={report.description} rows="4" maxLength="5000" required />
        <p className="edit-field-hint">Describe what happened and why it needs attention.</p>
        <label htmlFor="edit-location">Location / address *</label><input id="edit-location" name="location" defaultValue={report.location} maxLength="240" required />
        <p className="edit-field-hint">Photo attachments are not supported yet.</p>
        {error && <p className="update-save-error" role="alert">{error}</p>}
        <div className="edit-issue-actions"><a className="edit-cancel-button" href={`/issue-details?id=${report.id}`}>Cancel</a><button className="edit-save-button" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button></div>
      </form>}
      {report && <aside className="edit-guidance"><section className="before-save-card"><div className="guidance-heading"><span className="guidance-icon">i</span><h2>Before you save</h2></div><p>Please check your report details carefully.</p><ol><li><span>01</span><div><strong>Be specific</strong><small>Use a clear title and accurate description.</small></div></li><li><span>02</span><div><strong>Check the location</strong><small>Add a street, town or nearby landmark.</small></div></li><li><span>03</span><div><strong>Protect your privacy</strong><small>Do not include private or sensitive information.</small></div></li></ol></section></aside>}
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
