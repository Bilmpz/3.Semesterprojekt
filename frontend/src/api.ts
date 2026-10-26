// ALLE kald til backend. Sæt USE_MOCK = false, når backend er klar.
import * as mock from './mockData'
import type { Cluster, Company, Conversation, Dashboard, Filters } from './types'

const USE_MOCK = true

async function get<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Fejl ${res.status}`)
  return res.json()
}

const q = (f: Filters) => `companyId=${f.companyId}&days=${f.days}&search=${encodeURIComponent(f.search)}`

export const getCompanies = (): Promise<Company[]> =>
  USE_MOCK ? Promise.resolve(mock.companies) : get('/api/kunder')

export const getDashboard = (f: Filters): Promise<Dashboard> =>
  USE_MOCK ? Promise.resolve(mock.dashboard) : get(`/api/overblik?${q(f)}`)

export const getClusters = (f: Filters): Promise<Cluster[]> =>
  USE_MOCK
    ? Promise.resolve(mock.clusters.filter((c) => c.title.toLowerCase().includes(f.search.toLowerCase())))
    : get(`/api/clusters?${q(f)}`)

export const getConversations = (clusterId: string): Promise<Conversation[]> =>
  USE_MOCK ? Promise.resolve(mock.conversations) : get(`/api/clusters/${clusterId}/samtaler`)

export const startRun = (): Promise<unknown> =>
  USE_MOCK ? Promise.resolve({}) : fetch('/api/pipeline/run', { method: 'POST' }).then((r) => r.json())
