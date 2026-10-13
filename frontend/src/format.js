export const money = new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD' })

// visitDate is a plain "2026-09-20"; format it in UTC so it never shifts a day
const shortDate = new Intl.DateTimeFormat('en-CA', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' })
export const formatDate = (iso) => shortDate.format(new Date(iso))

// "LIMITED" -> "Limited"
export const pretty = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : '')
