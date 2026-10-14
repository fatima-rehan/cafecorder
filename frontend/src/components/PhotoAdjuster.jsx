import { useEffect, useRef, useState } from 'react'

const FRAME = 4 / 3 // same shape as the photo in the visit detail view
const OUT_W = 1200
const OUT_H = OUT_W / FRAME

// Image size and position as percentages of the frame, so the preview and the
// final crop use the same numbers no matter how big the frame is on screen.
function layout(img, zoom, offset) {
  const ratio = img.naturalWidth / img.naturalHeight
  // "cover" at zoom 1: fill the frame on the tighter side
  const w = (ratio > FRAME ? (ratio / FRAME) * 100 : 100) * zoom
  const h = (ratio > FRAME ? 100 : (FRAME / ratio) * 100) * zoom
  // keep the image covering the frame: no empty edges
  const maxX = (w - 100) / 2
  const maxY = (h - 100) / 2
  const x = Math.max(-maxX, Math.min(maxX, offset.x))
  const y = Math.max(-maxY, Math.min(maxY, offset.y))
  return { w, h, x, y, left: 50 + x - w / 2, top: 50 + y - h / 2 }
}

// Draw only the framed part at 1200x900 and re-encode as JPEG (a few hundred KB, under the ~3 MB cap)
function crop(img, l) {
  const canvas = document.createElement('canvas')
  canvas.width = OUT_W
  canvas.height = OUT_H
  const sx = (-l.left / l.w) * img.naturalWidth
  const sy = (-l.top / l.h) * img.naturalHeight
  const sw = (100 / l.w) * img.naturalWidth
  const sh = (100 / l.h) * img.naturalHeight
  canvas.getContext('2d').drawImage(img, sx, sy, sw, sh, 0, 0, OUT_W, OUT_H)
  return canvas.toDataURL('image/jpeg', 0.82)
}

// Drag to move the photo, slider to zoom. src must be same-origin (a picked file or data URL)
// or the canvas can't be read back.
export default function PhotoAdjuster({ src, onDone, onCancel, onError }) {
  const frameRef = useRef(null)
  const imgRef = useRef(null)
  const drag = useRef(null)
  const [loaded, setLoaded] = useState(false)
  const [zoom, setZoom] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })

  useEffect(() => {
    setLoaded(false)
    setZoom(1)
    setOffset({ x: 0, y: 0 })
  }, [src])

  const l = loaded ? layout(imgRef.current, zoom, offset) : null

  function onPointerDown(e) {
    if (!l) return
    e.currentTarget.setPointerCapture(e.pointerId)
    drag.current = { px: e.clientX, py: e.clientY, start: { x: l.x, y: l.y } }
  }

  function onPointerMove(e) {
    if (!drag.current) return
    const box = frameRef.current.getBoundingClientRect()
    const { px, py, start } = drag.current
    // pixels moved -> percent of the frame; layout() clamps it so the frame stays covered
    const next = { x: start.x + ((e.clientX - px) / box.width) * 100, y: start.y + ((e.clientY - py) / box.height) * 100 }
    const clamped = layout(imgRef.current, zoom, next)
    setOffset({ x: clamped.x, y: clamped.y })
  }

  const endDrag = () => { drag.current = null }

  return (
    <div className="adjuster">
      <div
        ref={frameRef}
        className="adjust-frame"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
      >
        <img
          ref={imgRef}
          src={src}
          alt="Drag to position your photo"
          draggable={false}
          onLoad={() => setLoaded(true)}
          onError={onError}
          style={l ? { width: `${l.w}%`, height: `${l.h}%`, left: `${l.left}%`, top: `${l.top}%` } : { opacity: 0 }}
        />
        <span className="adjust-hint">Drag to move</span>
      </div>

      <label className="adjust-zoom">
        Zoom
        <input type="range" min="1" max="3" step="0.01" value={zoom} onChange={(e) => setZoom(Number(e.target.value))} />
      </label>

      <div className="photo-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
        <button type="button" className="btn btn-primary" disabled={!l} onClick={() => onDone(crop(imgRef.current, l))}>
          Use photo
        </button>
      </div>
    </div>
  )
}
