// Falske data, indtil backend er klar
import type { Cluster, Company, Conversation, Dashboard } from './types'

export const companies: Company[] = [
  { id: 'v1', name: 'Værksted Nord' },
  { id: 'v2', name: 'Bilhuset Syd' },
]

export const clusters: Cluster[] = [
  { id: 'c1', title: 'Status på reparation', description: 'Kunden vil vide, hvornår bilen er klar', count: 24, share: 21, trend: 'up' },
  { id: 'c2', title: 'Spørgsmål om garanti', description: 'Er reparationen dækket af garantien?', count: 19, share: 17, trend: 'down' },
  { id: 'c3', title: 'Faktura og betaling', description: 'Spørgsmål til en faktura', count: 17, share: 15, trend: 'flat' },
  { id: 'c4', title: 'Vil tale med bestemt mekaniker', description: 'Kunden beder om en navngiven medarbejder', count: 12, share: 11, trend: 'up' },
  { id: 'c5', title: 'Dårlig forbindelse', description: 'Agenten kan ikke høre kunden', count: 9, share: 8, trend: 'down' },
  { id: 'c6', title: 'Ombooking af tid', description: 'Kunden vil flytte sin værkstedstid', count: 8, share: 7, trend: 'flat' },
  { id: 'c7', title: 'Lånebil', description: 'Spørgsmål om lånebil under reparation', count: 6, share: 5, trend: 'up' },
]

const timeline = Array.from({ length: 10 }, (_, i) => ({
  date: `2026-10-${String(i + 1).padStart(2, '0')}`,
  conversations: 40 + i * 4 + (i % 3) * 6,
  transfers: 10 + i + (i % 2) * 3,
}))

export const dashboard: Dashboard = {
  totalConversations: 500,
  transfers: 113,
  transferRate: 23,
  causesFound: clusters.length,
  lowData: false,
  topCauses: clusters.slice(0, 5).map((c) => ({ id: c.id, title: c.title, count: c.count })),
  timeline,
}

export const conversations: Conversation[] = [
  {
    id: 's1', timestamp: '2026-10-03T09:12:00Z', company: 'Værksted Nord',
    transcript: 'Agent: Hej, du taler med værkstedets assistent.\nKunde: Hej, jeg vil høre, om min bil er færdig?\nAgent: Det kan jeg desværre ikke se. Jeg stiller dig videre.',
    reason: 'Status på reparation', explanation: 'Agenten har ikke adgang til værkstedets status.',
  },
  {
    id: 's2', timestamp: '2026-10-05T14:40:00Z', company: 'Bilhuset Syd',
    transcript: 'Agent: Hej, hvad kan jeg hjælpe med?\nKunde: Hvornår kan jeg hente bilen?\nAgent: Jeg stiller dig videre til en medarbejder.',
    reason: 'Status på reparation', explanation: 'Agenten kan ikke slå reparationer op.',
  },
]
