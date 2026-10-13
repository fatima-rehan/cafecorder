import { useState } from 'react'
import { setToken } from './api.js'
import AuthPage from './components/AuthPage.jsx'

// Temporary: just the auth page and a signed-in message. The app shell comes next.
export default function App() {
  const [user, setUser] = useState(null)

  if (!user) return <AuthPage onAuthed={setUser} onBrowse={() => {}} onBack={() => {}} />

  return (
    <main className="content">
      <h1>Hi {user.displayName}</h1>
      <p className="muted">Signed in as {user.email}</p>
      <button className="btn btn-ghost" onClick={() => { setToken(null); setUser(null) }}>Log out</button>
    </main>
  )
}
