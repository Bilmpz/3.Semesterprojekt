// Samme felter som backendens DTO'er (backend/.../dto)

export type Company = { id: string; name: string }

export type Cause = { id: string; title: string; count: number }

export type DayPoint = { date: string; conversations: number; transfers: number }

/** DashboardDto – GET /api/dashboard */
export type Dashboard = {
  totalConversations: number
  transfers: number
  transferRate: number
  causesFound: number
  lowData: boolean
  topCauses: Cause[]
  timeline: DayPoint[]
}

/** ClusterDto – GET /api/clusters */
export type Cluster = {
  id: string
  title: string
  description: string
  count: number
  share: number
  trend: 'up' | 'down' | 'flat'
}

/** ConversationDto – GET /api/clusters/{id}/conversations */
export type Conversation = {
  id: string
  timestamp: string
  company: string
  transcript: string
  reason: string
  explanation: string
}

export type Filters = { companyId: string; days: number; search: string }
