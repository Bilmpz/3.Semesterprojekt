# E2b · Svartidstest med 10.000 samtaler (NFR3)

**Krav:** dashboardet svarer på under 5 sekunder (designmål 4 / NFR3).
**Resultat:** alle endpoints er under 1 sekund efter optimering. Langsomste måling: 853 ms.

## Opsætning

- Lokal Neo4j (Neo4j Desktop), **ikke** den fælles Aura-database
- 3 virksomheder, 15 kunder, 3 agenter, 20 clusters, **10.000 samtaler** over 90 dage, ca. 10 % støj
- Hvert endpoint kaldt **10 gange** med PowerShell `Measure-Command`, backend på `localhost:8081`

## Resultater (snit / langsomste, i ms)

| Endpoint | Før | Efter indeks | Efter kodeændring | < 5 sek. |
|---|---|---|---|---|
| `/api/virksomheder` | 121 / 850 | 93 / 673 | 128 / 846 | ✅ |
| `/api/overblik?days=90` | 470 / 3.231 | 406 / 2.719 | 174 / 659 | ✅ |
| `/api/clusters?days=90` | 193 / 604 | 212 / 781 | 222 / 663 | ✅ |
| `/api/clusters/{id}/samtaler` | **15.896 / 21.664** | **9.431 / 11.574** | **347 / 853** | ✅ |
| `/api/samtaler/{id}` | 216 / 1.669 | 56 / 296 | 39 / 130 | ✅ |

Den langsomste måling er typisk den første, mens Java "varmer op".

## Hvad vi fandt og rettede

1. **N+1-problem i `/api/clusters/{id}/samtaler`.** Virksomhedens navn blev slået op med én forespørgsel pr. samtale. Det største cluster (~2.000 samtaler) gav ~2.000 ekstra forespørgsler og 16 sekunders svartid.
2. **Indekser** på `id` for alle nodetyper og på `Samtale.tidspunkt`. Hvert opslag blev hurtigere, og svartiden faldt til 9,4 s, men stadig over kravet.
3. **Én samlet Cypher-forespørgsel** (`hentSamtalerICluster` i `ClusterStatistik`), der henter samtalerne og virksomhedens navn på én gang. Svartiden faldt fra 15,9 s til 0,35 s.

## Sådan gentager du testen

### 1. Testdata (Neo4j Browser, kør én ad gangen)

```cypher
UNWIND range(1, 3) AS v
CREATE (vir:Virksomhed {id: 'perf-v' + v, navn: 'Perf virksomhed ' + v})
CREATE (agent:AiAgent:SamtaleDeltager {id: 'perf-a' + v, navn: 'Perf agent ' + v})
CREATE (vir)-[:HAR_AI_AGENT]->(agent)
WITH vir, v
UNWIND range(1, 5) AS k
CREATE (kunde:Kunde:SamtaleDeltager {id: 'perf-k' + v + '-' + k, navn: 'Perf kunde ' + v + '-' + k})
CREATE (vir)-[:HAR_KUNDE]->(kunde)
```

```cypher
UNWIND range(1, 20) AS c
CREATE (:Cluster {id: 'perf-c' + c, navn: 'Perf årsag ' + c,
                  beskrivelse: 'Testcluster til svartidstest', stoerrelse: 0, erNy: false})
```

```cypher
MATCH (k:Kunde) WHERE k.id STARTS WITH 'perf-'
WITH collect(k) AS kunder
MATCH (c:Cluster) WHERE c.id STARTS WITH 'perf-'
WITH kunder, collect(c) AS clusters
UNWIND range(1, 10000) AS i
WITH clusters, i, kunder[toInteger(rand() * size(kunder))] AS kunde
MATCH (vir:Virksomhed)-[:HAR_KUNDE]->(kunde)
MATCH (vir)-[:HAR_AI_AGENT]->(agent:AiAgent)
CREATE (s:Samtale {
  id: 'perf-s' + i,
  viderestillingsaarsag: 'Testårsag',
  forklaring: 'Testforklaring til svartidstest',
  status: 'OK',
  transcript: 'Kunde: Hej, jeg ringer om min bil. Agent: Jeg stiller dig videre til en medarbejder.',
  tidspunkt: localdatetime() - duration({days: toInteger(rand() * 90), hours: toInteger(rand() * 24)})
})
CREATE (kunde)-[:HAR]->(s)
CREATE (agent)-[:HAANDTERER]->(s)
WITH s, clusters
WHERE rand() > 0.1
WITH s, clusters[toInteger(rand() * rand() * size(clusters))] AS c
CREATE (s)-[:TILHOERER]->(c)
```

### 2. Indekser

```cypher
CREATE CONSTRAINT samtale_id IF NOT EXISTS FOR (s:Samtale) REQUIRE s.id IS UNIQUE;
CREATE CONSTRAINT cluster_id IF NOT EXISTS FOR (c:Cluster) REQUIRE c.id IS UNIQUE;
CREATE CONSTRAINT kunde_id IF NOT EXISTS FOR (k:Kunde) REQUIRE k.id IS UNIQUE;
CREATE CONSTRAINT agent_id IF NOT EXISTS FOR (a:AiAgent) REQUIRE a.id IS UNIQUE;
CREATE CONSTRAINT virksomhed_id IF NOT EXISTS FOR (v:Virksomhed) REQUIRE v.id IS UNIQUE;
CREATE INDEX samtale_tidspunkt IF NOT EXISTS FOR (s:Samtale) ON (s.tidspunkt);
```

### 3. Måling (PowerShell, backend skal køre)

```powershell
$base = "http://localhost:8081"
$endpoints = @(
  "/api/virksomheder",
  "/api/overblik?companyId=&days=90&search=",
  "/api/clusters?companyId=&days=90&search=",
  "/api/clusters/perf-c1/samtaler",
  "/api/samtaler/perf-s1"
)
foreach ($e in $endpoints) {
  $tider = 1..10 | ForEach-Object {
    (Measure-Command { Invoke-RestMethod "$base$e" | Out-Null }).TotalMilliseconds
  }
  $snit = ($tider | Measure-Object -Average).Average
  $max  = ($tider | Measure-Object -Maximum).Maximum
  "{0,-45} snit {1,8:N0} ms   max {2,8:N0} ms" -f $e, $snit, $max
}
```

### 4. Oprydning

```cypher
MATCH (n) WHERE n.id STARTS WITH 'perf-' DETACH DELETE n
```