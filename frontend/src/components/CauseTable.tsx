import { useState } from 'react'
import type { Cluster } from '../types'

type Props = { clusters: Cluster[]; onOpen: (id: string) => void }
const arrow = { up: '▲', down: '▼', flat: '–' }

export function CauseTable({ clusters, onOpen }: Props) {
  const [sort, setSort] = useState<'count' | 'title'>('count')
  const [showAll, setShowAll] = useState(false)
  const sorted = [...clusters].sort((a, b) => (sort === 'count' ? b.count - a.count : a.title.localeCompare(b.title)))
  const rows = showAll ? sorted : sorted.slice(0, 6)

  return (
    <section className="card">
      <div className="row">
        <div>
          <h2>Alle årsager (clusters)</h2>
          <p className="hint">Hver række er ét cluster fundet af pipelinen – gælder valgt virksomhed og periode</p>
        </div>
        <label className="sort">Sortér
          <select value={sort} onChange={(e) => setSort(e.target.value as 'count' | 'title')}>
            <option value="count">Flest samtaler</option>
            <option value="title">Alfabetisk</option>
          </select>
        </label>
      </div>
      <table>
        <thead><tr><th>Årsag</th><th>Kort beskrivelse</th><th>Samtaler</th><th>Andel</th><th>Tendens</th><th /></tr></thead>
        <tbody>
          {rows.map((c) => (
            <tr key={c.id}>
              <td>{c.title}</td>
              <td className="muted">{c.description}</td>
              <td>{c.count}</td>
              <td>{c.share} %</td>
              <td>{arrow[c.trend]}</td>
              <td><button onClick={() => onOpen(c.id)}>Åbn →</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row">
        <span>Viser {rows.length} af {clusters.length} årsager</span>
        {!showAll && clusters.length > 6 && <button onClick={() => setShowAll(true)}>Vis alle</button>}
      </div>
    </section>
  )
}
