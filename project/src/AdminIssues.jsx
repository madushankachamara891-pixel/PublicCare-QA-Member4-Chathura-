import { useCallback, useEffect, useState } from 'react'
import SiteHeader from './SiteHeader.jsx'
import { deleteAdminReport, getAdminReports, reportStatusLabel, setAdminReportPublicApproval } from './reportsApi.js'
import SummaryIllustration from './SummaryIllustration.jsx'
import './AdminIssues.css'

const PAGE_SIZE = 10
const statusClass = (status) => reportStatusLabel(status).toLowerCase().replaceAll(' ', '-')

function downloadCsv(reports) {
  const columns = [['Reference', 'reference'], ['Issue title', 'title'], ['Category', 'category'], ['Location', 'location'], ['Date reported', 'createdAt'], ['Status', 'status']]
  const escape = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`
  const content = [columns.map(([label]) => escape(label)).join(','), ...reports.map((report) => columns.map(([, key]) => escape(key === 'status' ? reportStatusLabel(report[key]) : report[key])).join(','))].join('\r\n')
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = 'publiccare-reports.csv'; link.click(); URL.revokeObjectURL(url)
}

export default function AdminIssues() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [sortOrder, setSortOrder] = useState('desc')
  const [page, setPage] = useState(0)
  const [approvalPendingId, setApprovalPendingId] = useState(null)
  const [approvalError, setApprovalError] = useState('')
  const [deletePendingId, setDeletePendingId] = useState(null)
  const [deleteError, setDeleteError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setData(await getAdminReports({ page, size: PAGE_SIZE, q: search.trim(), category, status, sort: 'createdAt', direction: sortOrder })) }
    catch (requestError) { setError(requestError.message) }
    finally { setLoading(false) }
  }, [category, page, search, sortOrder, status])
  useEffect(() => {
    let active = true
    getAdminReports({ page, size: PAGE_SIZE, q: search.trim(), category, status, sort: 'createdAt', direction: sortOrder })
      .then((result) => { if (active) { setData(result); setError('') } })
      .catch((requestError) => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [category, page, search, sortOrder, status])

  const summary = data?.summary || { total: 0, pending: 0, inProgress: 0, resolved: 0 }
  const categories = [...new Set(['Road damage', 'Waste & garbage', 'Streetlights', 'Drainage', 'Water supply', 'Other public issue', ...Object.keys(summary.categories || {})])].sort()
  const reports = data?.content || []
  function resetFilters() { setSearch(''); setCategory(''); setStatus(''); setSortOrder('desc'); setPage(0) }
  async function changePublicApproval(report) {
    const approved = !report.publicApproved
    setApprovalPendingId(report.id); setApprovalError('')
    try {
      const result = await setAdminReportPublicApproval(report.id, approved)
      setData((current) => current && ({ ...current, content: current.content.map((item) => item.id === report.id ? { ...item, publicApproved: result.approved } : item) }))
    } catch (requestError) { setApprovalError(requestError.message) }
    finally { setApprovalPendingId(null) }
  }

  async function deleteReport(report) {
    const confirmed = window.confirm(`Delete report ${report.reference}? This will permanently remove the report, its progress history, and uploaded photos.`)
    if (!confirmed) return
    setDeletePendingId(report.id); setDeleteError('')
    try {
      await deleteAdminReport(report.id)
      await load()
    } catch (requestError) { setDeleteError(requestError.message) }
    finally { setDeletePendingId(null) }
  }

  return <main className="site-shell admin-page admin-issues-page">
    <SiteHeader active="reports" adminView reportsView trackReportLabel />
    <section className="admin-banner admin-issues-banner"><div className="admin-banner-inner">
      <p className="admin-breadcrumbs"><a href="/">Home</a><span>/</span><a href="/admin">Admin</a><span>/</span><span>Issue management</span></p>
      <div className="admin-banner-row"><div><h1>Issue management</h1><p>View, search and manage public issue reports.</p></div></div>
    </div></section>
    <section className="admin-issues-content">
      <div className="admin-summary" aria-label="Issue summary">
        {[
          { label: 'Total Reports', value: summary.total, detail: 'All reports', tone: 'blue', illustration: 'total' },
          { label: 'Pending', value: summary.pending, detail: 'Awaiting review', tone: 'amber', illustration: 'pending' },
          { label: 'In Progress', value: summary.inProgress, detail: 'Work underway', tone: 'sky', illustration: 'in-progress' },
          { label: 'Resolved', value: summary.resolved, detail: 'Completed reports', tone: 'green', illustration: 'resolved' },
        ].map((stat) => <article className={`admin-stat-card ${stat.tone}`} key={stat.label}>
          <div className="admin-stat-copy"><div className="admin-stat-top"><span>{stat.label}</span></div><strong>{stat.value}</strong><small>{stat.detail}</small></div>
          <span className={`admin-stat-illustration ${stat.tone}`}><SummaryIllustration type={stat.illustration} /></span>
        </article>)}
      </div>
      <section className="admin-panel issue-management-panel">
        <div className="issues-panel-heading"><div><h2>All issue reports</h2><p>Search and filter reports, then choose an action for an issue.</p></div><button className="issues-export-button" type="button" onClick={() => downloadCsv(reports)} disabled={!reports.length}>Export current page</button></div>
        <div className="issues-filters" role="search">
          <label className="issues-search"><span aria-hidden="true">⌕</span><input type="search" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value) }} placeholder="Search by title, reference or location" aria-label="Search by title, reference or location" /></label>
          <select value={category} onChange={(event) => { setPage(0); setCategory(event.target.value) }} aria-label="Filter by category"><option value="">All categories</option>{categories.map((item) => <option key={item}>{item}</option>)}</select>
          <select value={status} onChange={(event) => { setPage(0); setStatus(event.target.value) }} aria-label="Filter by status"><option value="">All statuses</option>{[['PENDING', 'Pending'], ['IN_PROGRESS', 'In progress'], ['RESOLVED', 'Resolved']].map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select value={sortOrder} onChange={(event) => { setPage(0); setSortOrder(event.target.value) }} aria-label="Sort reports by date"><option value="desc">Newest first ↓</option><option value="asc">Oldest first ↑</option></select>
          <button className="issues-reset-button" type="button" onClick={resetFilters}>Reset</button>
        </div>
        {error && <p role="alert" className="update-save-error">{error} <button type="button" onClick={load}>Try again</button></p>}
        {approvalError && <p role="alert" className="update-save-error">{approvalError}</p>}
        {deleteError && <p role="alert" className="update-save-error">{deleteError}</p>}
        {loading && <p role="status">Loading reports…</p>}
        <div className="issues-table-wrap"><table className="issues-table">
          <thead><tr><th scope="col">Reference</th><th scope="col">Issue title</th><th scope="col">Category</th><th scope="col">Location</th><th scope="col">Date reported</th><th scope="col">Status</th><th scope="col">Public display</th><th scope="col">Actions</th></tr></thead>
          <tbody>{reports.map((report) => <tr key={report.id}><td><a className="issues-reference" href={`/admin/update-issue?id=${report.id}`}>{report.reference}</a></td><td className="issues-title-cell">{report.title}</td><td>{report.category}</td><td>{report.location}</td><td className="issues-date-cell">{new Date(report.createdAt).toLocaleDateString()}</td><td><span className={`issues-status ${statusClass(report.status)}`}>{reportStatusLabel(report.status)}</span></td><td><div className="issues-public-approval"><span>{report.publicApproved ? 'Approved' : 'Private'}</span><button type="button" onClick={() => changePublicApproval(report)} disabled={approvalPendingId === report.id}>{approvalPendingId === report.id ? 'Saving…' : report.publicApproved ? 'Revoke' : 'Approve'}</button></div></td><td><div className="issues-row-actions"><a href={`/admin/update-issue?id=${report.id}`}>View / update</a><button type="button" aria-label={`Delete report ${report.reference}`} disabled={deletePendingId !== null} onClick={() => deleteReport(report)}>{deletePendingId === report.id ? 'Deleting…' : 'Delete'}</button></div></td></tr>)}
          {!loading && !reports.length && <tr><td className="issues-empty" colSpan="8">No reports match your search or filters.</td></tr>}</tbody>
        </table></div>
        {data && <div className="issues-table-footer"><p>Showing {data.totalElements ? page * PAGE_SIZE + 1 : 0}–{Math.min((page + 1) * PAGE_SIZE, data.totalElements)} of {data.totalElements} reports</p><span className="issues-page-size">Rows per page: {PAGE_SIZE}</span><nav className="issues-pagination" aria-label="Report pages"><button type="button" aria-label="Previous page" disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>←</button><span>{page + 1} / {Math.max(data.totalPages, 1)}</span><button type="button" aria-label="Next page" disabled={page + 1 >= data.totalPages} onClick={() => setPage((value) => value + 1)}>→</button></nav></div>}
      </section>
      <aside className="issues-management-note"><strong>Report management</strong><span>Report changes are saved to the PublicCare database and are available to authorized administrators.</span></aside>
    </section>
    <footer className="footer"><strong>PublicCare</strong><span>A clearer path from reporting to resolution.</span></footer>
  </main>
}
