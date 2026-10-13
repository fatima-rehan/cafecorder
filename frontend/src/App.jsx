import { useEffect, useState } from 'react'
import { api } from './api.js'

// Temporary: proves the frontend can reach the API (api.js + CORS). The real screens replace this.
export default function App() {
  const [status, setStatus] = useState('Checking the API…')

  useEffect(() => {
    api.feed.topCafes()
      .then((cafes) => setStatus(`Connected. The public feed has ${cafes.length} top cafes so far.`))
      .catch((err) => setStatus(`No luck: ${err.message}`))
  }, [])

  return (
    <main className="content">
      <h1>CafeCorder</h1>
      <p className="muted">{status}</p>
    </main>
  )
}
