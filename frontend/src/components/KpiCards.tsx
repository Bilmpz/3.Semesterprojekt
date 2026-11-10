import type { Dashboard } from '../types'

export function KpiCards({ d }: { d: Dashboard }) {
  return (
    <section className="kpis" aria-label="Samlet overblik">
      <div className="card kpi">
        <span>Viderestillinger i alt</span>
        <strong>{d.totalConversations.toLocaleString('da-DK')}</strong>
        <span className="hint">For de valgte filtre, inklusive samtaler uden cluster</span>
      </div>
      <div className="card kpi">
        <span>Identificerede årsager / clusters</span>
        <strong>{d.causesFound.toLocaleString('da-DK')}</strong>
        <span className="hint">Grupper af samtaler med lignende viderestillingsårsager</span>
      </div>
    </section>
  )
}
