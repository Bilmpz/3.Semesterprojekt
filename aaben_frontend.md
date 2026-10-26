# Sådan starter du projektet (P3 – Cendo)

**v1.1 · 26/10-2026**

## Overblik

Projektet har to dele, der ligger ved siden af hinanden:

- `frontend/` – React-app (TypeScript) bygget med Vite. Kører på port **5173**.
- `backend/` – Spring Boot (Java). Kører på port **8080** og taler med Neo4j.

I åbner altid **http://localhost:5173** i browseren. Alle kald, der starter med `/api`, sender Vite automatisk videre til Spring Boot, så I slipper for CORS-opsætning.

```
Browser ──► Vite (localhost:5173) ──/api/...──► Spring Boot (localhost:8080) ──► Neo4j
```

> **Frontend virker også uden backend.** Den bruger falske data fra `frontend/src/mockData.ts`, så længe `USE_MOCK = true` i `frontend/src/api.ts`. Arbejder du kun på frontend, behøver du ikke starte backend.

## Det skal du have installeret

| Program | Version | Tjek i terminalen |
| --- | --- | --- |
| Node.js (inkl. npm) | 20.19 eller nyere / 22.12 eller nyere | `node -v` |
| Java (JDK) | 21 eller nyere | `java -version` |
| Git | Vilkårlig | `git --version` |

Node hentes på [nodejs.org](https://nodejs.org) (vælg LTS). Maven skal ikke installeres, for projektet har sin egen `mvnw`.

## Første gang

### 1. Hent koden

Har du ikke repoet endnu, så klon det:

```bash
git clone https://github.com/Bilmpz/3.Semesterprojekt.git
cd 3.Semesterprojekt
```

Har du det allerede, så hent seneste `main`:

```bash
git pull
```

### 2. Opret `backend/.env`

Filen er ikke i Git, fordi den indeholder kodeordet til Neo4j. Opret den i mappen `backend/` med præcis disse tre linjer, og indsæt de rigtige værdier (få dem af den i gruppen, der har Neo4j-adgangen). Ingen mellemrum omkring `=`.

```
NEO4J_URI=neo4j+s://XXXXXXXX.databases.neo4j.io
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=INDSÆT_KODE
```

På Windows kan du oprette filen sådan og så rette værdierne i Notesblok:

```powershell
cd backend
@"
NEO4J_URI=neo4j+s://XXXXXXXX.databases.neo4j.io
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=INDSÆT_KODE
"@ | Set-Content -Encoding ascii .env
notepad .env
```

### 3. Installer frontend-pakkerne

`npm install` skal køres **inde i `frontend/`** – ikke i roden af repoet:

```bash
cd frontend
npm install
```

Kør `npm install` igen, hver gang `package.json` er ændret efter et `git pull`.

## Start projektet (hver gang)

Du skal bruge to terminaler. Begge kommandoer skal køres **inde i den rigtige mappe** (`backend/` eller `frontend/`), ikke i roden. Start backend først.

**Terminal 1 – backend** (Mac/Linux):

```bash
cd backend
./mvnw spring-boot:run
```

På Windows (PowerShell):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Vent til loggen siger `Tomcat started on port 8080`.

**Terminal 2 – frontend:**

```bash
cd frontend
npm run dev
```

Åbn **http://localhost:5173**. Ændringer i React-koden vises med det samme uden genstart.

**Test at de to taler sammen:** åbn http://localhost:5173/api/health. Viser den `{"status":"ok"}`, virker hele kæden.

Stop serverne med `Ctrl+C` i hver terminal.

## Hvor ligger hvad i frontend?

| Fil | Hvad |
| --- | --- |
| `src/App.tsx` | Hele siden: header, filtre og skift mellem Overblik / Årsager / Data |
| `src/components/` | Delene af siden: `KpiCards`, `TopCauses`, `TrendChart`, `CauseTable`, `ClusterView` … |
| `src/api.ts` | **Det eneste sted** frontend kalder backend. `USE_MOCK = true` → falske data |
| `src/mockData.ts` | De falske data (virksomheder, årsager, samtaler) |
| `src/types.ts` | Felterne i data – skal matche backendens DTO'er |

Når backend-endpoints er klar, sættes `USE_MOCK = false` i `src/api.ts`. Så henter frontend rigtige data.

## Sådan kalder du backend fra React

Brug altid en relativ sti, der starter med `/api`. Skriv aldrig `http://localhost:8080` i frontend-koden. Læg nye kald i `src/api.ts`.

```ts
const res = await fetch('/api/health')
const data = await res.json()
console.log(data.status) // "ok"
```

I backend skal alle controllere derfor ligge under `/api`, som i `HealthController.java`:

```java
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
```

## Fejlfinding

| Du ser | Årsag | Løsning |
| --- | --- | --- |
| `npm error Missing script: "dev"` | Du står i roden af repoet | `cd frontend` og kør igen |
| `.\mvnw.cmd : The term ... is not recognized` | Du står ikke i `backend/` | `cd backend` og kør igen |
| Der er kommet en `node_modules` eller `package-lock.json` i roden | `npm install` er kørt i roden | Slet dem i roden – de hører kun til i `frontend/` |
| `Invalid URI syntax ... ${NEO4J_URI}` når backend starter | `backend/.env` mangler eller er stavet forkert | Opret filen som i "Første gang", trin 2 |
| `/api/...` giver fejl 502, og Vite-terminalen skriver `http proxy error` | Backend kører ikke (eller `USE_MOCK = false` for tidligt) | Start backend i terminal 1, eller sæt `USE_MOCK = true` |
| `npm: command not found` | Node er ikke installeret | Installer Node fra nodejs.org og åbn en ny terminal |
| Vite klager over Node-versionen | Node er for gammel | Opdater til Node 22 LTS |
| `permission denied: ./mvnw` | Filen mangler kørselsret (Mac/Linux) | Kør `chmod +x mvnw` i `backend/` |
| `Port 8080 was already in use` | En backend kører allerede | Stop den gamle med `Ctrl+C` |
| Siden viser gammel kode efter `git pull` | Nye pakker er ikke installeret | Kør `npm install` i `frontend/` og start igen |
