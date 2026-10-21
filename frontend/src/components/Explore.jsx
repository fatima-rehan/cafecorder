import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { photoFor } from '../drinkArt.js'
import { formatPlace } from '../format.js'
import { KINDS, kindLabel } from '../kinds.js'
import CafeMap from './CafeMap.jsx'
import GalleryGrid from './GalleryGrid.jsx'
import Rating from './Rating.jsx'
import VisitDetail from './VisitDetail.jsx'

const TABS = [
  { value: 'top', label: 'Top cafes' },
  { value: 'photos', label: 'Photos' },
  { value: 'map', label: 'Map' },
]

const SORTS = [
  { value: 'RECENT', label: 'Newest' },
  { value: 'TOP', label: 'Top rated' },
]

const FEED_ERROR = "Can't load the feed right now."
// matches FeedService.PAGE_SIZE; a full page means there may be more
const PAGE_SIZE = 30

// Public: what everyone has shared. Works signed in or not.
export default function Explore({ signedIn, onSignUp }) {
  const [tab, setTab] = useState('top')
  const [topSort, setTopSort] = useState('TOP')
  const [photoSort, setPhotoSort] = useState('RECENT')
  // each tab keeps its own sort; the map has none
  const [sort, setSort] = tab === 'top' ? [topSort, setTopSort] : [photoSort, setPhotoSort]
  const [kind, setKind] = useState('') // '' = all types
  const [city, setCity] = useState('') // '' = all cities
  const [cities, setCities] = useState([])
  const filters = `${kind}|${city}`
  // for the empty messages: "No matcha spots in Dubai yet"
  const spots = kind ? `${kindLabel(kind).toLowerCase()} spots` : 'cafes'
  const inCity = city ? ` in ${city}` : ''
  // what the photo list currently shows, so a late "Load more" for an old filter is ignored
  const photoQuery = `${photoSort}|${filters}`
  const photoQueryRef = useRef(photoQuery)
  photoQueryRef.current = photoQuery
  const [topCafes, setTopCafes] = useState(null)
  const [photos, setPhotos] = useState(null)
  const [photoPage, setPhotoPage] = useState(0)
  const [morePhotos, setMorePhotos] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [mapCafes, setMapCafes] = useState(null)
  const [viewing, setViewing] = useState(null)
  const [error, setError] = useState('')

  // the city filter only matters once there's more than one city to pick from
  useEffect(() => {
    api.feed.cities().then(setCities).catch(() => setCities([]))
  }, [])

  // each effect drops answers that arrive after the filters have changed again
  useEffect(() => {
    let current = true
    setTopCafes(null)
    api.feed.topCafes(topSort, kind, city).then((cafes) => current && setTopCafes(cafes)).catch(() => current && setError(FEED_ERROR))
    return () => { current = false }
  }, [topSort, kind, city])

  // a new sort or filter starts over from the first page
  useEffect(() => {
    let current = true
    setPhotos(null)
    api.feed.photos(photoSort, kind, city).then((page) => {
      if (!current) return
      setPhotos(page)
      setPhotoPage(0)
      setMorePhotos(page.length === PAGE_SIZE)
    }).catch(() => current && setError(FEED_ERROR))
    return () => { current = false }
  }, [photoSort, kind, city])

  async function loadMorePhotos() {
    const asked = photoQuery
    setLoadingMore(true)
    try {
      const page = await api.feed.photos(photoSort, kind, city, photoPage + 1)
      if (asked !== photoQueryRef.current) return
      // skip anything already shown, in case new posts shifted the pages
      setPhotos((shown) => [...shown, ...page.filter((p) => !shown.some((s) => s.id === p.id))])
      setPhotoPage((n) => n + 1)
      setMorePhotos(page.length === PAGE_SIZE)
    } catch {
      setError(FEED_ERROR)
    } finally {
      setLoadingMore(false)
    }
  }

  // only fetched once the map is opened, and again when a filter changes
  // a ref, not state: changing it mustn't re-run the effect (that would cancel the request it just made)
  const mapFilters = useRef(null) // the filters mapCafes was loaded for
  useEffect(() => {
    if (tab !== 'map' || mapFilters.current === filters) return
    let current = true
    let loaded = false
    mapFilters.current = filters
    setMapCafes(null)
    api.feed.map(kind, city).then((cafes) => {
      if (!current) return
      loaded = true
      setMapCafes(cafes)
    }).catch(() => {
      if (!current) return
      mapFilters.current = null // try again next time the map is opened
      setError(FEED_ERROR)
    })
    return () => {
      current = false
      // left before it loaded: forget it so it's fetched again
      if (!loaded && mapFilters.current === filters) mapFilters.current = null
    }
  }, [tab, kind, city, filters])

  return (
    <>
      <header className="page-head">
        <h2>Explore</h2>
        <p className="muted">What people are drinking, and where.</p>
      </header>

      {!signedIn && (
        <div className="cta">
          <p><strong>Keep your own cafe logbook.</strong> Track every visit, spend and study spot. Share only what you want.</p>
          <button className="btn btn-primary" onClick={onSignUp}>Sign up free</button>
        </div>
      )}

      <section className="controls">
        <div className="control-group">
          <div className="segmented" role="group" aria-label="Feed">
            {TABS.map((t) => (
              <button key={t.value} className={tab === t.value ? 'active' : ''} onClick={() => setTab(t.value)}>
                {t.label}
              </button>
            ))}
          </div>
          {tab !== 'map' && (
            <div className="segmented" role="group" aria-label="Sort">
              {SORTS.map((s) => (
                <button key={s.value} className={sort === s.value ? 'active' : ''} onClick={() => setSort(s.value)}>
                  {s.label}
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="control-group">
          <select value={kind} onChange={(e) => setKind(e.target.value)} aria-label="Type of place">
            <option value="">All types</option>
            {KINDS.map((k) => <option key={k.value} value={k.value}>{k.label}</option>)}
          </select>
          {cities.length > 1 && (
            <select value={city} onChange={(e) => setCity(e.target.value)} aria-label="City">
              <option value="">All cities</option>
              {cities.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          )}
        </div>
      </section>

      {error && <p className="banner">{error}</p>}

      {tab === 'top' && topCafes && (
        topCafes.length === 0 ? (
          <p className="empty">
            No rated {spots}{inCity} yet. Be the first to share one.
          </p>
        ) : (
          <ol className="leaderboard">
            {topCafes.map((c, i) => (
              <li key={`${c.name}|${c.neighbourhood}`} className="leader">
                <span className="leader-rank" aria-label={`Rank ${i + 1}`}>{i + 1}</span>
                <img className="leader-photo" src={c.coverPhoto || photoFor({ order: '' })} alt="" loading="lazy" />
                <div className="leader-text">
                  <h3>{c.name}</h3>
                  <p className="muted">{formatPlace(c)}</p>
                  <div className="leader-score">
                    <Rating value={Math.round(c.averageRating)} />
                    <span className="muted">
                      {c.averageRating.toFixed(1)} · {c.visits} {c.visits === 1 ? 'visit' : 'visits'}
                    </span>
                  </div>
                </div>
              </li>
            ))}
          </ol>
        )
      )}

      {tab === 'photos' && photos && (
        photos.length === 0
          ? <p className="empty">{kind || city ? `No ${spots} shared${inCity} yet.` : 'Nothing shared yet.'}</p>
          : (
            <>
              <GalleryGrid visits={photos} onOpen={setViewing} showAuthor />
              {morePhotos && (
                <div className="load-more">
                  <button className="btn btn-outline" onClick={loadMorePhotos} disabled={loadingMore}>
                    {loadingMore ? 'Loading…' : 'Load more'}
                  </button>
                </div>
              )}
            </>
          )
      )}

      {tab === 'map' && (
        <>
          {mapCafes?.length === 0 && (
            <p className="empty">
              No {spots} on the map{inCity} yet. Add an address when you log a visit to pin it here.
            </p>
          )}
          <CafeMap cafes={mapCafes ?? []} />
        </>
      )}

      {viewing && <VisitDetail visit={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}
