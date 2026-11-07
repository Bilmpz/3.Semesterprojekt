# Web-API

Alle endpoints starter med `/api` og svarer med JSON.
Ved fejl svarer alle med `{ "fejl": "..." }` og den rigtige statuskode (se `FejlHaandtering`).

## W2 · Clusters (UC2)

### GET /api/clusters

Alle årsager (clusters) for en virksomhed i en periode, største først.

| Parameter | Standard | Betydning |
|---|---|---|
| `companyId` | `""` | Virksomhedens id. Tom = alle virksomheder |
| `days` | `30` | Perioden: de sidste X dage. Skal være mindst 1 |
| `search` | `""` | Kun clusters, hvis navn indeholder teksten (store/små bogstaver er ligegyldige) |

**Felter i svaret:**
- `count`: antal samtaler i clusteret i perioden
- `share`: andel af alle viderestillinger i perioden, i hele procent
- `trend`: `"up"` / `"down"` / `"flat"` – anden halvdel af perioden mod første halvdel (±10 %)

**Eksempel:** `GET /api/clusters?companyId=&days=60&search=`
```json
[
  { "id": "test-c0", "title": "Ombooking af tid", "description": "Kunden vil flytte en tid",
    "count": 4, "share": 17, "trend": "flat" },
  { "id": "test-c1", "title": "Reservedele", "description": "Kunden vil bestille dele",
    "count": 3, "share": 13, "trend": "up" }
]
```

**Fejl:** `days` < 1 → 400

### GET /api/clusters/{id}/samtaler

Samtalerne i ét cluster ("Åbn →"), nyeste først. Teksten er altid anonymiseret.

**Eksempel:** `GET /api/clusters/test-c1/samtaler`
```json
[
  { "id": "T-4", "timestamp": "2026-10-21T14:00:00", "company": "Testværksted",
    "transcript": "...", "reason": "Mangler del", "explanation": "..." }
]
```

**Fejl:** ukendt cluster-id → 404 `{ "fejl": "Clusteret med id ... findes ikke" }`