// Illustrated top-down "photos" of drinks, used for the demo's sample visits
// and as the placeholder when a visit has no photo yet.

const STYLES = {
  flatwhite: { bg: '#7a4d2e', vessel: 'cup', drink: '#9a6541', foam: '#f3e2c8', art: 'heart' },
  cortado: { bg: '#d8d2c8', vessel: 'glass', drink: '#8f5e3b', foam: '#efdcc0', art: 'rosetta' },
  matcha: { bg: '#ece6dd', vessel: 'glass', drink: '#f1ede2', foam: '#8fb455', art: 'cloud', side: 'bagel' },
  coldbrew: { bg: '#2f3b3a', vessel: 'glass', drink: '#3a2014', art: 'ice' },
  icedlatte: { bg: '#4f6b70', vessel: 'glass', drink: '#b58a63', art: 'ice' },
  drip: { bg: '#cdb487', vessel: 'mug', drink: '#2e1a10', art: 'shine' },
  americano: { bg: '#b98d6b', vessel: 'cup', drink: '#3d2415', foam: '#a8703f', art: 'crema', side: 'croissant' },
}

function heart(c) {
  return `<path d="M200 262 C140 222 138 162 176 158 C192 156 200 170 200 180 C200 170 208 156 224 158 C262 162 260 222 200 262Z" fill="${c}"/>`
}

function rosetta(c) {
  let leaves = ''
  for (let i = 0; i < 6; i++) {
    const y = 158 + i * 17
    const w = 58 - i * 7
    leaves += `<path d="M${200 - w} ${y + 10} Q200 ${y - 16} ${200 + w} ${y + 10} Q200 ${y - 2} ${200 - w} ${y + 10}Z" fill="${c}"/>`
  }
  return leaves + `<rect x="198" y="150" width="4" height="120" rx="2" fill="${c}"/>`
}

function side(kind) {
  if (kind === 'croissant') {
    return `<g transform="translate(330 330) rotate(-30)" filter="url(#sh)">
      <path d="M-62 10 C-50 -40 50 -40 62 10 C40 -6 -40 -6 -62 10Z" fill="#d49a52"/>
      <path d="M-30 -18 L-22 2 M0 -26 L0 -4 M30 -18 L22 2" stroke="#9c6428" stroke-width="3" stroke-linecap="round"/>
    </g>`
  }
  if (kind === 'bagel') {
    return `<g filter="url(#sh)">
      <circle cx="330" cy="330" r="60" fill="#c98b4c"/>
      <circle cx="330" cy="330" r="46" fill="#e6c79c"/>
      <circle cx="310" cy="318" r="6" fill="#3b3a6b"/><circle cx="342" cy="344" r="5" fill="#3b3a6b"/>
      <circle cx="350" cy="312" r="5" fill="#46457a"/><circle cx="318" cy="350" r="4" fill="#3b3a6b"/>
      <circle cx="330" cy="330" r="14" fill="${'#ece6dd'}"/>
    </g>`
  }
  return ''
}

export function drinkArt(kind = 'flatwhite') {
  const d = STYLES[kind] ?? STYLES.flatwhite
  const glass = d.vessel === 'glass'
  const rim = glass
    ? `<circle cx="200" cy="210" r="118" fill="rgba(255,255,255,0.18)" stroke="rgba(255,255,255,0.75)" stroke-width="7" filter="url(#sh)"/>`
    : `<circle cx="200" cy="210" r="${d.vessel === 'mug' ? 0 : 172}" fill="#efe5d8" filter="url(#sh)"/>
       <rect x="${d.vessel === 'mug' ? 306 : 314}" y="194" width="56" height="34" rx="17" fill="#fbf7f1"/>
       <circle cx="200" cy="210" r="126" fill="#fbf7f1" stroke="#e2d6c6" stroke-width="2" filter="url(#sh)"/>`

  let art = ''
  if (d.art === 'heart') art = heart(d.foam)
  if (d.art === 'rosetta') art = rosetta(d.foam)
  if (d.art === 'cloud') {
    // whipped matcha sitting on milk, with soft peaks
    art = `<circle cx="200" cy="210" r="90" fill="${d.foam}"/>
      <path d="M140 200 q20 -24 40 0 t40 0 t40 0 M150 236 q18 -20 36 0 t36 0 t36 0 M160 172 q16 -18 32 0 t32 0" stroke="#b3d27e" stroke-width="7" stroke-linecap="round" fill="none"/>
      <circle cx="200" cy="210" r="90" fill="none" stroke="#7a9c45" stroke-width="4"/>`
  }
  if (d.art === 'crema') art = `<circle cx="200" cy="210" r="98" fill="none" stroke="${d.foam}" stroke-width="12" opacity="0.7"/>`
  if (d.art === 'shine') art = `<ellipse cx="170" cy="180" rx="34" ry="14" fill="#fff" opacity="0.18" transform="rotate(-30 170 180)"/>`
  if (d.art === 'ice') {
    art = [[165, 185, 18], [228, 172, -12], [215, 240, 30], [160, 245, 8]]
      .map(([x, y, r]) => `<rect x="${x - 28}" y="${y - 28}" width="56" height="56" rx="12" fill="rgba(255,255,255,0.28)" stroke="rgba(255,255,255,0.6)" stroke-width="3" transform="rotate(${r} ${x} ${y})"/>`)
      .join('')
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400">
    <defs>
      <radialGradient id="l" cx="40%" cy="35%" r="75%"><stop offset="0" stop-color="#fff" stop-opacity="0.18"/><stop offset="1" stop-color="#000" stop-opacity="0.22"/></radialGradient>
      <filter id="sh" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy="8" stdDeviation="9" flood-color="#1c0e05" flood-opacity="0.35"/></filter>
    </defs>
    <rect width="400" height="400" fill="${d.bg}"/>
    <rect width="400" height="400" fill="url(#l)"/>
    ${side(d.side)}
    ${rim}
    <circle cx="200" cy="210" r="${glass ? 108 : 106}" fill="${d.drink}"/>
    ${art}
  </svg>`
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

// pick an illustration from what was ordered
function guessKind(order = '') {
  const o = order.toLowerCase()
  if (o.includes('matcha')) return 'matcha'
  if (o.includes('cold brew')) return 'coldbrew'
  if (o.includes('iced')) return 'icedlatte'
  if (o.includes('cortado') || o.includes('macchiato')) return 'cortado'
  if (o.includes('americano') || o.includes('espresso')) return 'americano'
  if (o.includes('drip') || o.includes('filter') || o.includes('coffee')) return 'drip'
  return 'flatwhite'
}

export const photoFor = (visit) => visit.photoUrl || drinkArt(guessKind(visit.order))
