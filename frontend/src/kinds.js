// Matches CafeKind on the backend. Add new ones in both places.
export const KINDS = [
  { value: 'COFFEE', label: 'Coffee' },
  { value: 'MATCHA', label: 'Matcha' },
  { value: 'TEA_HOUSE', label: 'Tea house' },
  { value: 'YEMENI', label: 'Yemeni' },
]

const LABELS = Object.fromEntries(KINDS.map((k) => [k.value, k.label]))
export const kindLabel = (value) => LABELS[value] ?? value
