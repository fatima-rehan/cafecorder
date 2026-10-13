import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import GalleryGrid from './GalleryGrid.jsx'
import StatsBar from './StatsBar.jsx'
import VisitCard from './VisitCard.jsx'
import VisitForm from './VisitForm.jsx'

const SORTS = [
  { value: 'RECENT', label: 'Recent' },
  { value: 'RATING', label: 'Top rated' },
  { value: 'PRICE', label: 'Priciest' },
]

const VIEWS = [
  { value: 'list', label: 'List' },
  { value: 'gallery', label: 'Gallery' },
]

const VIEW_KEY = 'cafecorder-view'

// remember list vs gallery between visits (storage can be unavailable, so fail quietly)
function savedView() {
  try {
    return localStorage.getItem(VIEW_KEY) ?? 'list'
  } catch {
    return 'list'
  }
}

// The signed-in user's own logbook
export default function MyVisits({ user, composing, onComposeDone }) {
  const [visits, setVisits] = useState([])
  const [stats, setStats] = useState(null)
  const [neighbourhoods, setNeighbourhoods] = useState([])
  const [neighbourhood, setNeighbourhood] = useState('')
  const [sort, setSort] = useState('RECENT')
  const [view, setView] = useState(savedView)
  const [editing, setEditing] = useState(null) // null = closed, {} = new visit, a visit = editing it
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const [list, s, hoods] = await Promise.all([
        api.visits.list(neighbourhood, sort),
        api.visits.stats(),
        api.visits.neighbourhoods(),
      ])
      setVisits(list)
      setStats(s)
      setNeighbourhoods(hoods)
      setError('')
    } catch (err) {
      // a 401 is handled in App (log out); anything else gets a banner
      if (err.status !== 401) setError("Can't load your visits right now. Is the Spring Boot server running?")
    } finally {
      setLoading(false)
    }
  }, [neighbourhood, sort])

  useEffect(() => {
    load()
  }, [load])

  // the "Log a visit" button lives in the top bar (App), so it signals through props
  useEffect(() => {
    if (composing) {
      setEditing({})
      onComposeDone()
    }
  }, [composing, onComposeDone])

  function changeView(next) {
    setView(next)
    try {
      localStorage.setItem(VIEW_KEY, next)
    } catch {
      // not saved, still works for now
    }
  }

  // errors are thrown back to VisitForm so it can show them under the fields
  async function save(payload) {
    if (editing?.id) await api.visits.update(editing.id, payload)
    else await api.visits.create(payload)
    setEditing(null)
    load()
  }

  async function remove(visit) {
    try {
      await api.visits.remove(visit.id)
      load()
    } catch (err) {
      if (err.status !== 401) setError(err.message)
    }
  }

  return (
    <>
      <header className="page-head">
        <h2>{user.displayName}'s logbook</h2>
      </header>

      <StatsBar stats={stats} />

      <section className="controls">
        <select value={neighbourhood} onChange={(e) => setNeighbourhood(e.target.value)} aria-label="Neighbourhood">
          <option value="">All neighbourhoods</option>
          {neighbourhoods.map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
        <div className="control-group">
          <div className="segmented" role="group" aria-label="Sort">
            {SORTS.map((s) => (
              <button key={s.value} className={sort === s.value ? 'active' : ''} onClick={() => setSort(s.value)}>
                {s.label}
              </button>
            ))}
          </div>
          <div className="segmented" role="group" aria-label="View">
            {VIEWS.map((v) => (
              <button key={v.value} className={view === v.value ? 'active' : ''} onClick={() => changeView(v.value)}>
                {v.label}
              </button>
            ))}
          </div>
        </div>
      </section>

      {error && <p className="banner">{error}</p>}

      {!loading && !error && visits.length === 0 && (
        <div className="empty">
          <p>No visits yet{neighbourhood && ` in ${neighbourhood}`}.</p>
          <button className="btn btn-primary" onClick={() => setEditing({})}>Log your first cup</button>
        </div>
      )}

      {view === 'gallery' ? (
        <GalleryGrid visits={visits} />
      ) : (
        <section className="cards">
          {visits.map((v) => (
            <VisitCard key={v.id} visit={v} onEdit={setEditing} onDelete={remove} />
          ))}
        </section>
      )}

      {editing && (
        <VisitForm
          visit={editing.id ? editing : null}
          neighbourhoods={neighbourhoods}
          onSave={save}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  )
}
