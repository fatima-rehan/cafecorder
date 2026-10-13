import { useCallback, useEffect, useState } from 'react'
import { api, hasToken, setToken, setUnauthorizedHandler } from './api.js'
import AuthPage from './components/AuthPage.jsx'
import Landing from './components/Landing.jsx'
import MyVisits from './components/MyVisits.jsx'

// No router library: three screens and two tabs are just state
export default function App() {
  const [screen, setScreen] = useState('landing') // 'landing' | 'auth' | 'app'
  const [tab, setTab] = useState('explore') // 'explore' | 'mine'
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(hasToken()) // checking a saved token on load
  const [menuOpen, setMenuOpen] = useState(false)
  const [composing, setComposing] = useState(false)
  const [authMode, setAuthMode] = useState('login')

  const showAuth = useCallback((mode = 'login') => {
    setAuthMode(mode)
    setScreen('auth')
  }, [])

  const logOut = useCallback(() => {
    setToken(null)
    setUser(null)
    setMenuOpen(false)
    setTab('explore')
  }, [])

  // any 401 on a signed-in request: the token expired or the account is gone, so log out
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logOut()
      setScreen((s) => (s === 'app' ? 'auth' : s))
    })
  }, [logOut])

  // a token saved from last time: make sure it still works
  useEffect(() => {
    if (!hasToken()) return
    api.auth.me()
      .then(setUser)
      .catch(() => {})
      .finally(() => setChecking(false))
  }, [])

  const composeDone = useCallback(() => setComposing(false), [])

  function enter() {
    if (user || checking) {
      setTab(user ? 'mine' : 'explore')
      setScreen('app')
    } else {
      showAuth()
    }
  }

  function authed(u) {
    setUser(u)
    setTab('mine')
    setScreen('app')
  }

  function openTab(next) {
    if (next === 'mine' && !user) {
      showAuth()
      return
    }
    setTab(next)
  }

  if (screen === 'landing') return <Landing onEnter={enter} />

  if (screen === 'auth') {
    return (
      <AuthPage
        key={authMode}
        initialMode={authMode}
        onAuthed={authed}
        onBack={() => setScreen('landing')}
        onBrowse={() => {
          setTab('explore')
          setScreen('app')
        }}
      />
    )
  }

  const showMine = tab === 'mine' && user

  return (
    <div className="app">
      <header className="topbar">
        <button className="brand" onClick={() => setScreen('landing')} aria-label="Back to the landing page">
          <span className="brand-mark" />
          CafeCorder
        </button>

        <nav className="tabs" aria-label="Sections">
          <button className={!showMine ? 'active' : ''} onClick={() => openTab('explore')}>Explore</button>
          <button className={showMine ? 'active' : ''} onClick={() => openTab('mine')}>My visits</button>
        </nav>

        <div className="topbar-actions">
          {user ? (
            <>
              {showMine && (
                <button className="btn btn-primary" onClick={() => setComposing(true)}>
                  <span className="hide-narrow">Log a visit</span>
                  <span className="show-narrow" aria-hidden="true">+ Log</span>
                </button>
              )}
              <div className="account">
                <button
                  className="avatar"
                  onClick={() => setMenuOpen((o) => !o)}
                  aria-expanded={menuOpen}
                  aria-label={`Account menu for ${user.displayName}`}
                >
                  {user.displayName.charAt(0).toUpperCase()}
                </button>
                {menuOpen && (
                  <div className="account-menu" role="menu">
                    <p>
                      <strong>{user.displayName}</strong>
                      <span className="muted">{user.email}</span>
                    </p>
                    <button className="btn btn-ghost" role="menuitem" onClick={logOut}>Log out</button>
                  </div>
                )}
              </div>
            </>
          ) : (
            !checking && <button className="btn btn-primary" onClick={() => showAuth()}>Log in</button>
          )}
        </div>
      </header>

      <main className="content" onClick={() => menuOpen && setMenuOpen(false)}>
        {showMine ? (
          <MyVisits user={user} composing={composing} onComposeDone={composeDone} />
        ) : (
          <p className="empty">The public feed goes here.</p>
        )}
      </main>
    </div>
  )
}
