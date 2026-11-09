import type { DayPoint } from '../types'

// Simpel SVG-linjegraf over antal viderestillede samtaler pr. dag.
export function TrendChart({ points }: { points: DayPoint[] }) {
  const w = 400, h = 160
  const max = Math.max(1, ...points.map((p) => p.conversations))
  const line = points
    .map((p, i) => `${(i / Math.max(1, points.length - 1)) * w},${h - (p.conversations / max) * (h - 10)}`)
    .join(' ')
  return (
    <section className="card">
      <h2>Udvikling over tid</h2>
      <svg viewBox={`0 0 ${w} ${h}`} className="chart">
        <polyline points={line} className="line" />
      </svg>
      <div className="axis"><span>{points[0]?.date}</span><span>{points[points.length - 1]?.date}</span></div>
      <p className="hint">— viderestillede samtaler</p>
    </section>
  )
}
