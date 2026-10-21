import { kindLabel } from '../kinds.js'

// Small labels like "Matcha" or "Yemeni" on a visit
export default function KindTags({ kinds }) {
  if (!kinds?.length) return null
  return (
    <ul className="kind-tags" aria-label="Type of place">
      {kinds.map((k) => <li key={k}>{kindLabel(k)}</li>)}
    </ul>
  )
}
