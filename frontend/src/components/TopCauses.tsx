import type { Cause } from '../types'

type Props = { causes: Cause[]; onOpen: (id: string) => void }

export function TopCauses({ causes, onOpen }: Props) {
  const max = Math.max(1, ...causes.map((c) => c.count))
  return (
    <section className="card">
      <h2>Top viderestillingsårsager</h2>
      {causes.map((c) => (
        <button key={c.id} className="bar-row" onClick={() => onOpen(c.id)}>
          <span className="bar-label">{c.title}</span>
          <span className="bar-track"><span className="bar" style={{ width: `${(c.count / max) * 100}%` }} /></span>
          <span className="bar-value">{c.count}</span>
        </button>
      ))}
      <p className="hint">Klik på en årsag for at se samtalerne</p>
    </section>
  )
}
