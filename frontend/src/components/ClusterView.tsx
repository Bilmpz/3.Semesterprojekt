import { useEffect, useState } from 'react'
import { getConversations } from '../api'
import type { Cluster, Conversation } from '../types'

type Props = { cluster: Cluster; onBack: () => void }

// Åbnes når man klikker på en årsag: viser samtalerne i clusteret
export function ClusterView({ cluster, onBack }: Props) {
  const [list, setList] = useState<Conversation[]>([])
  const [open, setOpen] = useState<string | null>(null)
  useEffect(() => { getConversations(cluster.id).then(setList) }, [cluster.id])

  return (
    <section className="card">
      <button onClick={onBack}>← Tilbage</button>
      <h2>{cluster.title}</h2>
      <p className="muted">{cluster.description} · {cluster.count} samtaler</p>
      {list.map((s) => (
        <div key={s.id} className="conversation">
          <button className="link" onClick={() => setOpen(open === s.id ? null : s.id)}>
            {new Date(s.timestamp).toLocaleString('da-DK')} · {s.company} · {s.reason}
          </button>
          {open === s.id && (
            <div>
              <p><strong>Forklaring:</strong> {s.explanation}</p>
              <pre>{s.transcript}</pre>
            </div>
          )}
        </div>
      ))}
    </section>
  )
}
