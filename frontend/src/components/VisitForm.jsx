import { useEffect, useState } from 'react'

// Phone photos are 3-12 MB. Draw onto a canvas at max 1200px and re-encode as JPEG,
// which brings it to a few hundred KB, well under the backend's ~3 MB cap.
async function resizePhoto(file, maxSide = 1200) {
  const bitmap = await createImageBitmap(file)
  const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(bitmap.width * scale)
  canvas.height = Math.round(bitmap.height * scale)
  canvas.getContext('2d').drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close()
  return canvas.toDataURL('image/jpeg', 0.82)
}

const today = () => new Date().toLocaleDateString('en-CA') // YYYY-MM-DD in local time

const EMPTY = {
  name: '',
  neighbourhood: '',
  visitDate: today(),
  order: '',
  price: '',
  rating: 0,
  studySession: false,
  hasWifi: true,
  hasOutlets: false,
  seating: 'GOOD',
  laptopPolicy: '',
  noiseLevel: 'MODERATE',
  photoUrl: '',
  shared: true,
}

// API response -> form fields (studySpot comes back, studySession goes out)
function fromVisit(v) {
  return {
    name: v.name,
    neighbourhood: v.neighbourhood,
    visitDate: v.visitDate,
    order: v.order,
    price: String(v.price),
    rating: v.rating,
    studySession: v.studySpot,
    hasWifi: v.hasWifi ?? EMPTY.hasWifi,
    hasOutlets: v.hasOutlets ?? EMPTY.hasOutlets,
    seating: v.seating ?? EMPTY.seating,
    laptopPolicy: v.laptopPolicy ?? '',
    noiseLevel: v.noiseLevel ?? EMPTY.noiseLevel,
    photoUrl: v.photoUrl ?? '',
    shared: v.shared !== false,
  }
}

function toPayload(form) {
  const payload = {
    ...form,
    price: form.price === '' ? null : Number(form.price),
    photoUrl: form.photoUrl || null,
    laptopPolicy: form.laptopPolicy.trim() || null,
  }
  if (!form.studySession) {
    Object.assign(payload, { hasWifi: null, hasOutlets: null, seating: null, laptopPolicy: null, noiseLevel: null })
  }
  return payload
}

// visit = null for a new visit, or the visit being edited
export default function VisitForm({ visit, neighbourhoods, onSave, onClose }) {
  const [form, setForm] = useState(visit ? fromVisit(visit) : EMPTY)
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
    setForm((f) => ({ ...f, [field]: value }))
  }

  async function pickPhoto(e) {
    const file = e.target.files?.[0]
    e.target.value = '' // lets you pick the same file again after removing it
    if (!file) return
    try {
      const photoUrl = await resizePhoto(file)
      setForm((f) => ({ ...f, photoUrl }))
      setErrors(({ photoUrl: _, ...rest }) => rest)
    } catch {
      setErrors((x) => ({ ...x, photoUrl: "Couldn't read that image. Try a JPG or PNG." }))
    }
  }

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    setMessage('')
    try {
      await onSave(toPayload(form))
    } catch (err) {
      setErrors(err.fields ?? {})
      setMessage(err.message)
      setSaving(false)
    }
  }

  const fieldError = (name) => errors[name] && <span className="field-error">{errors[name]}</span>

  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <form className="dialog" onSubmit={submit} noValidate aria-label={visit ? 'Edit visit' : 'Log a visit'}>
        <header className="dialog-head">
          <h2>{visit ? 'Edit visit' : 'Log a visit'}</h2>
          <button type="button" className="btn btn-ghost" onClick={onClose} aria-label="Close">✕</button>
        </header>

        <div className="photo-field">
          {form.photoUrl ? (
            <img src={form.photoUrl} alt="Your photo" />
          ) : (
            <div className="photo-empty">Add a photo of your drink or the cafe</div>
          )}
          <div className="photo-actions">
            <label className="btn btn-ghost photo-pick">
              {form.photoUrl ? 'Change photo' : 'Choose photo'}
              <input type="file" accept="image/*" onChange={pickPhoto} className="visually-hidden" />
            </label>
            {form.photoUrl && (
              <button type="button" className="btn btn-ghost" onClick={() => setForm((f) => ({ ...f, photoUrl: '' }))}>
                Remove
              </button>
            )}
          </div>
          {fieldError('photoUrl')}
        </div>

        <div className="grid-2">
          <label>
            Cafe
            <input value={form.name} onChange={set('name')} placeholder="Pilot Coffee" />
            {fieldError('name')}
          </label>
          <label>
            Neighbourhood
            <input value={form.neighbourhood} onChange={set('neighbourhood')} list="hoods" placeholder="Leslieville" />
            <datalist id="hoods">
              {neighbourhoods.map((n) => <option key={n} value={n} />)}
            </datalist>
            {fieldError('neighbourhood')}
          </label>
          <label>
            Date
            <input type="date" value={form.visitDate} max={today()} onChange={set('visitDate')} />
            {fieldError('visitDate')}
          </label>
          <label>
            Price
            <input type="number" inputMode="decimal" min="0" step="0.01" value={form.price} onChange={set('price')} placeholder="5.75" />
            {fieldError('price')}
          </label>
        </div>

        <label>
          What you ordered
          <input value={form.order} onChange={set('order')} placeholder="Oat flat white" />
          {fieldError('order')}
        </label>

        <div className="rating-input">
          <span>Rating</span>
          <div>
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                type="button"
                key={n}
                className={n <= form.rating ? 'bean on' : 'bean'}
                aria-label={`${n} out of 5`}
                aria-pressed={n <= form.rating}
                // tapping the current rating again clears it back to unrated
                onClick={() => setForm((f) => ({ ...f, rating: f.rating === n ? 0 : n }))}
              />
            ))}
            <span className="muted">{form.rating ? `${form.rating}/5` : 'Unrated'}</span>
          </div>
          {fieldError('rating')}
        </div>

        <label className="toggle">
          <input type="checkbox" checked={form.studySession} onChange={set('studySession')} />
          This was a study session
        </label>

        {form.studySession && (
          <fieldset className="study-fields">
            <div className="grid-2">
              <label className="check">
                <input type="checkbox" checked={form.hasWifi} onChange={set('hasWifi')} /> Wifi
              </label>
              <label className="check">
                <input type="checkbox" checked={form.hasOutlets} onChange={set('hasOutlets')} /> Outlets
              </label>
              <label>
                Seating
                <select value={form.seating} onChange={set('seating')}>
                  <option value="GOOD">Good</option>
                  <option value="LIMITED">Limited</option>
                  <option value="NONE">None</option>
                </select>
              </label>
              <label>
                Noise
                <select value={form.noiseLevel} onChange={set('noiseLevel')}>
                  <option value="QUIET">Quiet</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="LOUD">Loud</option>
                </select>
              </label>
            </div>
            <label>
              Laptop policy
              <input value={form.laptopPolicy} onChange={set('laptopPolicy')} placeholder="No laptops after 5pm" />
              {fieldError('laptopPolicy')}
            </label>
            {fieldError('studyDetailsComplete')}
          </fieldset>
        )}

        <label className="toggle share-toggle">
          <input type="checkbox" checked={form.shared} onChange={set('shared')} />
          <span>
            Share on the public feed
            <span className="hint">Others see the photo, cafe, order and rating. Never the price.</span>
          </span>
        </label>

        {message && <p className="form-error" role="alert">{message}</p>}

        <footer className="dialog-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save visit'}
          </button>
        </footer>
      </form>
    </div>
  )
}
