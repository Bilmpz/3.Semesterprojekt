import { useState } from 'react'
import type { Company, Filters } from '../types'

type Props = { companies: Company[]; value: Filters; onApply: (f: Filters) => void }

export function FilterBar({ companies, value, onApply }: Props) {
  const [f, setF] = useState(value)
  return (
    <section className="card dashed filters">
      <label>Virksomhed
        <select value={f.companyId} onChange={(e) => setF({ ...f, companyId: e.target.value })}>
          <option value="">Alle virksomheder</option>
          {companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </label>
      <label>Periode
        <select value={f.days} onChange={(e) => setF({ ...f, days: Number(e.target.value) })}>
          <option value={7}>Sidste 7 dage</option>
          <option value={30}>Sidste 30 dage</option>
          <option value={90}>Sidste 90 dage</option>
        </select>
      </label>
      <label className="grow">Søg i årsager
        <input placeholder='fx "faktura"' value={f.search} onChange={(e) => setF({ ...f, search: e.target.value })} />
      </label>
      <button onClick={() => onApply(f)}>Anvend</button>
    </section>
  )
}
