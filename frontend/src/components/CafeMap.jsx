import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { photoFor } from '../drinkArt.js'
import { formatDate } from '../format.js'

// Pins are inline SVG (divIcon), so there are no marker image files for the bundler to lose
// Same hex values as the theme variables in styles.css, since an SVG string can't read CSS vars
const ESPRESSO = '#2b1a12'
const CREMA = '#c8905a'
const CREAM = '#fffcf7'

const pin = (fill, inner) => `
  <svg viewBox="0 0 36 46" width="36" height="46" aria-hidden="true">
    <path d="M18 45C18 45 3 28.5 3 17a15 15 0 0 1 30 0c0 11.5-15 28-15 28z" fill="${fill}" stroke="${CREAM}" stroke-width="2"/>
    ${inner}
  </svg>`

const CUP = `<path d="M11 12h12v5a6 6 0 0 1-12 0zM23 13.5h1.5a2.5 2.5 0 0 1 0 5H23" fill="none" stroke="${CREAM}" stroke-width="2" stroke-linejoin="round"/>`
const DOT = `<circle cx="18" cy="17" r="6" fill="${CREAM}"/>`

const cafeIcon = L.divIcon({ className: 'map-pin', html: pin(ESPRESSO, CUP), iconSize: [36, 46], iconAnchor: [18, 45], popupAnchor: [0, -40] })
const youIcon = L.divIcon({ className: 'map-pin', html: pin(CREMA, DOT), iconSize: [36, 46], iconAnchor: [18, 45], popupAnchor: [0, -40] })

// Popup built with DOM nodes and textContent, so a cafe name like "<img onerror=...>" stays plain text
function el(tag, className, text) {
  const node = document.createElement(tag)
  if (className) node.className = className
  if (text != null) node.textContent = text
  return node
}

function popupFor(cafe, onOpenVisit) {
  const box = el('div', 'map-popup')
  const img = el('img')
  img.src = cafe.coverPhoto || photoFor({ order: cafe.name })
  img.alt = ''
  box.append(img, el('strong', null, cafe.name))
  const where = [cafe.address, cafe.city].filter(Boolean).join(', ')
  if (where) box.append(el('span', 'muted', where))
  const score = cafe.averageRating != null ? `${cafe.averageRating.toFixed(1)}/5 · ` : ''
  box.append(el('span', 'muted', `${score}${cafe.visits} ${cafe.visits === 1 ? 'visit' : 'visits'}`))
  if (onOpenVisit && cafe.visitList) {
    const list = el('div', 'map-popup-visits')
    for (const visit of cafe.visitList) {
      const open = el('button', null, `${formatDate(visit.visitDate)} · ${visit.order}`)
      open.type = 'button'
      open.addEventListener('click', () => onOpenVisit(visit))
      list.append(open)
    }
    box.append(list)
  }
  const link = el('a', null, 'Open in OpenStreetMap')
  link.href = `https://www.openstreetmap.org/?mlat=${cafe.latitude}&mlon=${cafe.longitude}#map=18/${cafe.latitude}/${cafe.longitude}`
  link.target = '_blank'
  link.rel = 'noopener'
  box.append(link)
  return box
}

// Cafes as pins, plus your own location if you allow it.
// locateOnOpen=false keeps the view on the cafes until "Near me" is pressed.
export default function CafeMap({ cafes, label = 'Map of shared cafes', locateOnOpen = true, onOpenVisit }) {
  const boxRef = useRef(null)
  const mapRef = useRef(null)
  const cafeLayer = useRef(null)
  const youMarker = useRef(null)
  const [locating, setLocating] = useState(false)
  const [locationNote, setLocationNote] = useState('')

  useEffect(() => {
    const map = L.map(boxRef.current, { worldCopyJump: true }).setView([20, 0], 2)
    // OpenStreetMap's tile policy requires this attribution to stay visible
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map)
    cafeLayer.current = L.layerGroup().addTo(map)
    mapRef.current = map
    if (locateOnOpen) locate()
    return () => {
      map.remove()
      mapRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    const layer = cafeLayer.current
    if (!layer) return
    layer.clearLayers()
    for (const cafe of cafes) {
      L.marker([cafe.latitude, cafe.longitude], { icon: cafeIcon, title: cafe.name })
        .bindPopup(() => popupFor(cafe, onOpenVisit), { minWidth: 200 })
        .addTo(layer)
    }
    // without your location, frame all the pins
    if (!youMarker.current && cafes.length) {
      mapRef.current.fitBounds(cafes.map((c) => [c.latitude, c.longitude]), { padding: [40, 40], maxZoom: 14 })
    }
  }, [cafes, onOpenVisit])

  function locate() {
    if (!navigator.geolocation) {
      setLocationNote("Your browser can't share your location.")
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setLocating(false)
        setLocationNote('')
        const map = mapRef.current
        if (!map) return
        const here = [coords.latitude, coords.longitude]
        if (youMarker.current) youMarker.current.setLatLng(here)
        else youMarker.current = L.marker(here, { icon: youIcon, title: 'You', zIndexOffset: 1000 }).bindPopup('You are here').addTo(map)
        map.setView(here, 14)
      },
      () => {
        setLocating(false)
        setLocationNote('Location is off. Allow it in your browser to see cafes near you.')
      },
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 300_000 },
    )
  }

  return (
    <div className="map-wrap">
      <div ref={boxRef} className="cafe-map" role="region" aria-label={label} />
      <button className="btn btn-primary map-locate" onClick={locate} disabled={locating}>
        {locating ? 'Finding you…' : 'Near me'}
      </button>
      {locationNote && <p className="muted map-note">{locationNote}</p>}
    </div>
  )
}
