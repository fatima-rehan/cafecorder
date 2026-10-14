import { useState } from 'react'
import { photoFor } from '../drinkArt.js'
import { formatDate, money } from '../format.js'
import Rating from './Rating.jsx'
import StudyChips from './StudyChips.jsx'

export default function VisitCard({ visit, onOpen, onEdit, onDelete }) {
  // inline "Delete this visit?" instead of window.confirm(), which looks out of place and blocks the page
  const [confirming, setConfirming] = useState(false)

  return (
    <article className="card">
      <button className="card-photo" onClick={() => onOpen?.(visit)} aria-label={`Open ${visit.name}`}>
        <img src={photoFor(visit)} alt="" loading="lazy" />
      </button>

      <div className="card-body">
        <header className="card-head">
          <div>
            <h3>{visit.name}</h3>
            <p className="muted">
              {[visit.neighbourhood, visit.city].filter(Boolean).join(', ')} · {formatDate(visit.visitDate)}
              {visit.shared === false && <span className="private-tag">Private</span>}
            </p>
          </div>
          <span className="price">{money.format(visit.total ?? visit.price)}</span>
        </header>

        <p className="order">{[visit.order, ...(visit.extras ?? []).map((x) => x.name)].join(' + ')}</p>
        <Rating value={visit.rating} />
        {visit.studySpot && <StudyChips visit={visit} />}

        <footer className="card-actions">
          {confirming ? (
            <>
              <span className="muted confirm-text">Delete this visit?</span>
              <button className="btn btn-ghost" onClick={() => setConfirming(false)}>Keep</button>
              <button className="btn btn-ghost danger" onClick={() => onDelete(visit)}>Delete</button>
            </>
          ) : (
            <>
              {onEdit && <button className="btn btn-ghost" onClick={() => onEdit(visit)}>Edit</button>}
              <button className="btn btn-ghost danger" onClick={() => setConfirming(true)}>Delete</button>
            </>
          )}
        </footer>
      </div>
    </article>
  )
}
