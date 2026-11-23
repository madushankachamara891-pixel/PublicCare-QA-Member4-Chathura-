import { useEffect, useState } from 'react'
import './CreateIssue.css'
import SiteHeader from './SiteHeader.jsx'
import { useAuth } from './authContext.js'
import { createReport, uploadReportImage } from './reportsApi.js'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png']

function CreateIssue() {
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')
  const [imageSelectionError, setImageSelectionError] = useState('')
  const [imageUploadError, setImageUploadError] = useState('')
  const [imageUploaded, setImageUploaded] = useState(false)
  const [imageUploadFailed, setImageUploadFailed] = useState(false)
  const [selectedImage, setSelectedImage] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [saving, setSaving] = useState(false)
  const [uploadingImage, setUploadingImage] = useState(false)
  const { user, loading } = useAuth()

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl)
  }, [previewUrl])

  function handleImageChange(event) {
    const file = event.currentTarget.files?.[0]
    setImageSelectionError('')
    if (!file) {
      setSelectedImage(null)
      setPreviewUrl('')
      return
    }
    const allowedExtension = /\.(jpe?g|png)$/i.test(file.name)
    const allowedType = !file.type || ACCEPTED_IMAGE_TYPES.includes(file.type)
    if (!allowedExtension || !allowedType) {
      setSelectedImage(null)
      setPreviewUrl('')
      event.currentTarget.value = ''
      setImageSelectionError('Choose a JPG or PNG image.')
      return
    }
    if (file.size === 0 || file.size > MAX_IMAGE_BYTES) {
      setSelectedImage(null)
      setPreviewUrl('')
      event.currentTarget.value = ''
      setImageSelectionError(file.size === 0 ? 'The selected image file is empty.' : 'The image must be 5 MiB or smaller.')
      return
    }
    setSelectedImage(file)
    setPreviewUrl(URL.createObjectURL(file))
  }

  function removeSelectedImage() {
    setSelectedImage(null)
    setPreviewUrl('')
    setImageSelectionError('')
    const input = document.getElementById('issue-photo')
    if (input) input.value = ''
  }

  async function uploadSelectedImage(reportId, file) {
    setImageUploadFailed(false)
    setImageUploadError('')
    setUploadingImage(true)
    try {
      await uploadReportImage(reportId, file)
      setImageUploaded(true)
    } catch (uploadError) {
      setImageUploaded(false)
      setImageUploadFailed(true)
      setImageUploadError(uploadError.message)
    } finally {
      setUploadingImage(false)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!event.currentTarget.reportValidity()) return
    if (imageSelectionError) return
    const form = new FormData(event.currentTarget)
    setSaving(true)
    setError('')
    try {
      const createdReport = await createReport({ title: form.get('title'), category: form.get('category'), description: form.get('description'), location: form.get('location') })
      setReport(createdReport)
      if (selectedImage) await uploadSelectedImage(createdReport.id, selectedImage)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSaving(false)
    }
  }

  return <main className="site-shell create-issue-page">
    <SiteHeader />
    <section className="issue-banner"><div className="issue-banner-inner">
      <p className="breadcrumbs"><a href="/">Home</a><span>/</span><span>Report an issue</span></p>
      <div className="issue-banner-row"><div><h1>Report a public issue</h1><p>Tell us what needs attention in your community.</p></div><span className="step-pill">STEP 1 OF 2</span></div>
    </div></section>

    <section className="issue-layout">
      <form className="issue-form-card" onSubmit={handleSubmit}>
        <div className="form-heading"><h2>Issue details</h2><p>Fields marked with * are required.</p></div>
        <label className="field-label" htmlFor="issue-title">Issue title *</label>
        <input id="issue-title" name="title" required maxLength="160" placeholder="e.g. Large pothole near the school" />
        <label className="field-label" htmlFor="issue-category">Issue category *</label>
        <select id="issue-category" name="category" required defaultValue="">
          <option value="" disabled>Select an issue category</option><option>Road damage</option><option>Waste &amp; garbage</option><option>Streetlights</option><option>Drainage</option><option>Water supply</option><option>Other public issue</option>
        </select>
        <label className="field-label" htmlFor="issue-description">Description *</label>
        <textarea id="issue-description" name="description" required maxLength="5000" placeholder="Describe the issue, what happened, and why it needs attention." />
        <p className="field-hint description-hint">Please avoid including private or sensitive personal information.</p>
        <label className="field-label" htmlFor="issue-location">Location / address *</label>
        <input id="issue-location" name="location" required maxLength="240" placeholder="Street, town or nearby landmark" />
        <label className="field-label photo-label" htmlFor="issue-photo">Photo (optional)</label>
        <label className={`upload-box${report ? ' upload-disabled' : ''}`} htmlFor="issue-photo" aria-disabled={Boolean(report)}>
          <span className="upload-icon" aria-hidden="true">+</span>
          <span className="upload-copy"><strong>{selectedImage ? selectedImage.name : 'Add a photo of the issue'}</strong><small>{selectedImage ? `${(selectedImage.size / (1024 * 1024)).toFixed(2)} MiB` : 'JPG or PNG, up to 5 MiB'}</small></span>
          {!selectedImage && <span className="browse-link">Browse files</span>}
        </label>
        <input className="file-input" id="issue-photo" name="image" type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" onChange={handleImageChange} disabled={Boolean(report)} aria-describedby="photo-upload-hint" />
        <p className="field-hint upload-hint" id="photo-upload-hint">One optional JPG or PNG image, maximum 5 MiB.</p>
        {selectedImage && previewUrl && <div className="selected-photo-preview">
          <img src={previewUrl} alt={`Preview of ${selectedImage.name}`} />
          {!report && <button type="button" className="remove-photo-button" onClick={removeSelectedImage}>Remove photo</button>}
        </div>}
        {!loading && !user && <p className="demo-notice" role="status">Please <a href="/login">sign in</a> before submitting a report.</p>}
        {error && <p className="demo-notice" role="alert">{error}</p>}
        {imageSelectionError && <p className="demo-notice upload-failed" role="alert">{imageSelectionError}</p>}
        {imageUploadError && <p className="demo-notice upload-failed" role="alert">Report {report?.reference} was created, but its image upload failed: {imageUploadError}</p>}
        {report && !imageUploadFailed && <p className="demo-notice" role="status">Report {report.reference} was submitted{imageUploaded ? ' with its photo' : ''}. <a href={`/issue-details?id=${report.id}`}>View report</a></p>}
        {report && imageUploadFailed && selectedImage && <button className="retry-image-button" type="button" disabled={saving || uploadingImage} onClick={() => uploadSelectedImage(report.id, selectedImage)}>{uploadingImage ? 'Uploading…' : 'Retry image upload'}</button>}
        <div className="form-actions"><a className="cancel-button" href="/">Cancel</a><button className="submit-button" type="submit" disabled={saving || loading || !user || Boolean(report)}>{saving ? 'Submitting…' : report ? 'Report submitted' : 'Submit report'}</button></div>
      </form>

      <aside className="issue-sidebar">
        <section className="why-card"><div className="why-heading"><span className="info-icon">i</span><h2>Why report an issue?</h2></div><p className="why-intro">Your report helps local services understand what needs attention.</p>
          <ol className="why-list"><li><span>01</span><div><strong>Be specific</strong><small>Include a clear location and useful details.</small></div></li><li><span>02</span><div><strong>Share useful details</strong><small>A clear description helps explain the issue.</small></div></li><li><span>03</span><div><strong>Track progress</strong><small>Keep your reference number after submitting.</small></div></li></ol>
        </section>
        <section className="privacy-card"><h2>Your information matters</h2><p>Only share details needed to identify the public issue. Avoid including private information.</p><span>DESIGNED FOR PUBLIC REPORTING</span></section>
      </aside>
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}

export default CreateIssue
