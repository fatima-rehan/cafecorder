// Search-as-you-type for places, using Photon (an OpenStreetMap search built for autocomplete;
// Nominatim's own policy doesn't allow that). Called straight from the browser.
const PHOTON = 'https://photon.komoot.io/api/'
const NEARBY_KM = 50

function km(a, b) {
  const rad = (d) => (d * Math.PI) / 180
  const dLat = rad(b.latitude - a.latitude)
  const dLon = rad(b.longitude - a.longitude)
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLon / 2) ** 2
  return 12742 * Math.asin(Math.sqrt(h))
}

function toPlace({ properties: p, geometry }) {
  const street = [p.housenumber, p.street].filter(Boolean).join(' ')
  return {
    id: `${p.osm_type}${p.osm_id}`,
    name: p.name ?? '',
    street,
    city: p.city ?? p.town ?? p.village ?? (p.type === 'city' ? p.name : ''),
    // not p.locality: in many cities that's a voting ward ("Spadina–Fort York"), not what locals call the area
    area: p.district ?? '',
    region: p.state ?? '',
    country: p.country ?? '',
    isCafe: p.osm_value === 'cafe',
    longitude: geometry.coordinates[0],
    latitude: geometry.coordinates[1],
  }
}

async function photon(params, signal) {
  const res = await fetch(`${PHOTON}?${new URLSearchParams(params)}`, { signal })
  if (!res.ok) throw new Error('Place search failed')
  return (await res.json()).features.map(toPlace)
}

// Cities and towns, for the City field
export function searchCities(query, signal) {
  return photon([['q', query], ['limit', '6'], ['layer', 'city']], signal)
}

// Cafes and street addresses. With a city picked, results are pulled toward it and anything
// far away is dropped, so "Pilot Coffee" in Toronto doesn't suggest one in another country.
export async function searchAddresses(query, near, signal) {
  const params = [['q', query], ['limit', '8']]
  if (near) params.push(['lat', String(near.latitude)], ['lon', String(near.longitude)])
  const places = await photon(params, signal)
  return (near ? places.filter((p) => km(p, near) <= NEARBY_KM) : places).filter((p) => p.street || p.isCafe)
}
