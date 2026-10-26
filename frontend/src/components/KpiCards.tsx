import type { Dashboard } from '../types'

export function KpiCards({ d }: { d: Dashboard }) {
  const kpi = (label: string, value: string) => (
    <div className="card kpi"><span>{label}</span><strong>{value}</strong></div>
  )
  return (
    <section className="kpis">
      {kpi('Samtaler i alt', String(d.totalConversations))}
      {kpi('Viderestillinger', String(d.transfers))}
      {kpi('Viderestillingsrate', `${d.transferRate} %`)}
      {kpi('Fundne årsager', String(d.causesFound))}
    </section>
  )
}
