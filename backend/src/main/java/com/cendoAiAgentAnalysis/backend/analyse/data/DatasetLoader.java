package com.cendoAiAgentAnalysis.backend.analyse.data;

import com.cendoAiAgentAnalysis.backend.analyse.model.Conversation;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Cendo's .xlsx dataset.
 *
 * The pipeline itself only reads the "Model Input" sheet. The "Labels" sheet (the answer key) is read
 * separately and only used by the evaluation step after clustering, so the models never see it.
 */
public final class DatasetLoader {

    private static final String MODEL_INPUT_SHEET = "Model Input";
    private static final String LABELS_SHEET = "Labels";
    private static final DataFormatter FORMATTER = new DataFormatter();

    private DatasetLoader() {
    }

    /** Loads every conversation where the agent called the transfer tool, filtered by split ("all" = no filter). */
    public static List<Conversation> loadTransfers(Path xlsx, String split) throws IOException {
        List<Conversation> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(xlsx.toFile(), null, true)) {
            Sheet sheet = requireSheet(workbook, MODEL_INPUT_SHEET);
            Map<String, Integer> columns = headerIndex(sheet);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String id = cell(row, columns, "conversation_id");
                if (id.isEmpty()) {
                    continue;
                }
                String rowSplit = cell(row, columns, "dataset_split");
                if (!split.equals("all") && !split.equalsIgnoreCase(rowSplit)) {
                    continue;
                }
                if (!calledTransfer(cell(row, columns, "tools_called"))) {
                    continue;
                }
                String transcript = TranscriptSanitizer.sanitize(cell(row, columns, "synthetic_transcript"));
                result.add(new Conversation(id, rowSplit, transcript));
            }
        }
        return result;
    }

    /** Loads the answer key: conversation_id -> transfer_classification. Only for evaluation. */
    public static Map<String, String> loadTransferLabels(Path xlsx) throws IOException {
        Map<String, String> labels = new HashMap<>();
        try (Workbook workbook = WorkbookFactory.create(xlsx.toFile(), null, true)) {
            Sheet sheet = requireSheet(workbook, LABELS_SHEET);
            Map<String, Integer> columns = headerIndex(sheet);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String id = cell(row, columns, "conversation_id");
                if (!id.isEmpty()) {
                    labels.put(id, cell(row, columns, "transfer_classification"));
                }
            }
        }
        return labels;
    }

    private static boolean calledTransfer(String toolsCalled) {
        // tools_called looks like "Nummerplade; AddServiceInfo; transfer" (or is empty)
        return Arrays.stream(toolsCalled.split(";"))
                .map(String::strip)
                .anyMatch(tool -> tool.equals("transfer"));
    }

    private static Sheet requireSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new IllegalStateException("Fanen '" + name + "' findes ikke i datasættet.");
        }
        return sheet;
    }

    private static Map<String, Integer> headerIndex(Sheet sheet) {
        Map<String, Integer> index = new LinkedHashMap<>();
        Row header = sheet.getRow(0);
        for (Cell c : header) {
            index.put(FORMATTER.formatCellValue(c).strip(), c.getColumnIndex());
        }
        return index;
    }

    private static String cell(Row row, Map<String, Integer> columns, String name) {
        Integer col = columns.get(name);
        if (col == null) {
            throw new IllegalStateException("Kolonnen '" + name + "' findes ikke i datasættet.");
        }
        Cell c = row.getCell(col);
        return c == null ? "" : FORMATTER.formatCellValue(c).strip();
    }
}
