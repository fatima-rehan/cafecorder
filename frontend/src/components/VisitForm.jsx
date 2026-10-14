import { useEffect, useState } from 'react'
import { money } from '../format.js'
import PhotoAdjuster from './PhotoAdjuster.jsx'

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
  extras: [], // [{ name, price }] for anything besides the main order
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
    extras: (v.extras ?? []).map((x) => ({ name: x.name, price: String(x.price) })),
  }
}

function toPayload(form) {
  const payload = {
    ...form,
    price: form.price === '' ? null : Number(form.price),
    photoUrl: form.photoUrl || null,
    laptopPolicy: form.laptopPolicy.trim() || null,
    // rows left completely blank are dropped; half-filled ones go through so the backend can flag them
    extras: form.extras
      .filter((x) => x.name.trim() || x.price !== '')
      .map((x) => ({ name: x.name, price: x.price === '' ? null : Number(x.price) })),
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
  const [adjusting, setAdjusting] = useState(null) // photo being positioned, before it's saved to the form

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
    setForm((f) => ({ ...f, [field]: value }))
  }

  const setExtra = (i, field) => (e) =>
    setForm((f) => ({ ...f, extras: f.extras.map((x, j) => (j === i ? { ...x, [field]: e.target.value } : x)) }))
  const addExtra = () => setForm((f) => ({ ...f, extras: [...f.extras, { name: '', price: '' }] }))
  const removeExtra = (i) => setForm((f) => ({ ...f, extras: f.extras.filter((_, j) => j !== i) }))

  const total = [form.price, ...form.extras.map((x) => x.price)].reduce((sum, p) => sum + (Number(p) || 0), 0)

  function pickPhoto(e) {
    const file = e.target.files?.[0]
    e.target.value = '' // lets you pick the same file again after removing it
    if (!file) return
    setErrors(({ photoUrl: _, ...rest }) => rest)
    setAdjusting(URL.createObjectURL(file))
  }

  function stopAdjusting() {
    if (adjusting?.startsWith('blob:')) URL.revokeObjectURL(adjusting)
    setAdjusting(null)
  }

  function applyPhoto(photoUrl) {
    setForm((f) => ({ ...f, photoUrl }))
    stopAdjusting()
  }

  function badPhoto() {
    stopAdjusting()
    setErrors((x) => ({ ...x, photoUrl: "Couldn't read that image. Try a JPG or PNG." }))
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
          {adjusting ? (
            <PhotoAdjuster src={adjusting} onDone={applyPhoto} onCancel={stopAdjusting} onError={badPhoto} />
          ) : (
            <>
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
                {/* uploads only: a photo from another site can't be redrawn on a canvas */}
                {form.photoUrl.startsWith('data:') && (
                  <button type="button" className="btn btn-ghost" onClick={() => setAdjusting(form.photoUrl)}>
                    Adjust
                  </button>
                )}
                {form.photoUrl && (
                  <button type="button" className="btn btn-ghost" onClick={() => setForm((f) => ({ ...f, photoUrl: '' }))}>
                    Remove
                  </button>
                )}
              </div>
            </>
          )}
          {fieldError('photoUrl')}
        </div>

        <div className="grid-2">
          <label>
            Cafe
            <input value={form.name} onChange={set('name')} placeholder="All Is Well" />
            {fieldError('name')}
          </label>
          <label>
            Neighbourhood
            <input value={form.neighbourhood} onChange={set('neighbourhood')} list="hoods" placeholder="Ossington" />
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
          <input value={form.order} onChange={set('order')} placeholder="Iced Vanilla Latter" />
          {fieldError('order')}
        </label>

        <div className="extras">
          {form.extras.map((x, i) => (
            <div className="extra-row" key={i}>
              <label>
                Also ordered
                <input value={x.name} onChange={setExtra(i, 'name')} placeholder="Almond croissant" />
                {fieldError(`extras[${i}].name`)}
              </label>
              <label>
                Price
                <input type="number" inputMode="decimal" min="0" step="0.01" value={x.price} onChange={setExtra(i, 'price')} placeholder="4.25" />
                {fieldError(`extras[${i}].price`)}
              </label>
              <button type="button" className="btn btn-ghost extra-remove" onClick={() => removeExtra(i)} aria-label={`Remove item ${i + 1}`}>✕</button>
            </div>
          ))}
          {form.extras.length < 10 && (
            <button type="button" className="btn btn-ghost add-extra" onClick={addExtra}>+ Add another item</button>
          )}
          {form.extras.length > 0 && <p className="muted extras-total">Total {money.format(total)}</p>}
          {fieldError('extras')}
        </div>

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
            <span className="hint">Others see the photo, cafe, order, price and rating.</span>
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
