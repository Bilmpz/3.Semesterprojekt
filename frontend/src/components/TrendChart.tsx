import type { DayPoint } from '../types'

export function TrendChart({ points }: { points: DayPoint[] }) {
  const max = Math.max(1, ...points.map((p) => p.conversations))
  const date = (value: string) => new Date(value).toLocaleDateString('da-DK', { day: 'numeric', month: 'short' })
  return (
    <section className="activity-chart" aria-label="Viderestillinger per dag">
      <div className="row">
        <span>Udvikling i den valgte periode</span>
        <span>{points.length > 0 ? `${date(points[0].date)} – ${date(points[points.length - 1].date)}` : ''}</span>
      </div>
      {points.length === 0 ? <p className="hint">Ingen viderestillinger i den valgte periode.</p> : (
        <div className="daily-bars">
          {points.map((point) => (
            <div className="daily-bar-column" key={point.date} title={`${date(point.date)}: ${point.conversations} viderestillinger`}>
              <span className="daily-bar-value">{point.conversations}</span>
              <div className="daily-bar" style={{ height: `${Math.max(2, point.conversations / max * 72)}px` }} />
              <span className="daily-bar-date">{date(point.date)}</span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
