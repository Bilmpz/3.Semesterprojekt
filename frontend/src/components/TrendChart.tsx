import type { DayPoint } from '../types'

// Simpel SVG-linjegraf over antal viderestillede samtaler pr. dag.
export function TrendChart({ points }: { points: DayPoint[] }) {
  const width = 440
  const height = 220
  const plot = { left: 48, right: 12, top: 12, bottom: 42 }
  const plotWidth = width - plot.left - plot.right
  const plotHeight = height - plot.top - plot.bottom
  const maxCount = Math.max(0, ...points.map((point) => point.conversations))
  const yStep = Math.max(1, Math.ceil(maxCount / 4))
  const yMax = yStep * 4
  const firstDay = points.length ? Date.parse(`${points[0].date}T00:00:00Z`) : 0
  const lastDay = points.length ? Date.parse(`${points[points.length - 1].date}T00:00:00Z`) : 0
  const daySpan = Math.max(1, (lastDay - firstDay) / 86_400_000)
  const xForDate = (date: string) =>
    plot.left + ((Date.parse(`${date}T00:00:00Z`) - firstDay) / 86_400_000 / daySpan) * plotWidth
  const yForCount = (count: number) => plot.top + plotHeight - (count / yMax) * plotHeight
  const line = points.map((point) => `${xForDate(point.date)},${yForCount(point.conversations)}`).join(' ')
  const xTickIndexes = [...new Set([0, ...Array.from(
    { length: 3 },
    (_, index) => Math.round((index + 1) * (points.length - 1) / 4),
  ), points.length - 1])]
  const yTicks = Array.from({ length: 5 }, (_, index) => index * yStep)

  return (
    <section className="card">
      <h2>Udvikling over tid</h2>
      {points.length > 0 ? (
        <>
          <svg viewBox={`0 0 ${width} ${height}`} className="chart" role="img" aria-label="Viderestillede samtaler pr. dag">
            {yTicks.map((tick) => (
              <g key={tick}>
                <line x1={plot.left} x2={width - plot.right} y1={yForCount(tick)} y2={yForCount(tick)} className="chart-grid" />
                <text x={plot.left - 8} y={yForCount(tick) + 4} className="chart-label" textAnchor="end">{tick}</text>
              </g>
            ))}
            <line x1={plot.left} x2={plot.left} y1={plot.top} y2={plot.top + plotHeight} className="chart-axis" />
            <line x1={plot.left} x2={width - plot.right} y1={plot.top + plotHeight} y2={plot.top + plotHeight} className="chart-axis" />
            {xTickIndexes.map((index) => {
              const point = points[index]
              const x = xForDate(point.date)
              return (
                <g key={point.date}>
                  <line x1={x} x2={x} y1={plot.top + plotHeight} y2={plot.top + plotHeight + 4} className="chart-axis" />
                  <text x={x} y={plot.top + plotHeight + 18} className="chart-label" textAnchor="middle">
                    {point.date.slice(5)}
                  </text>
                </g>
              )
            })}
            <polyline points={line} className="line" />
            <text x={plot.left + plotWidth / 2} y={height - 4} className="chart-label" textAnchor="middle">Dato</text>
            <text
              x={14}
              y={plot.top + plotHeight / 2}
              className="chart-label"
              textAnchor="middle"
              transform={`rotate(-90 14 ${plot.top + plotHeight / 2})`}
            >
              Samtaler
            </text>
          </svg>
          <p className="hint">— Viser viderestillede samtaler pr. dag. X-aksen er dato, Y-aksen er antal. Højere punkter betyder flere samtaler den dag.</p>
        </>
      ) : <p className="hint">Ingen samtaler i den valgte periode.</p>}
    </section>
  )
}
