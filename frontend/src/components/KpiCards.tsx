import type { Dashboard } from '../types'

export function KpiCards({ d }: { d: Dashboard }) {
  const kpi = (label: string, value: string) => (
    <div className="card kpi"><span>{label}</span><strong>{value}</strong></div>
  )
  return (
    <section className="kpis">
      {kpi('Viderestillede samtaler', String(d.totalConversations))}
      {kpi('Fundne årsager', String(d.causesFound))}
    </section>
  )
}
