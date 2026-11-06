/**
 * Format a date string to a human-readable format.
 * Input: "2024-03-21 10:30:00" or similar DB timestamp
 * Output: "Mar 21, 2024 10:30 AM"
 */
export function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(typeof dateStr === 'string' ? dateStr.replace(' ', 'T') : dateStr)
  if (isNaN(d.getTime())) return String(dateStr)
  return d.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  })
}

/**
 * Returns a human-readable "time until" or "time ago" string.
 * e.g. "expires in 47 min", "expired 2 hrs ago"
 */
export function timeUntil(dateStr) {
  if (!dateStr) return ''
  const d = new Date(typeof dateStr === 'string' ? dateStr.replace(' ', 'T') : dateStr)
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const diffMs = d - now
  const diffSec = Math.round(diffMs / 1000)
  const diffMin = Math.round(diffSec / 60)
  const diffHrs = Math.round(diffMin / 60)

  if (diffMs > 0) {
    // Future
    if (diffMin < 1) return 'expires in < 1 min'
    if (diffMin < 60) return `expires in ${diffMin} min`
    return `expires in ${diffHrs} hr${diffHrs !== 1 ? 's' : ''}`
  } else {
    // Past
    const absSec = Math.abs(diffSec)
    const absMin = Math.abs(diffMin)
    const absHrs = Math.abs(diffHrs)
    const absDays = Math.round(absHrs / 24)
    if (absSec < 60) return 'just expired'
    if (absMin < 60) return `expired ${absMin} min ago`
    if (absHrs < 24) return `expired ${absHrs} hr${absHrs !== 1 ? 's' : ''} ago`
    return `expired ${absDays} day${absDays !== 1 ? 's' : ''} ago`
  }
}

/**
 * Returns a color for the expiry chip: 'error' if < 10 min, 'warning' if < 1hr, 'success' otherwise.
 */
export function expiryColor(dateStr) {
  if (!dateStr) return 'default'
  const d = new Date(typeof dateStr === 'string' ? dateStr.replace(' ', 'T') : dateStr)
  if (isNaN(d.getTime())) return 'default'
  const diffMin = (d - new Date()) / 60000
  if (diffMin < 10) return 'error'
  if (diffMin < 60) return 'warning'
  return 'success'
}
