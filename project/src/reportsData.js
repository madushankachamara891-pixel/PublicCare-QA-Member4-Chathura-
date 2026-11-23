// Browser-only sample fixtures used by the issue workflow demos. These are not account reports or backend data.
export const demoReports = [
  { id: 'PC-2048', title: 'Streetlight not working', category: 'Streetlights', location: 'Galle', detailLocation: 'Galle, Southern Province', date: '24 Oct 2026', status: 'In progress', updated: '27 Oct 2026', description: 'The streetlight near the community road is not working at night, making the area difficult to use safely.', additionalInformation: 'The issue is near the junction by the public playground.', photo: 'streetlight.jpg' },
  { id: 'PC-2042', title: 'Garbage not collected', category: 'Waste & garbage', location: 'Matara', date: '22 Oct 2026', status: 'Pending', updated: '25 Oct 2026', description: 'The scheduled garbage collection was missed on this street.', additionalInformation: 'Residents report this was the scheduled collection day.' },
  { id: 'PC-2037', title: 'Large pothole near school', category: 'Road damage', location: 'Galle', date: '19 Oct 2026', status: 'Resolved', updated: '23 Oct 2026', description: 'A large pothole near the school entrance needs attention.', additionalInformation: 'The pothole is beside the school entrance.' },
  { id: 'PC-2029', title: 'Blocked roadside drain', category: 'Drainage', location: 'Weligama', date: '15 Oct 2026', status: 'Resolved', updated: '20 Oct 2026', description: 'The roadside drain is blocked and water collects after rainfall.', additionalInformation: 'Water pools along the roadside after heavy rain.' },
]

const supportedStatuses = ['Pending', 'In progress', 'Resolved']

export function normalizeStatus(status) {
  const normalized = String(status ?? '').trim().toLocaleLowerCase().replace(/[\s_-]+/g, '')
  return supportedStatuses.find((value) => value.toLocaleLowerCase().replaceAll(' ', '') === normalized) || null
}

export function getStatusProgress(status) {
  const normalized = normalizeStatus(status)
  const currentStep = normalized === 'Pending' ? 1 : normalized === 'In progress' ? 2 : normalized === 'Resolved' ? 3 : -1
  return {
    status: normalized,
    steps: [
      { label: 'Issue submitted', complete: true, current: false },
      { label: 'Under review', complete: currentStep >= 2, current: currentStep === 1 },
      { label: 'In progress', complete: currentStep >= 3, current: currentStep === 2 },
      { label: 'Resolved', complete: normalized === 'Resolved', current: normalized === 'Resolved' },
    ],
  }
}

export function getDemoReportById(id) {
  const report = demoReports.find((item) => item.id === id)
  if (!report) return null
  try {
    const edits = JSON.parse(localStorage.getItem(`publiccare-${report.id}`) || '{}')
    return { ...report, ...edits, id: report.id, status: normalizeStatus(edits.status) || normalizeStatus(report.status), detailLocation: edits.location || report.detailLocation }
  } catch { return report }
}

export function getSelectedDemoReport() {
  const id = new URLSearchParams(window.location.search).get('id')
  return getDemoReportById(id) || getDemoReportById(demoReports[0].id)
}

export function getDemoReports() {
  return demoReports.map((report) => getDemoReportById(report.id))
}

export function saveDemoReportUpdates(id, updates) {
  const report = demoReports.find((item) => item.id === id)
  const status = normalizeStatus(updates?.status)
  if (!report || !status || typeof updates?.adminUpdateNote !== 'string') return false
  let existing
  try {
    existing = JSON.parse(localStorage.getItem(`publiccare-${id}`) || '{}')
    if (!existing || typeof existing !== 'object' || Array.isArray(existing)) return false
  } catch { return false }
  try {
    const updated = new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).replace(',', '')
    localStorage.setItem(`publiccare-${id}`, JSON.stringify({ ...existing, ...updates, status, updated, id: report.id }))
    return true
  } catch {
    return false
  }
}
