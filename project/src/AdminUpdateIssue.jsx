import { useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { addAdminProgressUpdate, getAdminProgressImage, getAdminProgressUpdates, getAdminReport, getAdminReportImage, reportStatusLabel, setAdminProgressNotePublicApproval, setAdminProgressPublicApproval, setAdminReportPublicApproval } from './reportsApi.js'
import './AdminUpdateIssue.css'

const statusOptions = [['PENDING', 'Pending'], ['IN_PROGRESS', 'In progress'], ['RESOLVED', 'Resolved']]
const statusClass = (value) => reportStatusLabel(value).toLowerCase().replaceAll(' ', '-')

export default function AdminUpdateIssue() {
  const id = new URLSearchParams(window.location.search).get('id')
  const [report, setReport] = useState(null)
  const [status, setStatus] = useState('PENDING')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [imageUrl, setImageUrl] = useState('')
  const [imageLoading, setImageLoading] = useState(false)
  const [imageMissing, setImageMissing] = useState(false)
  const [imageError, setImageError] = useState('')
  const [updates, setUpdates] = useState([])
  const [photo, setPhoto] = useState(null)
  const [preview, setPreview] = useState('')
  const [approvalPending, setApprovalPending] = useState('')

  useEffect(() => {
    let active = true
    const historyUrls = []
    if (!id) return () => { active = false }
    let objectUrl = ''
    getAdminReport(id).then(async (data) => {
      if (!active) return
      setReport(data)
      setStatus(data.status)
      setNote(data.adminUpdateNote || '')
      setImageLoading(true)
      try {
        const history = await getAdminProgressUpdates(data.id)
        if (active) {
          const populated = await Promise.all(history.map(async (entry) => {
            if (!entry.hasImage) return entry
            try { const imageUrl = URL.createObjectURL(await getAdminProgressImage(data.id, entry.id)); historyUrls.push(imageUrl); return { ...entry, imageUrl } }
            catch { return { ...entry, imageError: true } }
          }))
          if (active) setUpdates(populated)
        }
      } catch (requestError) { if (active) setError(requestError.message) }
      try {
        const image = await getAdminReportImage(data.id)
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
    })
      .catch((requestError) => { if (active) setError(requestError.message) }).finally(() => { if (active) setLoading(false) })
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
      historyUrls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [id])

  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])

  async function saveChanges(event) {
    event.preventDefault(); setSaving(true); setError(''); setSuccess('')
    try {
      const saved = await addAdminProgressUpdate(id, { status, note, image: photo })
      setReport((current) => ({ ...current, status: saved.status, adminUpdateNote: saved.note, publicApproved: current.status === saved.status ? current.publicApproved : false }))
      let imageUrl = ''
      if (saved.hasImage) { try { imageUrl = URL.createObjectURL(await getAdminProgressImage(id, saved.id)); } catch { /* Saved data remains available when the page reloads. */ } }
      const next = { ...saved, imageUrl }
      setUpdates((current) => [...current, next]); setPhoto(null); setSuccess(`Progress update for ${report.reference} was saved.`)
    } catch (requestError) { setError(requestError.message) }
    finally { setSaving(false) }
  }

  async function changeReportApproval(approved) {
    setApprovalPending('report'); setError(''); setSuccess('')
    try {
      const result = await setAdminReportPublicApproval(id, approved)
      setReport((current) => ({ ...current, publicApproved: result.approved }))
      setSuccess(result.approved ? 'Report approved for public display.' : 'Report removed from public display.')
    } catch (requestError) { setError(requestError.message) }
    finally { setApprovalPending('') }
  }

  async function changeProgressApproval(entry, approvalType, approved) {
    const pendingKey = `${entry.id}-${approvalType}`
    setApprovalPending(pendingKey); setError(''); setSuccess('')
    try {
      const result = approvalType === 'note'
        ? await setAdminProgressNotePublicApproval(id, entry.id, approved)
        : await setAdminProgressPublicApproval(id, entry.id, approved)
      const key = approvalType === 'note' ? 'notePublicApproved' : 'publicApproved'
      setUpdates((current) => current.map((item) => item.id === entry.id ? { ...item, [key]: result.approved } : item))
      setSuccess(approvalType === 'note'
        ? (result.approved ? 'Progress note approved for public display.' : 'Progress note removed from public display.')
        : (result.approved ? 'Progress update approved for public display.' : 'Progress update removed from public display.'))
    } catch (requestError) { setError(requestError.message) }
    finally { setApprovalPending('') }
  }

  const label = report && reportStatusLabel(report.status)
  return <main className="site-shell admin-page admin-update-page">
    <SiteHeader active="reports" adminView reportsView trackReportLabel />
    <section className="admin-banner admin-update-banner"><div className="admin-banner-inner">
      <p className="admin-breadcrumbs"><a href="/">Home</a><span>/</span><a href="/admin">Admin</a><span>/</span><a href="/admin/issues">Issue Management</a><span>/</span><span>{report?.reference || 'Update issue'}</span></p>
      <div className="admin-banner-row"><div><h1>Update Issue</h1><p>Review report details and update its status.</p></div>{report && <span className="update-reference">REFERENCE: {report.reference}</span>}</div>
    </div></section>
    <section className="admin-update-content">
      {loading && id && <p role="status">Loading report…</p>}
      {(error || (!id && 'Choose a report from Issue management.')) && <p role="alert" className="update-save-error">{error || 'Choose a report from Issue management.'} <a href="/admin/issues">Back to Issue Management</a></p>}
      {report && <>
        <form className="update-form-card" onSubmit={saveChanges}>
          <h2>Issue Information</h2>
          {success && <p className="update-save-success" role="status">{success}</p>}
          <div className="update-field-grid">
            <label htmlFor="update-id">Issue ID</label><input id="update-id" value={report.reference} readOnly />
            <label htmlFor="update-title">Issue Title</label><input id="update-title" value={report.title} readOnly />
            <label htmlFor="update-category">Category</label><input id="update-category" value={report.category} readOnly />
            <label htmlFor="update-description">Description</label><textarea id="update-description" value={report.description} readOnly rows="3" />
            <label htmlFor="update-location">Location / Address</label><input id="update-location" value={report.location} readOnly />
            <div className="admin-report-photo" aria-label="Report photo">
              <h3>Report Photo</h3>
              {imageLoading && <p role="status">Loading photo…</p>}
              {imageUrl && <img src={imageUrl} alt={`Photo attached to ${report.reference}`} />}
              {imageMissing && <p>No photo attached.</p>}
              {imageError && <p className="admin-report-photo-error" role="alert">Unable to load photo. {imageError}</p>}
            </div>
            <label htmlFor="update-owner">Reported by</label><input id="update-owner" value={report.ownerName} readOnly />
            <label htmlFor="current-status">Current Status</label><div className="current-status-wrap"><span id="current-status" className={`update-status-pill ${statusClass(report.status)}`}><i />{label}</span><small>Last updated {new Date(report.updatedAt).toLocaleString()}</small></div>
            <span className="public-approval-field-label">Public display</span><div className="public-approval-control"><span className={report.publicApproved ? 'approved' : ''}>{report.publicApproved ? 'Approved for public display' : 'Private'}</span><button type="button" onClick={() => changeReportApproval(!report.publicApproved)} disabled={approvalPending === 'report'}>{approvalPending === 'report' ? 'Saving…' : report.publicApproved ? 'Revoke approval' : 'Approve for public display'}</button></div>
            <label htmlFor="update-status">Update Status <b>*</b></label><div className="select-wrap"><select id="update-status" value={status} onChange={(event) => setStatus(event.target.value)} required>{statusOptions.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select></div>
            <span className="field-spacer" /><small className="update-hint">Change the current status of this issue.</small>
            <label htmlFor="admin-update-note">Admin Update Note</label><div className="note-wrap"><textarea id="admin-update-note" value={note} onChange={(event) => setNote(event.target.value)} maxLength="1000" rows="3" placeholder="Add a note about the current progress or actions taken." /><small>This note is visible to the report owner.</small></div>
            <label htmlFor="progress-photo">Progress photo <small>(optional)</small></label><div className="note-wrap"><input id="progress-photo" type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" onChange={(event) => { const selected = event.target.files?.[0]; const extension = selected?.name.toLowerCase().split('.').pop(); const supported = selected && (['image/jpeg', 'image/png'].includes(selected.type) || ['jpg', 'jpeg', 'png'].includes(extension)); setError(''); setPreview(selected ? URL.createObjectURL(selected) : ''); if (selected && (!supported || selected.size > 5 * 1024 * 1024)) { setPhoto(null); setPreview(''); setError('Choose a JPG or PNG image no larger than 5 MiB.'); event.target.value = ''; return } setPhoto(selected || null) }} /><small>JPG or PNG, maximum 5 MiB. The update is saved once with its photo.</small>{preview && <img className="progress-photo-preview" src={preview} alt="Selected progress photo preview" />}</div>
          </div>
          <div className="update-form-actions"><a href="/admin/issues">Cancel</a><button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save Changes'}</button></div>
        </form>
        <aside className="admin-update-sidebar">
          <section className="update-side-card citizen-card"><h2>Citizen Information</h2><p>{report.ownerName}</p><small>{report.ownerId ? `Account #${report.ownerId}` : ''}</small></section>
          <section className="update-side-card timeline-card"><h2>Progress updates</h2>{updates.length === 0 ? <p>No saved progress updates yet.</p> : <ol className="update-timeline">{updates.map((entry) => <li key={entry.id} className="complete"><span>✓</span><div><strong>{reportStatusLabel(entry.status)}</strong><small>{new Date(entry.createdAt).toLocaleString()}</small>{entry.note && <p>{entry.note}</p>}<div className="progress-public-controls"><div><span>{entry.publicApproved ? 'Public update approved' : 'Update private'}</span><button type="button" onClick={() => changeProgressApproval(entry, 'update', !entry.publicApproved)} disabled={approvalPending === `${entry.id}-update`}>{approvalPending === `${entry.id}-update` ? 'Saving…' : entry.publicApproved ? 'Revoke update approval' : 'Approve update'}</button></div><div><span>{entry.notePublicApproved ? 'Public note approved' : 'Note private'}</span>{entry.note?.trim() ? <button type="button" onClick={() => changeProgressApproval(entry, 'note', !entry.notePublicApproved)} disabled={approvalPending === `${entry.id}-note`}>{approvalPending === `${entry.id}-note` ? 'Saving…' : entry.notePublicApproved ? 'Revoke note approval' : 'Approve note'}</button> : <small>No note to approve.</small>}</div></div>{entry.imageUrl && <img className="progress-photo-preview" src={entry.imageUrl} alt="Administrator progress update" />}{entry.imageError && <small role="alert">Unable to load progress photo.</small>}</div></li>)}</ol>}</section>
          <section className="update-side-card helpful-card"><h2>Helpful Actions</h2><a href="/admin/issues">Back to issue management</a><button type="button" onClick={() => { setStatus('RESOLVED'); setError(''); setSuccess('') }}>Mark as resolved</button></section>
        </aside>
      </>}
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
