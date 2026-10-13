import { useSyncExternalStore } from 'react'

// Top-down cafe scene: steaming cup on a saucer with a cookie, torn sugar packet,
// spoon and receipt, set on a woven placemat.

// wide layout for laptops, tall layout for phones
const LAYOUTS = {
  wide: {
    viewBox: '0 0 1600 600',
    fit: 'xMidYMid slice',
    mat: { x: 170, y: 20, w: 1260, h: 544 },
    cx: 800,
    cy: 300,
    k: 1.32, // cup size
    sugar: { x: 335, y: 150, rot: -18, s: 1 },
    cookie: { x: 405, y: 372, r: 92 },
    spoon: { x: 1135, y: 305, rot: 96, s: 1.55 },
    receipt: { x: 1350, y: 312, rot: -7, s: 1 },
  },
  tall: {
    viewBox: '0 0 700 900',
    fit: 'xMidYMid meet',
    mat: { x: 30, y: 30, w: 640, h: 840 },
    cx: 350,
    cy: 450,
    k: 1.2,
    sugar: { x: 185, y: 118, rot: -14, s: 0.9 },
    cookie: { x: 165, y: 775, r: 72 },
    spoon: { x: 548, y: 772, rot: 58, s: 1.15 },
    receipt: { x: 532, y: 150, rot: 9, s: 0.55 },
  },
}

const PHONE = '(max-width: 700px)'

function useIsPhone() {
  return useSyncExternalStore(
    (onChange) => {
      const mq = window.matchMedia(PHONE)
      mq.addEventListener('change', onChange)
      return () => mq.removeEventListener('change', onChange)
    },
    () => window.matchMedia(PHONE).matches,
  )
}

const RECEIPT_LINES = [
  ['Dalgona matcha', '6.50'],
  ['Blueberry bagel', '4.25'],
  ['Subtotal', '10.75'],
  ['HST 13%', '1.40'],
]

const CHIPS = [
  [-0.37, -0.35, 13, 20], [0.17, -0.5, 11, -15], [0.43, -0.07, 14, 40], [-0.09, 0.11, 12, 70],
  [-0.54, 0.22, 11, -30], [0.13, 0.46, 13, 10], [-0.26, 0.57, 9, 55], [0.5, 0.37, 9, -40], [-0.63, -0.09, 8, 15],
]

function spiralPath(cx, cy, turns, maxRadius, offset = 0) {
  const steps = 240
  const end = turns * Math.PI * 2
  let d = ''
  for (let i = 0; i <= steps; i++) {
    const t = (i / steps) * end
    const r = (t / end) * maxRadius
    d += `${i === 0 ? 'M' : 'L'}${(cx + r * Math.cos(t + offset)).toFixed(1)},${(cy + r * Math.sin(t + offset)).toFixed(1)}`
  }
  return d
}

// shift a path drawn around (0,0) to the cup centre at cup scale
function place(d, cx, cy, k) {
  let i = 0
  return d.replace(/-?\d+(\.\d+)?/g, (n) => (i++ % 2 === 0 ? cx + +n * k : cy + +n * k).toFixed(1))
}

function Placemat({ x, y, w, h }) {
  return (
    <g>
      <rect x={x} y={y + 8} width={w} height={h} rx="30" fill="#3b2416" opacity="0.18" filter="url(#matShadow)" />
      <rect x={x} y={y} width={w} height={h} rx="30" fill="url(#weave)" />
      <rect x={x} y={y} width={w} height={h} rx="30" fill="url(#matLight)" />
      <rect x={x} y={y} width={w} height={h} rx="30" fill="none" stroke="#a88659" strokeWidth="14" />
      <rect x={x + 20} y={y + 20} width={w - 40} height={h - 40} rx="18" fill="none" stroke="#8c6a42" strokeWidth="1.5" strokeDasharray="7 6" opacity="0.7" />
    </g>
  )
}

export default function TableScene() {
  const S = LAYOUTS[useIsPhone() ? 'tall' : 'wide']
  const { cx, cy, k } = S
  const s = (n) => +(n * k).toFixed(1)
  const origin = { transformOrigin: `${cx}px ${cy}px` }

  const sugar = Array.from({ length: 16 }, (_, i) => ({
    angle: (i * 137) % 360,
    radius: s(50 + ((i * 29) % 60)),
    delay: -(i * 0.37),
    size: s(4 + (i % 3)),
  }))
  const steam = [
    { d: 'M-30 50 C-50 20 -5 0 -25 -35 C-40 -62 -10 -80 -18 -108', w: 18, delay: 0 },
    { d: 'M10 40 C30 10 -10 -10 12 -45 C28 -72 0 -90 10 -120', w: 22, delay: -1.4 },
    { d: 'M40 55 C25 25 62 5 44 -28 C32 -52 58 -72 50 -100', w: 16, delay: -2.7 },
    { d: 'M-5 60 C-25 32 15 12 -4 -20 C-18 -44 6 -62 0 -88', w: 14, delay: -3.6 },
  ]
  const ck = S.cookie

  return (
    <svg className="scene-svg" viewBox={S.viewBox} preserveAspectRatio={S.fit} role="img"
      aria-label="A cafe table from above: a steaming coffee, a cookie, a sugar packet, a spoon and a receipt">
      <defs>
        <radialGradient id="coffee" cx="45%" cy="42%" r="60%">
          <stop offset="0%" stopColor="#7a4a2c" />
          <stop offset="70%" stopColor="#4a2b1a" />
          <stop offset="100%" stopColor="#2e1a10" />
        </radialGradient>
        <radialGradient id="ceramic" cx="40%" cy="35%" r="70%">
          <stop offset="0%" stopColor="#fffdf9" />
          <stop offset="100%" stopColor="#e9dfd2" />
        </radialGradient>
        <linearGradient id="steel" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#f4f4f2" />
          <stop offset="50%" stopColor="#b9b7b2" />
          <stop offset="100%" stopColor="#e2e0dc" />
        </linearGradient>
        {/* inside of the spoon bowl: dark where it dips away from the light, bright where it curves up */}
        <radialGradient id="bowl" cx="68%" cy="72%" r="85%">
          <stop offset="0%" stopColor="#fbfbf9" />
          <stop offset="45%" stopColor="#cfccc6" />
          <stop offset="100%" stopColor="#85827c" />
        </radialGradient>
        <radialGradient id="cookie" cx="45%" cy="40%" r="65%">
          <stop offset="0%" stopColor="#e2ad6c" />
          <stop offset="75%" stopColor="#c98d4b" />
          <stop offset="100%" stopColor="#a86a30" />
        </radialGradient>

        {/* placemat: basket weave of jute strands */}
        <pattern id="weave" width="32" height="32" patternUnits="userSpaceOnUse">
          <rect width="32" height="32" fill="#d3b98f" />
          <rect x="0" y="0" width="16" height="8" fill="#dcc49c" />
          <rect x="0" y="8" width="16" height="8" fill="#cfb387" />
          <rect x="16" y="0" width="8" height="16" fill="#d8bf95" />
          <rect x="24" y="0" width="8" height="16" fill="#c9ad80" />
          <rect x="0" y="16" width="8" height="16" fill="#d8bf95" />
          <rect x="8" y="16" width="8" height="16" fill="#c9ad80" />
          <rect x="16" y="16" width="16" height="8" fill="#dcc49c" />
          <rect x="16" y="24" width="16" height="8" fill="#cfb387" />
          <path d="M0 8H16M0 16H32M16 24H32M16 0V32M8 16V32M24 0V16" stroke="#a88659" strokeWidth="0.8" opacity="0.5" />
        </pattern>
        <radialGradient id="matLight" cx="50%" cy="45%" r="70%">
          <stop offset="0%" stopColor="#fff6e6" stopOpacity="0.25" />
          <stop offset="100%" stopColor="#5a3a20" stopOpacity="0.18" />
        </radialGradient>

        <filter id="matShadow" x="-10%" y="-10%" width="120%" height="130%">
          <feGaussianBlur stdDeviation="14" />
        </filter>
        <filter id="soft" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3" />
        </filter>
        <filter id="steamBlur" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="6" />
        </filter>
        <filter id="object" x="-20%" y="-20%" width="140%" height="150%">
          <feDropShadow dx="0" dy="6" stdDeviation="7" floodColor="#2a1608" floodOpacity="0.3" />
        </filter>
        <clipPath id="surface">
          <circle cx={cx} cy={cy} r={s(122)} />
        </clipPath>
        <mask id="bite">
          <circle cx={ck.x} cy={ck.y} r={ck.r} fill="#fff" />
          <circle cx={ck.x + ck.r * 0.83} cy={ck.y - ck.r * 0.74} r={ck.r * 0.3} fill="#000" />
          <circle cx={ck.x + ck.r * 0.59} cy={ck.y - ck.r * 0.96} r={ck.r * 0.22} fill="#000" />
          <circle cx={ck.x + ck.r * 1.0} cy={ck.y - ck.r * 0.41} r={ck.r * 0.22} fill="#000" />
        </mask>
      </defs>

      <Placemat {...S.mat} />

      {/* torn sugar packet with a few spilled grains */}
      <g transform={`translate(${S.sugar.x} ${S.sugar.y}) rotate(${S.sugar.rot}) scale(${S.sugar.s})`}>
        <g filter="url(#object)">
          <path d="M-70 -30 H70 V30 H-70 Z" fill="#fbf7ef" />
          <rect x="-70" y="-30" width="10" height="60" fill="#8a5a3b" />
          <path d="M58 -30 l4 6 -3 6 4 6 -3 6 4 6 -3 6 4 6 -3 6 4 6 V-30 Z" fill="#ece3d4" />
          <text x="-2" y="7" textAnchor="middle" className="packet">SUGAR</text>
        </g>
        {[[82, -8], [90, 8], [100, -2], [96, 18], [110, 10]].map(([x, y], i) => (
          <rect key={i} x={x} y={y} width="4" height="4" rx="1" fill="#fffaf2" stroke="#d8c7ad" strokeWidth="0.5" />
        ))}
      </g>

      {/* cookie with a bite out of it, and crumbs */}
      <g filter="url(#object)">
        <g mask="url(#bite)">
          <circle cx={ck.x} cy={ck.y} r={ck.r} fill="url(#cookie)" />
          {[[-0.4, -0.4, 0.17], [0.33, 0.3, 0.15], [-0.26, 0.52, 0.13], [0.52, -0.13, 0.11]].map(([dx, dy, r], i) => (
            <circle key={i} cx={ck.x + dx * ck.r} cy={ck.y + dy * ck.r} r={r * ck.r} fill="#a86d34" opacity="0.35" />
          ))}
          {CHIPS.map(([dx, dy, sz, rot], i) => {
            const x = ck.x + dx * ck.r
            const y = ck.y + dy * ck.r
            const z = (sz * ck.r) / 92
            return <rect key={i} x={x - z / 2} y={y - z / 2} width={z} height={z * 0.8} rx="3" fill="#3d2415" transform={`rotate(${rot} ${x} ${y})`} />
          })}
        </g>
      </g>
      {[[1.2, -0.76, 6], [1.38, -0.46, 4], [1.1, -0.33, 3], [1.46, -0.68, 3]].map(([dx, dy, r], i) => (
        <circle key={i} cx={ck.x + dx * ck.r} cy={ck.y + dy * ck.r} r={r} fill="#c98d4b" />
      ))}

      {/* spoon: tapered handle and a concave bowl */}
      <g transform={`translate(${S.spoon.x} ${S.spoon.y}) rotate(${S.spoon.rot}) scale(${S.spoon.s})`} filter="url(#object)">
        <path d="M-32 -2.2 C0 -2.6 40 -4.8 62 -4.6 A4.6 4.6 0 0 1 62 4.6 C40 4.8 0 2.6 -32 2.2 Z" fill="url(#steel)" stroke="#a19d97" strokeWidth="0.6" />
        <ellipse cx="-48" cy="0" rx="21" ry="14" fill="url(#steel)" stroke="#9d9a94" strokeWidth="0.8" />
        <ellipse cx="-47.5" cy="0.4" rx="17.5" ry="11" fill="url(#bowl)" />
        <path d="M-63 1 A16.5 10.5 0 0 1 -38 -9.6" fill="none" stroke="#6f6c67" strokeWidth="1.4" strokeLinecap="round" opacity="0.45" />
        <path d="M-36 6.5 A15 9 0 0 1 -52 10" fill="none" stroke="#ffffff" strokeWidth="2.2" strokeLinecap="round" opacity="0.85" />
        <ellipse cx="-58" cy="-7" rx="5" ry="2" fill="#ffffff" opacity="0.7" transform="rotate(-20 -58 -7)" />
        <path d="M40 -2.5 H58" stroke="#ffffff" strokeWidth="1.2" strokeLinecap="round" opacity="0.7" />
      </g>

      {/* receipt */}
      <g transform={`translate(${S.receipt.x} ${S.receipt.y}) rotate(${S.receipt.rot}) scale(${S.receipt.s})`} filter="url(#object)">
        <path d="M-105 -150 H105 V140 l-10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 -10 -10 -10 10 Z" fill="#fdfcf8" />
        <g className="receipt" fill="#4a4a48" fontSize="12.5">
          <text x="0" y="-116" textAnchor="middle" fontWeight="600" fontSize="18" letterSpacing="3">18FEET</text>
          <text x="0" y="-98" textAnchor="middle">Toronto, ON</text>
          <line x1="-88" x2="88" y1="-86" y2="-86" stroke="#9a9a96" strokeDasharray="4 3" />
          {RECEIPT_LINES.map(([item, price], i) => (
            <g key={item}>
              <text x="-88" y={-62 + i * 22}>{item}</text>
              <text x="88" y={-62 + i * 22} textAnchor="end">{price}</text>
            </g>
          ))}
          <text x="-88" y="38" fontWeight="600" fontSize="14">TOTAL</text>
          <text x="88" y="38" textAnchor="end" fontWeight="600" fontSize="14">$12.15</text>
          <line x1="-88" x2="88" y1="54" y2="54" stroke="#9a9a96" strokeDasharray="4 3" />
          <text x="0" y="80" textAnchor="middle">SEP 28 2026  08:41</text>
          <text x="0" y="104" textAnchor="middle">Thank you!</text>
        </g>
      </g>

      {/* saucer and cup */}
      <circle cx={cx} cy={cy} r={s(188)} fill="#efe5d8" filter="url(#object)" />
      <circle cx={cx} cy={cy} r={s(160)} fill="none" stroke="#e2d5c4" strokeWidth="2" />
      <circle cx={cx} cy={cy + 8} r={s(146)} fill="#3b2416" opacity="0.28" filter="url(#soft)" />

      <rect x={cx + s(118)} y={cy - s(22)} width={s(64)} height={s(44)} rx={s(22)} fill="url(#ceramic)" stroke="#dccfbf" strokeWidth="2" />
      <rect x={cx + s(134)} y={cy - s(9)} width={s(34)} height={s(18)} rx={s(9)} fill="#efe5d8" />
      <circle cx={cx} cy={cy} r={s(142)} fill="url(#ceramic)" stroke="#dccfbf" strokeWidth="2" />
      <circle cx={cx} cy={cy} r={s(122)} fill="url(#coffee)" />

      <g clipPath="url(#surface)">
        <g className="spin spin-slow" style={origin}>
          <path d={spiralPath(cx, cy, 3.2, s(112))} fill="none" stroke="#d9b38a" strokeWidth={s(9)} strokeLinecap="round" opacity="0.55" filter="url(#soft)" />
        </g>
        <g className="spin spin-slower" style={origin}>
          <path d={spiralPath(cx, cy, 2.6, s(104), Math.PI)} fill="none" stroke="#e8cda9" strokeWidth={s(4)} strokeLinecap="round" opacity="0.35" filter="url(#soft)" />
        </g>
        {sugar.map((g, i) => (
          <g key={i} className="orbit" style={{ ...origin, '--start': `${g.angle}deg`, animationDelay: `${g.delay}s` }}>
            <rect className="granule" x={cx - g.size / 2} y={cy - g.size / 2} width={g.size} height={g.size} rx="1"
              fill="#fffaf2" style={{ ...origin, '--r': `${g.radius}px`, animationDelay: `${g.delay}s` }} />
          </g>
        ))}
        <circle cx={cx} cy={cy} r={s(122)} fill="none" stroke="#1f120a" strokeWidth={s(8)} opacity="0.35" filter="url(#soft)" />
      </g>

      <g className="steam" fill="none" stroke="#ffffff" strokeLinecap="round" filter="url(#steamBlur)">
        {steam.map((w, i) => (
          <path key={i} d={place(w.d, cx, cy, k)} strokeWidth={s(w.w)} style={{ animationDelay: `${w.delay}s` }} />
        ))}
      </g>
    </svg>
  )
}
