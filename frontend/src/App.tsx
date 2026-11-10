
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

  // Holder styr på den valgte side og dashboardets filtre
  const [page, setPage] = useState<Page>('overblik')
  const [filters, setFilters] = useState<Filters>({
    companyId: '',
    days: 30,
    search: ''
  })

  // Data som hentes fra backend
  const [companies, setCompanies] = useState<Company[]>([])
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [clusters, setClusters] = useState<Cluster[]>([])

  // Holder styr på hvilket cluster der er åbnet
  const [openId, setOpenId] = useState<string | null>(null)

  // Indlæsning og fejlbeskeder fra API-kaldene
  const [loading, setLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState<string | null>(null)
  const [clustersError, setClustersError] = useState<string | null>(null)
  const [companiesError, setCompaniesError] = useState<string | null>(null)
  const [runError, setRunError] = useState<string | null>(null)

  // Henter kunder, når dashboardet åbnes
  useEffect(() => {
    let active = true

    getCompanies()
        .then((data) => {
          if (!active) return
          setCompanies(data)
          setCompaniesError(null)
        })
        .catch((error: unknown) => {
          if (!active) return

          setCompaniesError(
              error instanceof Error
                  ? error.message
                  : 'Kunne ikke hente virksomheder'
          )
        })

    // Forhindrer opdatering efter komponenten er fjernet
    return () => {
      active = false
    }
  }, [])

  // Henter dashboard og clusters, når filtrene ændres
  useEffect(() => {
    let active = true

    // Nulstiller gamle data og fejl
    setLoading(true)
    setDashboard(null)
    setClusters([])
    setOpenId(null)
    setDashboardError(null)
    setClustersError(null)

    const loadData = async () => {

      // Henter begge datasæt uafhængigt af hinanden
      const results = await Promise.allSettled([
        getDashboard(filters),
        getClusters(filters)
      ])

      // Ignorerer svar fra tidligere API-kald
      if (!active) return

      const dashboardResult = results[0]
      const clusterResult = results[1]

      // Behandler dashboardets svar
      if (dashboardResult.status === 'fulfilled') {
        setDashboard(dashboardResult.value)
      } else {
        setDashboardError(
            dashboardResult.reason instanceof Error
                ? dashboardResult.reason.message
                : 'Kunne ikke hente overbliksdata'
        )
      }

      // Behandler clusters
      if (clusterResult.status === 'fulfilled') {
        setClusters(clusterResult.value)
      } else {
        setClustersError(
            clusterResult.reason instanceof Error
                ? clusterResult.reason.message
                : 'Kunne ikke hente clusters'
        )
      }

      setLoading(false)
    }

    void loadData()

    // Undgår at gamle svar overskriver nye data
    return () => {
      active = false
    }
  }, [filters])

  // Finder det cluster brugeren har åbnet
  const opened = clusters.find((c) => c.id === openId)

  // Åbner et cluster og skifter til årsagssiden
  const open = (id: string) => {
    setOpenId(id)
    setPage('aarsager')
  }

  // Starter en manuel pipelinekørsel
  const handleRun = () => {
    setRunError(null)

    void startRun().catch((error: unknown) => {
      setRunError(
          error instanceof Error
              ? error.message
              : 'Kunne ikke starte pipelinen'
      )
    })
  }

  return (
      <div className="page">

        {/* Navigation og manuel pipelinekørsel */}
        <Header
            page={page}
            onPage={(p) => {
              setPage(p)
              setOpenId(null)
            }}
            onRun={handleRun}
        />

        {/* Filtre til virksomhed, periode og søgning */}
        <FilterBar
            companies={companies}
            value={filters}
            onApply={setFilters}
        />

        {/* Viser fejl fra kundelisten */}
        {companiesError && (
            <div className="card dashed warning">
              Kunne ikke hente virksomheder: {companiesError}
            </div>
        )}

        {/* Viser fejl ved manuel pipelinekørsel */}
        {runError && (
            <div className="card dashed warning">
              Pipelinefejl: {runError}
            </div>
        )}

        {/* Viser at dashboardet henter data */}
        {loading && (
            <div className="card">
              Henter data fra backend...
            </div>
        )}

        {/* Dashboardets overbliksside */}
        {page === 'overblik' && !loading && (
            <>
              {dashboardError && (
                  <div className="card dashed warning">
                    Kunne ikke hente overbliksdata: {dashboardError}
                  </div>
              )}

              {dashboard && (
                  <>
                    <div className="overview-grid overview-priority">
                      <TopCauses causes={dashboard.topCauses} onOpen={open} />
                      <section className="card activity-card">
                        <div className="row">
                          <div>
                            <h2>Aktivitet</h2>
                            <p className="hint">Viderestillinger for de valgte filtre</p>
                          </div>
                          <button className="soft-button" onClick={() => setPage('aarsager')}>Se clusters <span aria-hidden="true">→</span></button>
                        </div>
                        <KpiCards d={dashboard} />
                        <TrendChart points={dashboard.timeline} />
                      </section>

                    </div>

                  </>
              )}

              {clustersError ? (
                  <div className="card dashed warning" role="alert">
                    Kunne ikke hente clusters: {clustersError}
                  </div>
              ) : (
                  <CauseTable clusters={clusters} onOpen={open} />
              )}
            </>
        )}

        {/* Clusteroversigt og visning af samtaler */}
        {page === 'aarsager' && !loading && (
            <>
              {clustersError && (
                  <div className="card dashed warning">
                    Kunne ikke hente clusters: {clustersError}
                  </div>
              )}

              {!clustersError && (
                  opened
                      ? <ClusterView
                          cluster={opened}
                          onBack={() => setOpenId(null)}
                      />
                      : <CauseTable
                          clusters={clusters}
                          onOpen={setOpenId}
                      />
              )}
            </>
        )}



      </div>
  )
}
