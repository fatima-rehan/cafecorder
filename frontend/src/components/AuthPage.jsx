import { useState } from 'react'
import { api, setToken } from '../api.js'
import { drinkArt } from '../drinkArt.js'

const CUP = drinkArt('flatwhite')

export default function AuthPage({ initialMode = 'login', onAuthed, onBrowse, onBack }) {
  const [mode, setMode] = useState(initialMode) // 'login' | 'signup'
  const [form, setForm] = useState({ email: '', password: '', displayName: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const signup = mode === 'signup'
  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  function switchMode(next) {
    setMode(next)
    setErrors({})
    setMessage('')
  }

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setErrors({})
    setMessage('')
    try {
      const { token, user } = signup
        ? await api.auth.register(form)
        : await api.auth.login({ email: form.email, password: form.password })
      setToken(token)
      onAuthed(user)
    } catch (err) {
      setErrors(err.fields)
      // field problems show under each input; anything else (wrong password, email taken) shows here
      if (Object.keys(err.fields).length === 0) setMessage(err.message)
      setBusy(false)
    }
  }

  const fieldError = (name) => errors[name] && <span className="field-error">{errors[name]}</span>

  return (
    <main className="auth">
      <button className="brand auth-brand" onClick={onBack} aria-label="Back to the landing page">
        <span className="brand-mark" />
        CafeCorder
      </button>

      <section className="auth-panel">
        <img className="auth-cup" src={CUP} alt="" />
        <h1>{signup ? 'Start your cafe logbook' : 'Welcome back'}</h1>
        <p className="muted auth-sub">
          {signup ? 'Log every cup. Keep your notes private or share them with the city.' : 'Log in to see your visits.'}
        </p>

        <div className="segmented auth-switch" role="group" aria-label="Log in or sign up">
          <button className={!signup ? 'active' : ''} onClick={() => switchMode('login')}>Log in</button>
          <button className={signup ? 'active' : ''} onClick={() => switchMode('signup')}>Sign up</button>
        </div>

        <form className="auth-form" onSubmit={submit} noValidate>
          {signup && (
            <label>
              Name
              <input value={form.displayName} onChange={set('displayName')} autoComplete="name" placeholder="Fatima" />
              {fieldError('displayName')}
            </label>
          )}
          <label>
            Email
            <input type="email" value={form.email} onChange={set('email')} autoComplete="email" placeholder="you@example.com" />
            {fieldError('email')}
          </label>
          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={set('password')}
              autoComplete={signup ? 'new-password' : 'current-password'}
            />
            {signup && !errors.password && <span className="hint">At least 8 characters</span>}
            {fieldError('password')}
          </label>

          {message && <p className="form-error" role="alert">{message}</p>}

          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'One sec…' : signup ? 'Create account' : 'Log in'}
          </button>
        </form>

        <div className="auth-divider"><span>or</span></div>
        <button className="btn btn-outline btn-block" onClick={onBrowse}>Browse the public feed</button>
      </section>
    </main>
  )
}
