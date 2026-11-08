
// Datatyper til frontend.
// Feltnavnene skal matche backendens JSON-svar.

// Kundeoplysninger til virksomhedsfilteret.
export type Company = {
  id: string
  name: string
}

// En viderestillingsårsag med antal samtaler.
export type Cause = {
  id: string
  title: string
  count: number
}

// Statistik for én dag i dashboardets tidslinje.
export type DayPoint = {
  date: string
  conversations: number
  transfers: number
}

// Dashboardets samlede statistik.
// Modtages fra GET /api/overblik.
export type Dashboard = {
  totalConversations: number
  transfers: number
  transferRate: number
  causesFound: number
  lowData: boolean
  topCauses: Cause[]
  timeline: DayPoint[]
}

// Et cluster med navn, beskrivelse og statistik.
// Modtages fra GET /api/clusters.
export type Cluster = {
  id: string
  title: string
  description: string
  count: number
  share: number
  trend: 'up' | 'down' | 'flat'
}

// En konkret samtale med årsag og forklaring.
// Bruges både i clusteroversigten og ved visning
// af en enkelt samtale.
export type Conversation = {
  id: string
  timestamp: string
  company: string
  transcript: string
  reason: string
  explanation: string
}

// Filtre som sendes med API-kaldene.
// Tomt companyId betyder alle virksomheder.
export type Filters = {
  companyId: string
  days: number
  search: string
}
