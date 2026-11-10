
import type {
  Cluster,
  Company,
  Conversation,
  Dashboard,
  Filters
} from './types'

// Alle API-kald til Spring Boot samles i denne fil.
// Dashboardet bruger rigtige data i stedet for mock-data.
const USE_MOCK = false

// Fælles funktion til at hente JSON fra backend.
// T angiver den datatype, som API'et forventes at returnere.
async function get<T>(url: string): Promise<T> {
  let response: Response

  try {
    response = await fetch(url, {
      headers: {
        Accept: 'application/json'
      }
    })
  } catch {
    // Håndterer situationen, hvor backend ikke kan kontaktes.
    throw new Error(
        'Kunne ikke kontakte backend. Kontrollér at Spring Boot kører.'
    )
  }

  // Kontrollerer om API'et returnerede en fejl.
  if (!response.ok) {
    throw new Error(
        `API-fejl ${response.status} ved ${url}`
    )
  }

  // Konverterer JSON-svaret til den forventede datatype.
  return (await response.json()) as T
}

// Omdanner de valgte filtre til URL-parametre.
// URLSearchParams sørger for korrekt håndtering af specialtegn.
function query(filters: Filters): string {
  return new URLSearchParams({
    companyId: filters.companyId,
    days: String(filters.days),
    search: filters.search
  }).toString()
}

// Henter alle kunder til dashboardets virksomhedsfilter.
export function getCompanies(): Promise<Company[]> {
  return get<Company[]>('/api/virksomheder')
}

// Henter dashboardets nøgletal, topårsager og tidslinje.
export function getDashboard(
    filters: Filters
): Promise<Dashboard> {
  return get<Dashboard>(
      `/api/overblik?${query(filters)}`
  )
}

// Henter clusters ud fra virksomhed, periode og søgetekst.
export function getClusters(
    filters: Filters
): Promise<Cluster[]> {
  return get<Cluster[]>(
      `/api/clusters?${query(filters)}`
  )
}

// Henter alle samtaler, der tilhører et bestemt cluster.
export function getConversations(
    clusterId: string
): Promise<Conversation[]> {
  return get<Conversation[]>(
      `/api/clusters/${encodeURIComponent(clusterId)}/samtaler`
  )
}

// Henter en enkelt samtale ud fra dens ID.
// Funktionen kan bruges, hvis samtaler senere åbnes separat.
export function getConversation(
    conversationId: string
): Promise<Conversation> {
  return get<Conversation>(
      `/api/samtaler/${encodeURIComponent(conversationId)}`
  )
}

// Starter en manuel pipelinekørsel.
// Endpointet er bevaret fra den eksisterende frontend.
// Det kræver, at backend understøtter POST /api/pipeline/run.
export async function startRun(): Promise<unknown> {
  let response: Response

  try {
    response = await fetch('/api/pipeline/run', {
      method: 'POST'
    })
  } catch {
    throw new Error(
        'Kunne ikke kontakte backend for at starte pipelinen.'
    )
  }

  // Kontrollerer at backend accepterede forespørgslen.
  if (!response.ok) {
    throw new Error(
        `Kunne ikke starte pipelinen. HTTP ${response.status}`
    )
  }

  // Et endpoint kan returnere JSON eller et tomt svar.
  const body = await response.text()

  return body ? JSON.parse(body) as unknown : null
}

// Markerer at mock-data er deaktiveret.
// Konstanten bevares, så indstillingen er tydelig.
void USE_MOCK
