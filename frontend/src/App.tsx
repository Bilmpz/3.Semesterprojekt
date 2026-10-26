import { useEffect, useState } from 'react'
import { getClusters, getCompanies, getDashboard, startRun } from './api'
import { CauseTable } from './components/CauseTable'
import { ClusterView } from './components/ClusterView'
import { FilterBar } from './components/FilterBar'
import { Header, type Page } from './components/Header'
import { KpiCards } from './components/KpiCards'
import { TopCauses } from './components/TopCauses'
import { TrendChart } from './components/TrendChart'
import type { Cluster, Company, Dashboard, Filters } from './types'
import './App.css'

export default function App() {
  const [page, setPage] = useState<Page>('overblik')
  const [filters, setFilters] = useState<Filters>({ companyId: '', days: 30, search: '' })
  const [companies, setCompanies] = useState<Company[]>([])
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [openId, setOpenId] = useState<string | null>(null)

  useEffect(() => { getCompanies().then(setCompanies) }, [])
  useEffect(() => {
    getDashboard(filters).then(setDashboard)
    getClusters(filters).then(setClusters)
  }, [filters])

  const opened = clusters.find((c) => c.id === openId)
  const open = (id: string) => { setOpenId(id); setPage('aarsager') }

  return (
    <div className="page">
      <Header page={page} onPage={(p) => { setPage(p); setOpenId(null) }} onRun={() => startRun()} />
      <FilterBar companies={companies} value={filters} onApply={setFilters} />

      {page === 'overblik' && dashboard && (
        <>
          <KpiCards d={dashboard} />
          {dashboard.lowData && (
            <div className="card dashed warning">! Lavt datagrundlag i valgt periode – tolk tallene varsomt.</div>
          )}
          <div className="two">
            <TopCauses causes={dashboard.topCauses} onOpen={open} />
            <TrendChart points={dashboard.timeline} />
          </div>
        </>
      )}

      {page === 'aarsager' && (opened
        ? <ClusterView cluster={opened} onBack={() => setOpenId(null)} />
        : <CauseTable clusters={clusters} onOpen={setOpenId} />)}

      {page === 'data' && <section className="card"><h2>Data</h2><p className="muted">Kommer senere.</p></section>}
    </div>
  )
}
