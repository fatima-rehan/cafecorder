import { useState } from 'react'
import TableScene from './TableScene.jsx'

export default function Landing({ onEnter }) {
  const [entering, setEntering] = useState(false)

  function enter() {
    setEntering(true)
    // let the dive into the coffee play (see .landing.entering in styles.css), then switch screens
    setTimeout(onEnter, 1050)
  }

  return (
    <main className={`landing ${entering ? 'entering' : ''}`}>
      <div className="landing-copy">
        <p className="eyebrow">A little logbook for your cafe visits</p>
        <h1>CafeCorder</h1>
      </div>

      <div className="scene">
        <TableScene />
      </div>

      <button className="btn btn-primary btn-lg enter-btn" onClick={enter} disabled={entering}>
        Enter cafe
      </button>
    </main>
  )
}
