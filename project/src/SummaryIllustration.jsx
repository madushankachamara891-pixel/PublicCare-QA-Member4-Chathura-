export default function SummaryIllustration({ type }) {
  if (type === 'pending') return (
    <svg viewBox="0 0 72 56" aria-hidden="true" focusable="false">
      <path d="M18 45h35" stroke="#d9e4e8" strokeWidth="2" strokeLinecap="round" />
      <circle cx="35" cy="27" r="17" fill="#fff8e7" stroke="#d9a43b" strokeWidth="1.7" />
      <path d="M35 17v11l7 4" fill="none" stroke="#173b5b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M29 6h12m-12 42h12" stroke="#d9a43b" strokeWidth="2" strokeLinecap="round" />
      <circle cx="35" cy="27" r="1.5" fill="#13866f" />
    </svg>
  )

  if (type === 'in-progress') return (
    <svg viewBox="0 0 72 56" aria-hidden="true" focusable="false">
      <path d="M15 45l7-34h28l7 34" fill="#edf3f5" stroke="#7890a0" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M36 16v6m0 5v6m0 5v3" stroke="#fff" strokeWidth="2" strokeLinecap="round" />
      <path d="M18 39h36" stroke="#b8c8cf" strokeWidth="1.4" />
      <path d="M9 42l5-11 5 11z" fill="#f4ca6e" stroke="#c8892c" strokeWidth="1.4" strokeLinejoin="round" />
      <path d="M53 42l5-11 5 11z" fill="#f4ca6e" stroke="#c8892c" strokeWidth="1.4" strokeLinejoin="round" />
      <path d="M14 38h1m43 0h1" stroke="#fff" strokeWidth="1.4" strokeLinecap="round" />
      <path d="M28 11h16" stroke="#13866f" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )

  if (type === 'resolved') return (
    <svg viewBox="0 0 72 56" aria-hidden="true" focusable="false">
      <path d="M17 7h25l10 10v29H17z" fill="#fff" stroke="#7890a0" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M42 7v11h10" fill="#e4f5ef" stroke="#7890a0" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M23 25h15m-15 6h10" stroke="#173b5b" strokeWidth="1.7" strokeLinecap="round" />
      <circle cx="46" cy="38" r="11" fill="#dff4e8" stroke="#13866f" strokeWidth="1.6" />
      <path d="m41 38 3.5 3.5 6.5-7" fill="none" stroke="#13866f" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )

  return (
    <svg viewBox="0 0 72 56" aria-hidden="true" focusable="false">
      <path d="M18 7h26l10 10v32H18z" fill="#fff" stroke="#7890a0" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M44 7v11h10" fill="#e5f2fb" stroke="#7890a0" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M25 25h17m-17 7h17m-17 7h10" stroke="#173b5b" strokeWidth="1.7" strokeLinecap="round" />
      <path d="m24 24 1.5 1.5 2.5-3m-4 10 1.5 1.5 2.5-3m-4 10 1.5 1.5 2.5-3" fill="none" stroke="#13866f" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
