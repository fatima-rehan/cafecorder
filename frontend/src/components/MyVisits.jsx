import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'
import { KINDS, kindLabel } from '../kinds.js'
import CafeMap from './CafeMap.jsx'
import GalleryGrid from './GalleryGrid.jsx'
import StatsBar from './StatsBar.jsx'
import VisitCard from './VisitCard.jsx'
import VisitDetail from './VisitDetail.jsx'
import VisitForm from './VisitForm.jsx'

const SORTS = [
  { value: 'RECENT', label: 'Recent' },
  { value: 'RATING', label: 'Top rated' },
]

const VIEWS = [
  { value: 'list', label: 'List' },
  { value: 'gallery', label: 'Gallery' },
  { value: 'map', label: 'Map' },
]

const VIEW_KEY = 'cafecorder-view'

// One pin per cafe, grouped the same way as the public map (name + address), newest visit first
function pinsFor(visits) {
  const cafes = new Map()
  const newestFirst = [...visits].sort((a, b) => b.visitDate.localeCompare(a.visitDate))
  for (const v of newestFirst) {
    if (v.latitude == null || v.longitude == null) continue
    const key = `${v.name.toLowerCase()}|${(v.address ?? '').toLowerCase()}`
    const cafe = cafes.get(key) ?? {
      name: v.name, address: v.address, city: v.city, latitude: v.latitude, longitude: v.longitude,
      coverPhoto: null, visitList: [],
    }
    cafe.visitList.push(v)
    cafe.coverPhoto ??= v.photoUrl ?? null
    cafes.set(key, cafe)
  }
  return [...cafes.values()].map((c) => {
    const rated = c.visitList.filter((v) => v.rating != null)
    const averageRating = rated.length ? rated.reduce((sum, v) => sum + v.rating, 0) / rated.length : null
    return { ...c, visits: c.visitList.length, averageRating }
  })
}

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
  const [kind, setKind] = useState('')
  const [sort, setSort] = useState('RECENT')
  const [view, setView] = useState(savedView)
  const [editing, setEditing] = useState(null) // null = closed, {} = new visit, a visit = editing it
  const [viewing, setViewing] = useState(null) // visit open in the detail view
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const pins = useMemo(() => pinsFor(visits), [visits])

  const load = useCallback(async () => {
    try {
      const [list, s, hoods] = await Promise.all([
        api.visits.list(neighbourhood, kind, sort),
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
  }, [neighbourhood, kind, sort])

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

  function edit(visit) {
    setViewing(null)
    setEditing(visit)
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
      setViewing(null)
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
        <div className="control-group">
          <select value={neighbourhood} onChange={(e) => setNeighbourhood(e.target.value)} aria-label="Area">
            <option value="">All areas</option>
            {neighbourhoods.map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
          <select value={kind} onChange={(e) => setKind(e.target.value)} aria-label="Type of place">
            <option value="">All types</option>
            {KINDS.map((k) => <option key={k.value} value={k.value}>{k.label}</option>)}
          </select>
        </div>
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
          {neighbourhood || kind ? (
            <>
              <p>No {kind ? `${kindLabel(kind).toLowerCase()} ` : ''}visits{neighbourhood && ` in ${neighbourhood}`} yet.</p>
              <button className="btn btn-outline" onClick={() => { setNeighbourhood(''); setKind('') }}>Clear filters</button>
            </>
          ) : (
            <>
              <p>No visits yet.</p>
              <button className="btn btn-primary" onClick={() => setEditing({})}>Log your first cup</button>
            </>
          )}
        </div>
      )}

      {view === 'map' ? (
        visits.length > 0 && (
          <>
            {pins.length === 0 && (
              <p className="empty">None of these visits have an address yet. Edit one and pick an address to pin it.</p>
            )}
            {pins.length > 0 && pins.reduce((n, c) => n + c.visits, 0) < visits.length && (
              <p className="muted map-note">Visits without an address aren't on the map.</p>
            )}
            <CafeMap cafes={pins} label="Map of your cafes" locateOnOpen={false} onOpenVisit={setViewing} />
          </>
        )
      ) : view === 'gallery' ? (
        <GalleryGrid visits={visits} onOpen={setViewing} />
      ) : (
        <section className="cards">
          {visits.map((v) => (
            <VisitCard key={v.id} visit={v} onOpen={setViewing} onEdit={edit} onDelete={remove} />
          ))}
        </section>
      )}

      {viewing && <VisitDetail visit={viewing} onEdit={edit} onDelete={remove} onClose={() => setViewing(null)} />}

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
