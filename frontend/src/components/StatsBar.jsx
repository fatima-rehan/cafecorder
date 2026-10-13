import { money } from '../format.js'

export default function StatsBar({ stats }) {
  const tiles = [
    { label: 'Visits', value: stats?.visits ?? '–' },
    { label: 'Cafes', value: stats?.uniqueCafes ?? '–' },
    { label: 'Total spent', value: stats ? money.format(stats.totalSpent) : '–' },
    { label: 'Avg rating', value: stats?.averageRating != null ? stats.averageRating.toFixed(1) : '–' },
  ]

  return (
    <section className="stats">
      {tiles.map((t) => (
        <div className="stat" key={t.label}>
          <span className="stat-value">{t.value}</span>
          <span className="stat-label">{t.label}</span>
        </div>
      ))}
    </section>
  )
}
