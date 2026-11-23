import { useEffect, useRef, useState } from 'react'
import { useAuth } from './authContext.js'
import { logoutAccount } from './authApi.js'
import PublicCareLogo from './PublicCareLogo.jsx'

function AccountMenuIcon({ name }) {
  const shared = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.7, strokeLinecap: 'round', strokeLinejoin: 'round' }
  if (name === 'user') return <svg viewBox="0 0 24 24" aria-hidden="true" {...shared}><circle cx="12" cy="8" r="3.2"/><path d="M5.5 20c.7-3.4 3-5.2 6.5-5.2s5.8 1.8 6.5 5.2"/></svg>
  if (name === 'reports') return <svg viewBox="0 0 24 24" aria-hidden="true" {...shared}><path d="M7 3.75h7l4 4V20H7z"/><path d="M14 3.75v4h4M9.5 12h6M9.5 15.5h6"/></svg>
  if (name === 'login') return <svg viewBox="0 0 24 24" aria-hidden="true" {...shared}><path d="M13.5 4.5h5v15h-5M3.5 12h10M9.5 8l4 4-4 4"/></svg>
  return <svg viewBox="0 0 24 24" aria-hidden="true" {...shared}><path d="M10.5 4.5h-5v15h5M20.5 12h-10M14.5 8l-4 4 4 4"/></svg>
}

function scrollToHomeSection(event, sectionId) {
  if (window.location.pathname !== '/') return

  event.preventDefault()
  window.history.pushState(null, '', `#${sectionId}`)
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  document.getElementById(sectionId)?.scrollIntoView({ behavior, block: 'start' })
}

export default function SiteHeader({ active = '', reportsView = false, adminView = false, trackReportLabel = false }) {
  const [accountMenuOpen, setAccountMenuOpen] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [logoutLoading, setLogoutLoading] = useState(false)
  const { user, setUser } = useAuth()
  const showAdminNavigation = adminView && user?.role === 'ADMIN'
  const accountMenuRef = useRef(null)
  const accountButtonRef = useRef(null)

  useEffect(() => {
    if (!accountMenuOpen) return undefined

    function closeOnOutsidePointer(event) {
      if (!accountMenuRef.current?.contains(event.target)) setAccountMenuOpen(false)
    }
    function closeOnEscape(event) {
      if (event.key === 'Escape') {
        setAccountMenuOpen(false)
        accountButtonRef.current?.focus()
      }
    }

    document.addEventListener('pointerdown', closeOnOutsidePointer)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsidePointer)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [accountMenuOpen])

  async function handleLogout() {
    setLogoutError('')
    setLogoutLoading(true)
    try {
      await logoutAccount()
      setUser(null)
      window.location.assign('/')
    } catch (error) {
      setLogoutError(error.message)
      setAccountMenuOpen(true)
    } finally {
      setLogoutLoading(false)
    }
  }

  function handleAccountMenuKeyDown(event) {
    if (!accountMenuOpen) return
    const menuItems = accountMenuRef.current?.querySelectorAll('[role="menuitem"]')
    if (!menuItems?.length) return
    const currentIndex = Array.from(menuItems).indexOf(document.activeElement)
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      const direction = event.key === 'ArrowDown' ? 1 : -1
      const nextIndex = (currentIndex + direction + menuItems.length) % menuItems.length
      menuItems[nextIndex].focus()
    } else if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault()
      menuItems[event.key === 'Home' ? 0 : menuItems.length - 1].focus()
    }
  }

  function handleAccountButtonKeyDown(event) {
    if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return
    event.preventDefault()
    const openingKey = event.key
    setAccountMenuOpen(true)
    requestAnimationFrame(() => {
      const items = accountMenuRef.current?.querySelectorAll('[role="menuitem"]')
      if (!items?.length) return
      items[openingKey === 'ArrowDown' ? 0 : items.length - 1].focus()
    })
  }

  const accountMenuId = showAdminNavigation ? 'shared-admin-menu' : 'shared-account-menu'
  const homeSectionPrefix = window.location.pathname === '/' ? '' : '/'

  return <header className={`topbar${showAdminNavigation ? ' admin-topbar' : ''}`}>
    <a className="brand" href="/" aria-label="PublicCare home"><PublicCareLogo className="brand-mark"/><span className="brand-copy"><strong>PublicCare</strong><small>A better way to report local issues</small></span></a>
    <nav className={`navigation${showAdminNavigation ? ' admin-navigation' : ''}`} aria-label="Main navigation">
      {showAdminNavigation ? <><a className={active === 'admin' ? 'active' : ''} aria-current={active === 'admin' ? 'page' : undefined} href="/admin">Admin Dashboard</a><a className={active === 'reports' ? 'active' : ''} aria-current={active === 'reports' ? 'page' : undefined} href="/admin/issues">Manage Issues</a></> : <>
        <a className={active === 'home' ? 'active' : ''} href="/">Home</a><a href={`${homeSectionPrefix}#how-it-works`} onClick={(event) => scrollToHomeSection(event, 'how-it-works')}>How it works</a><a href={`${homeSectionPrefix}#issue-categories`} onClick={(event) => scrollToHomeSection(event, 'issue-categories')}>Issue categories</a><a className={active === 'reports' ? 'active' : ''} href="/my-reports">{reportsView || trackReportLabel ? 'Track a report' : 'My Reports'}</a>
      </>}
    </nav>
    <div className="nav-actions">{showAdminNavigation ? <div className="admin-nav-actions"><div className="account-menu-wrap" ref={accountMenuRef} onKeyDown={handleAccountMenuKeyDown} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setAccountMenuOpen(false) }}>
      <button ref={accountButtonRef} className="login account-link" type="button" aria-haspopup="dialog" aria-expanded={accountMenuOpen} aria-controls={accountMenuId} onClick={() => setAccountMenuOpen((open) => !open)} onKeyDown={handleAccountButtonKeyDown}>
        Admin account<span className={`account-chevron${accountMenuOpen ? ' is-open' : ''}`} aria-hidden="true" />
      </button>
      <div className="account-dropdown" id={accountMenuId} role="dialog" aria-label="Admin account" hidden={!accountMenuOpen}>
        <div className="account-dropdown-heading" role="presentation"><AccountMenuIcon name="user"/><span><strong>{user.fullName}</strong><small>Administrator</small></span></div>
      </div>
      </div><button className="get-started" type="button" onClick={handleLogout} disabled={logoutLoading}>{logoutLoading ? 'Signing out…' : 'Log out'}</button></div> : user ? <div className="account-menu-wrap" ref={accountMenuRef} onKeyDown={handleAccountMenuKeyDown} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setAccountMenuOpen(false) }}>
      <button ref={accountButtonRef} className="login account-link" type="button" aria-label="My account" aria-haspopup="menu" aria-expanded={accountMenuOpen} aria-controls={accountMenuId} onClick={() => setAccountMenuOpen((open) => !open)} onKeyDown={handleAccountButtonKeyDown}>
        My account<span className={`account-chevron${accountMenuOpen ? ' is-open' : ''}`} aria-hidden="true" />
      </button>
      <div className="account-dropdown" id={accountMenuId} role="menu" aria-label="My account" hidden={!accountMenuOpen} onClick={(event) => { if (event.target.closest('a[role="menuitem"]')) setAccountMenuOpen(false) }}>
        <div className="account-dropdown-heading" role="presentation"><AccountMenuIcon name="user"/><span><strong>{user.fullName}</strong><small>{user.role === 'ADMIN' ? 'Administrator' : 'Citizen account'}</small></span><i className="account-chevron is-open" aria-hidden="true"/></div>
        {user.role === 'ADMIN' && <><a className="account-menu-item" role="menuitem" href="/admin">Dashboard</a><a className="account-menu-item" role="menuitem" href="/admin/issues">Issue management</a></>}
        <a className="account-menu-item" role="menuitem" href="/my-reports"><AccountMenuIcon name="reports"/><span>My Reports</span></a>
        <div className="account-menu-divider" role="separator" />
        <button className="account-menu-item" role="menuitem" type="button" onClick={handleLogout} disabled={logoutLoading}><AccountMenuIcon name="logout"/><span>{logoutLoading ? 'Signing out…' : 'Log out'}</span></button>
        {logoutError && <p className="account-menu-error" role="alert">{logoutError}</p>}
      </div>
    </div> : <><a className="login" href="/login">Log in</a><a className="get-started" href="/create-issue">Get started</a></>}</div>
  </header>
}
