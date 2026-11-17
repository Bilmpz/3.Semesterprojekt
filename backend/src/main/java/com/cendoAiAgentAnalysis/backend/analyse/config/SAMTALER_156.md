# Kun de 156 train-viderestillinger

Standardkonfigurationerne bruger nu samtaler_156_pipeline.xlsx. Den indeholder præcis
de samme 156 train-viderestillinger som originalens Model Input-fane. ID'er og
demovirksomheder er bevaret fra 500-importen. Kør lav_train156.py for at genskabe filen
direkte fra backend/data/syntetisk_voice_agent_dataset_analyseklart_500.xlsx.
Generatoren kræver Python og openpyxl; Java-importen læser den færdige Excel-fil direkte.
Navnene stammer fra kundereferencer på https://cendo.ai/ og bruges kun som demotilknytninger.
De historiske ID-præfikser samtaler500- og demo500- bevares, så genimport bruger samme noder.
Alle 156 er viderestillinger. Originalens validation- og testdata indgår ikke.

## Import i VS Code
Stop den gamle backend før oprydning i databasen. Brug derefter launch-konfigurationen
"Backend – importer 156 samtaler" med profilen demo156 og arbejdsmappe backend.
Denne profil importerer 156, analyserer dem og gemmer resultater. Natjobbet er slået fra
under denne kørsel. Importen sletter ikke tidligere data.

Backendens log skal først vise 156 demosamtaler gemt. Antallet af vellykkede analyser
kan være mindre, hvis modelsvar bliver afvist. Modellerne genbruger eksisterende cache.
Tidspunktet er første importtidspunkt; virksomhedstilknytningerne er syntetiske.

## Oprydning af den fælles database
Koordiner med gruppen først. Disse forespørgsler sletter ALLE samtaler og clusters,
også gruppemedlemmers data. Virksomheder, kunder og AI-agenter bevares.
Kør i Neo4j Query mens backend er stoppet:

```cypher
MATCH (s:Samtale) DETACH DELETE s;
```

```cypher
MATCH (c:Cluster) DETACH DELETE c;
```

Start derefter demo156-importen i VS Code.

Kontrollér resultatet:

```cypher
MATCH (s:Samtale)
RETURN count(s) AS alle,
       count(CASE WHEN s.viderestillet THEN 1 END) AS viderestillinger;
```

Efter en ren import forventes 156 og 156. Kontrollér også dashboard med alle virksomheder,
de seneste 30 dage og tom søgning. Brug demo156 til import. Lokal cache genbruges og
er udeladt fra Git, så modelsvar ikke bliver en del af versionshistorikken.
