// 0 = unrated, otherwise 1-5 coffee beans
export default function Rating({ value }) {
  if (!value) return <span className="rating unrated">Unrated</span>
  return (
    <span className="rating" aria-label={`${value} out of 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} className={n <= value ? 'bean on' : 'bean'} />
      ))}
    </span>
  )
}
