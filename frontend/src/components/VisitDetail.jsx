import { useEffect, useState } from 'react'
import { photoFor } from '../drinkArt.js'
import { formatDate, money } from '../format.js'
import Rating from './Rating.jsx'
import StudyChips from './StudyChips.jsx'

// Your own visit (with Edit/Delete), or someone's shared visit from the feed (read-only)
export default function VisitDetail({ visit, onEdit, onDelete, onClose }) {
  const [confirming, setConfirming] = useState(false)
  const editable = Boolean(onEdit)

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <article className="dialog detail" role="dialog" aria-label={visit.name}>
        <div className="detail-photo">
          <img src={photoFor(visit)} alt={`${visit.order} at ${visit.name}`} />
          <button className="detail-close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        <div className="detail-body">
          <header className="card-head">
            <div>
              <h2>{visit.name}</h2>
              <p className="muted">{visit.neighbourhood} · {formatDate(visit.visitDate)}</p>
            </div>
            {visit.price != null && <span className="price">{money.format(visit.total ?? visit.price)}</span>}
          </header>

          {visit.extras?.length ? (
            <ul className="order-lines">
              {[{ name: visit.order, price: visit.price }, ...visit.extras].map((x, i) => (
                <li key={i}><span>{x.name}</span><span>{money.format(x.price)}</span></li>
              ))}
            </ul>
          ) : (
            <p className="order">{visit.order}</p>
          )}
          <Rating value={visit.rating} />
          {visit.author && <p className="muted byline">Shared by {visit.author}</p>}
          {editable && visit.shared === false && <p className="muted byline">Private: only you can see this visit</p>}
          {/* feed items only say whether it was a study session, not the details */}
          {visit.studySpot && visit.seating && <StudyChips visit={visit} />}
          {visit.studySpot && !visit.seating && <span className="study-tag">Study spot</span>}
        </div>

        {editable && (
          <footer className="dialog-actions">
            {confirming ? (
              <>
                <span className="muted confirm-text">Delete this visit?</span>
                <button className="btn btn-ghost" onClick={() => setConfirming(false)}>Keep</button>
                <button className="btn btn-primary btn-danger" onClick={() => onDelete(visit)}>Delete</button>
              </>
            ) : (
              <>
                <button className="btn btn-ghost danger" onClick={() => setConfirming(true)}>Delete</button>
                <button className="btn btn-primary" onClick={() => onEdit(visit)}>Edit visit</button>
              </>
            )}
          </footer>
        )}
      </article>
    </div>
  )
}
