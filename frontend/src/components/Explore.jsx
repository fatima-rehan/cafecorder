import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { photoFor } from '../drinkArt.js'
import CafeMap from './CafeMap.jsx'
import GalleryGrid from './GalleryGrid.jsx'
import Rating from './Rating.jsx'
import VisitDetail from './VisitDetail.jsx'

const TABS = [
  { value: 'top', label: 'Top cafes' },
  { value: 'photos', label: 'Photos' },
  { value: 'map', label: 'Map' },
]

const PHOTO_SORTS = [
  { value: 'RECENT', label: 'Newest' },
  { value: 'TOP', label: 'Top rated' },
]

const FEED_ERROR = "Can't load the feed right now."
// matches FeedService.PAGE_SIZE; a full page means there may be more
const PAGE_SIZE = 30

// Public: what everyone has shared. Works signed in or not.
export default function Explore({ signedIn, onSignUp }) {
  const [tab, setTab] = useState('top')
  const [photoSort, setPhotoSort] = useState('RECENT')
  const photoSortRef = useRef(photoSort)
  photoSortRef.current = photoSort
  const [topCafes, setTopCafes] = useState(null)
  const [photos, setPhotos] = useState(null)
  const [photoPage, setPhotoPage] = useState(0)
  const [morePhotos, setMorePhotos] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [mapCafes, setMapCafes] = useState(null)
  const [viewing, setViewing] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.feed.topCafes().then(setTopCafes).catch(() => setError(FEED_ERROR))
  }, [])

  // a new sort starts over from the first page; stale answers from the old sort are dropped
  useEffect(() => {
    let current = true
    setPhotos(null)
    api.feed.photos(photoSort).then((page) => {
      if (!current) return
      setPhotos(page)
      setPhotoPage(0)
      setMorePhotos(page.length === PAGE_SIZE)
    }).catch(() => current && setError(FEED_ERROR))
    return () => { current = false }
  }, [photoSort])

  async function loadMorePhotos() {
    const sort = photoSort
    setLoadingMore(true)
    try {
      const page = await api.feed.photos(sort, photoPage + 1)
      if (sort !== photoSortRef.current) return
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

  // only fetched once the map is opened
  useEffect(() => {
    if (tab === 'map' && !mapCafes) api.feed.map().then(setMapCafes).catch(() => setError(FEED_ERROR))
  }, [tab, mapCafes])

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
        <div className="segmented" role="group" aria-label="Feed">
          {TABS.map((t) => (
            <button key={t.value} className={tab === t.value ? 'active' : ''} onClick={() => setTab(t.value)}>
              {t.label}
            </button>
          ))}
        </div>
        {tab === 'photos' && (
          <div className="segmented" role="group" aria-label="Sort photos">
            {PHOTO_SORTS.map((s) => (
              <button key={s.value} className={photoSort === s.value ? 'active' : ''} onClick={() => setPhotoSort(s.value)}>
                {s.label}
              </button>
            ))}
          </div>
        )}
      </section>

      {error && <p className="banner">{error}</p>}

      {tab === 'top' && topCafes && (
        topCafes.length === 0 ? (
          <p className="empty">No rated cafes yet. Be the first to share one.</p>
        ) : (
          <ol className="leaderboard">
            {topCafes.map((c, i) => (
              <li key={`${c.name}|${c.neighbourhood}`} className="leader">
                <span className="leader-rank" aria-label={`Rank ${i + 1}`}>{i + 1}</span>
                <img className="leader-photo" src={c.coverPhoto || photoFor({ order: '' })} alt="" loading="lazy" />
                <div className="leader-text">
                  <h3>{c.name}</h3>
                  <p className="muted">{c.neighbourhood}</p>
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
          ? <p className="empty">Nothing shared yet.</p>
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
            <p className="empty">No cafes on the map yet. Add an address when you log a visit to pin it here.</p>
          )}
          <CafeMap cafes={mapCafes ?? []} />
        </>
      )}

      {viewing && <VisitDetail visit={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}
