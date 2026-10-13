import { pretty } from '../format.js'

export default function StudyChips({ visit }) {
  return (
    <div className="study">
      <span className="study-tag">Study spot</span>
      <ul className="chips">
        <li className={visit.hasWifi ? 'yes' : 'no'}>Wifi</li>
        <li className={visit.hasOutlets ? 'yes' : 'no'}>Outlets</li>
        <li>{pretty(visit.seating)} seating</li>
        <li>{pretty(visit.noiseLevel)}</li>
        {visit.laptopPolicy && <li>Laptops: {visit.laptopPolicy}</li>}
      </ul>
    </div>
  )
}
