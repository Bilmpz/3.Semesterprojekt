package com.cendoAiAgentAnalysis.backend.analyse.config;

import com.cendoAiAgentAnalysis.backend.analyse.data.TranscriptSanitizer;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/** Importerer de 156 train-viderestillinger. Virksomhederne er demotilknytninger. */
@Service
public class DemoDatasetImporter {
    private final Neo4jClient client;
    private final PipelineConfig config;

    public DemoDatasetImporter(Neo4jClient client, PipelineConfig config) {
        this.client = client;
        this.config = config;
    }

    @Transactional
    public int importer() throws IOException {
        List<Map<String, Object>> rows = readRows(config.datasetPath(), config.split());
        LocalDateTime importedAt = LocalDateTime.now(ZoneId.of("Europe/Copenhagen"));
        client.query("""
                UNWIND $rows AS row
                MERGE (v:Virksomhed {id: row.companyId})
                SET v.navn = row.companyName, v.demodata = true
                MERGE (k:Kunde {id: row.companyId + '-kunde'})
                SET k.navn = 'Demo-kunde: ' + row.companyName
                MERGE (a:AiAgent {id: row.companyId + '-agent'})
                SET a.navn = 'Demo AI-agent: ' + row.companyName
                MERGE (v)-[:HAR_KUNDE]->(k)
                MERGE (v)-[:HAR_AI_AGENT]->(a)
                MERGE (s:Samtale {id: row.id})
                ON CREATE SET s.status = CASE WHEN row.transfer THEN 'AFVENTER_ANALYSE'
                                             ELSE 'IKKE_VIDERESTILLET' END
                SET s.tidspunkt = coalesce(s.tidspunkt, $importedAt),
                    s.transcript = row.transcript,
                    s.viderestillet = row.transfer,
                    s.demodata = true,
                    s.tidspunktKilde = 'importtidspunkt',
                    s.virksomhedKilde = 'syntetisk demofordeling'
                MERGE (k)-[:HAR]->(s)
                MERGE (a)-[:HAANDTERER]->(s)
                """)
                .bind(rows).to("rows")
                .bind(importedAt).to("importedAt")
                .run();
        return rows.size();
    }

    // Valider hele filen før nogen databaseændringer.
    static List<Map<String, Object>> readRows(Path path, String split) throws IOException {
        if (!"train".equals(split)) {
            throw new IllegalArgumentException("Demoimport kræver dataset.split=train");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
            Sheet sheet = workbook.getSheet("Model Input");
            if (sheet == null || sheet.getRow(0) == null) {
                throw new IllegalArgumentException("Model Input-fanen mangler");
            }
            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : sheet.getRow(0)) {
                columns.put(formatter.formatCellValue(cell).strip(), cell.getColumnIndex());
            }
            for (String name : List.of("conversation_id", "dataset_split", "tools_called",
                    "synthetic_transcript", "company_id", "company_name")) {
                if (!columns.containsKey(name)) {
                    throw new IllegalArgumentException("Demoimport mangler kolonnen " + name);
                }
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                columns.forEach((name, index) ->
                        values.put(name, formatter.formatCellValue(row.getCell(index)).strip()));
                String id = values.get("conversation_id");
                String companyId = values.get("company_id");
                if (id == null || !id.matches("samtaler500-\\d{4}") || !ids.add(id)
                        || !companyId.startsWith("demo500-")
                        || values.get("company_name").isBlank()
                        || values.get("synthetic_transcript").isBlank()
                        || !"train".equals(values.get("dataset_split"))) {
                    throw new IllegalArgumentException("Ugyldig demorække " + (i + 1));
                }
                boolean transfer = Arrays.stream(values.get("tools_called").split(";"))
                        .map(String::strip).anyMatch("transfer"::equals);
                if (!transfer) {
                    throw new IllegalArgumentException("Trainimport kræver et transfer-kald i række " + (i + 1));
                }
                result.add(Map.of("id", id, "companyId", companyId,
                        "companyName", values.get("company_name"),
                        "transcript", TranscriptSanitizer.sanitize(values.get("synthetic_transcript")),
                        "transfer", transfer));
            }
        }
        if (result.size() != 156) {
            throw new IllegalArgumentException("Demoimport kræver præcis 156 train-viderestillinger");
        }
        return result;
    }
}
