export default function CategoryIllustration({ type }) {
  if (type === 'road') return (
    <svg viewBox="0 0 56 44" aria-hidden="true" focusable="false">
      <path d="M4 35h48M10 35l5-19h26l5 19" fill="#e8eef1" stroke="#7890a0" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M24 21c3-2 8-2 11 0l3 4-3 3H24l-3-3z" fill="#173b5b" stroke="#102e4a" strokeWidth="1.4" strokeLinejoin="round" />
      <path d="M13 30l3-9m27 9-3-9" stroke="#fff" strokeWidth="1.5" strokeDasharray="3 3" />
      <path d="M5 34l4-10 5 10z" fill="#ef9b43" stroke="#c87726" strokeWidth="1.3" strokeLinejoin="round" />
      <path d="M7 30h5" stroke="#fff8ed" strokeWidth="1.6" />
    </svg>
  )

  if (type === 'waste') return (
    <svg viewBox="0 0 56 44" aria-hidden="true" focusable="false">
      <path d="M7 16h25l-2 22H9z" fill="#dff2e9" stroke="#13866f" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M5 13h29v4H5zm8-4h13l2 4H11z" fill="#13866f" stroke="#0c705d" strokeWidth="1.2" strokeLinejoin="round" />
      <path d="M15 21v12m9-12v12" stroke="#88bda9" strokeWidth="1.5" />
      <path d="M39 19l3-4 3 4-2 2 3 2-3 4-2-2-3 2-3-4 2-2z" fill="#8dc5a7" stroke="#4c9d79" strokeWidth="1.2" strokeLinejoin="round" />
      <path d="M38 32h13m-10-4 3 4 3-4" stroke="#173b5b" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )

  if (type === 'streetlight') return (
    <svg viewBox="0 0 56 44" aria-hidden="true" focusable="false">
      <path d="M24 39V10h16c4 0 6 2 6 5v2" stroke="#173b5b" strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M40 14h8v5h-8z" fill="#f9dc8d" stroke="#b78326" strokeWidth="1.4" strokeLinejoin="round" />
      <path d="M43 22l-4 8m9-8 2 8m-9-12v3" stroke="#e8b94d" strokeWidth="1.4" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M18 39h13" stroke="#13866f" strokeWidth="2" fill="none" strokeLinecap="round" />
      <circle cx="44" cy="16.5" r="1.5" fill="#fff8dc" />
    </svg>
  )

  if (type === 'other') return (
    <svg viewBox="0 0 56 44" aria-hidden="true" focusable="false">
      <path d="M13 7h22l8 8v23H13z" fill="#fff" stroke="#7890a0" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M35 7v9h8" fill="#e4f5ef" stroke="#7890a0" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M19 23h17m-17 5h12" stroke="#173b5b" strokeWidth="1.6" fill="none" strokeLinecap="round" />
      <path d="M31 35l5-8 5 8z" fill="#dff2e9" stroke="#13866f" strokeWidth="1.4" strokeLinejoin="round" />
      <path d="M36 30v2m0 1v.2" stroke="#13866f" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )

  return (
    <svg viewBox="0 0 56 44" aria-hidden="true" focusable="false">
      <path d="M6 15h43v20H6z" fill="#e8eef1" stroke="#607b8b" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M12 19v12m8-12v12m8-12v12m8-12v12m8-12v12M9 23h37m-37 5h37" stroke="#173b5b" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M3 11c4 0 4 3 8 3m-6-7c4 0 4 3 8 3m34 8c3 1 3 4 6 4m-8 2c3 1 3 4 6 4" stroke="#138c72" strokeWidth="1.7" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M4 38h48" stroke="#a9bbc4" strokeWidth="1.5" fill="none" strokeLinecap="round" />
    </svg>
  )
}
