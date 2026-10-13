import { photoFor } from '../drinkArt.js'

export default function GalleryGrid({ visits, onOpen, showAuthor = false }) {
  return (
    <section className="gallery">
      {visits.map((v) => (
        <button key={v.id} className="tile" onClick={() => onOpen?.(v)}>
          <img src={photoFor(v)} alt={`${v.order} at ${v.name}`} loading="lazy" />
          <span className="tile-tags">
            {v.studySpot && <span className="tile-tag">Study</span>}
            {v.shared === false && <span className="tile-tag tile-tag-private">Private</span>}
          </span>
          <span className="tile-caption">
            <strong>{v.name}</strong>
            <span>{showAuthor ? `${v.neighbourhood} · ${v.author}` : v.neighbourhood}</span>
          </span>
        </button>
      ))}
    </section>
  )
}
