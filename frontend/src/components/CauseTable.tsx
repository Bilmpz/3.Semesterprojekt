import { useState } from 'react'
import type { Cluster } from '../types'

type Props = { clusters: Cluster[]; onOpen: (id: string) => void }
type Sort = 'count-desc' | 'count-asc' | 'title'

export function CauseTable({ clusters, onOpen }: Props) {
  const [sort, setSort] = useState<Sort>('count-desc')
  const sorted = [...clusters].sort((a, b) => {
    const titleOrder = a.title.localeCompare(b.title, 'da')
    if (sort === 'title') return titleOrder
    return (sort === 'count-desc' ? b.count - a.count : a.count - b.count) || titleOrder
  })

  return (
    <section className="card">
      <div className="row cluster-heading">
        <div>
          <h2>Identificerede årsager og clusters</h2>
          <p className="hint">Alle clusters for den valgte virksomhed, periode og søgning. Samtaler uden cluster indgår i det samlede antal viderestillinger.</p>
        </div>
        <label className="sort">Sortér clusters
          <select value={sort} onChange={(e) => setSort(e.target.value as Sort)}>
            <option value="count-desc">Flest samtaler først</option>
            <option value="count-asc">Færrest samtaler først</option>
            <option value="title">Årsag, alfabetisk</option>
          </select>
        </label>
      </div>
      {sorted.length === 0 ? (
        <p className="hint">Ingen clusters matcher de valgte filtre. Prøv en anden periode eller søgning.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Årsag / cluster</th>
                <th scope="col">Beskrivelse</th>
                <th scope="col" aria-sort={sort === 'count-desc' ? 'descending' : sort === 'count-asc' ? 'ascending' : 'none'}>Antal samtaler</th>
                <th scope="col">Andel af viderestillinger</th>
                <th scope="col"><span className="muted">Detaljer</span></th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((c) => (
                <tr key={c.id}>
                  <th scope="row">{c.title || 'Unavngivet cluster'}</th>
                  <td className="muted">{c.description || 'Ingen beskrivelse endnu.'}</td>
                  <td>{c.count.toLocaleString('da-DK')}</td>
                  <td>{c.share} %</td>
                  <td><button aria-label={`Se samtaler i ${c.title || 'unavngivet cluster'}`} onClick={() => onOpen(c.id)}>Se samtaler</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="hint" aria-live="polite">Viser {sorted.length} clusters med i alt {clusters.reduce((sum, c) => sum + c.count, 0).toLocaleString('da-DK')} samtaler.</p>
    </section>
  )
}
