import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportAdmissionGroups {
    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();
    private static final Path TSV_PATH = PROJECT_ROOT.resolve("tools").resolve("admission_groups_import.tsv");
    private static final Path SCHEMA_PATH = PROJECT_ROOT.resolve("src").resolve("main").resolve("resources").resolve("db").resolve("schema.sql");
    private static final String URL = "jdbc:h2:file:./data/gaokao_recommend_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            connection.setAutoCommit(false);
            runSchema(connection);
            ImportStats stats = importRows(connection);
            connection.commit();
            System.out.println(stats.toJson());
        }
    }

    private static void runSchema(Connection connection) throws IOException, SQLException {
        String schema = new String(Files.readAllBytes(SCHEMA_PATH), StandardCharsets.UTF_8);
        for (String sql : splitSql(schema)) {
            String statement = sql.trim();
            if (statement.isEmpty()) {
                continue;
            }
            try (Statement st = connection.createStatement()) {
                st.execute(statement);
            } catch (SQLException ex) {
                String upper = statement.toUpperCase();
                if (upper.startsWith("ALTER TABLE PREDICTION_LINE ADD COLUMN IF NOT EXISTS FILING_RANK")) {
                    continue;
                }
                throw ex;
            }
        }
    }

    private static List<String> splitSql(String sql) {
        List<String> statements = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                inString = !inString;
            }
            if (ch == ';' && !inString) {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            statements.add(current.toString());
        }
        return statements;
    }

    private static ImportStats importRows(Connection connection) throws Exception {
        ImportStats stats = new ImportStats();
        clearPreviousExcelImport(connection);
        try (BufferedReader reader = Files.newBufferedReader(TSV_PATH, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return stats;
            }
            String[] headers = headerLine.split("\t", -1);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\t", -1);
                Map<String, String> row = new HashMap<String, String>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], i < values.length ? values[i] : "");
                }
                upsertAdmissionGroup(connection, row, stats);
                if (hasText(row.get("group_min_score"))) {
                    upsertPredictionLine(connection, row, stats);
                } else {
                    stats.skippedPredictionRows++;
                }
                stats.totalRows++;
            }
        }
        return stats;
    }

    private static void clearPreviousExcelImport(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM admission_major_group WHERE source_file IN (?, ?)")) {
            ps.setString(1, "25年物理类院校专业组汇总.xlsx");
            ps.setString(2, "25年历史类院校专业组汇总_含专科批.xlsx");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM prediction_line WHERE confidence=?")) {
            ps.setString(1, "按2025投档线");
            ps.executeUpdate();
        }
    }

    private static void upsertAdmissionGroup(Connection connection, Map<String, String> row, ImportStats stats) throws SQLException {
        Long id = findAdmissionGroupId(connection, row);
        if (id == null) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO admission_major_group(" +
                            "source_subject, source_row_no, school_code, school_name, subject_type, batch_name, major_group, elective_subject, " +
                            "group_min_score, min_rank, majors, major_count, pdf_page, book_page, review_note, raw_group_line, source_file" +
                            ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                fillAdmissionStatement(ps, row, null);
                ps.executeUpdate();
                stats.insertedAdmissionRows++;
            }
        } else {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE admission_major_group SET " +
                            "source_subject=?, source_row_no=?, school_code=?, school_name=?, subject_type=?, batch_name=?, major_group=?, elective_subject=?, " +
                            "group_min_score=?, min_rank=?, majors=?, major_count=?, pdf_page=?, book_page=?, review_note=?, raw_group_line=?, source_file=?, " +
                            "import_time=CURRENT_TIMESTAMP WHERE id=?")) {
                fillAdmissionStatement(ps, row, id);
                ps.executeUpdate();
                stats.updatedAdmissionRows++;
            }
        }
    }

    private static void fillAdmissionStatement(PreparedStatement ps, Map<String, String> row, Long id) throws SQLException {
        ps.setString(1, row.get("source_subject"));
        setIntOrNull(ps, 2, row.get("source_row_no"));
        ps.setString(3, row.get("school_code"));
        ps.setString(4, row.get("school_name"));
        ps.setString(5, row.get("subject_type"));
        ps.setString(6, row.get("batch_name"));
        ps.setString(7, row.get("major_group"));
        ps.setString(8, row.get("elective_subject"));
        setIntOrNull(ps, 9, row.get("group_min_score"));
        setIntOrNull(ps, 10, row.get("min_rank"));
        ps.setString(11, row.get("majors"));
        setIntOrNull(ps, 12, row.get("major_count"));
        ps.setString(13, row.get("pdf_page"));
        ps.setString(14, row.get("book_page"));
        ps.setString(15, row.get("review_note"));
        ps.setString(16, row.get("raw_group_line"));
        ps.setString(17, row.get("source_file"));
        if (id != null) {
            ps.setLong(18, id);
        }
    }

    private static Long findAdmissionGroupId(Connection connection, Map<String, String> row) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM admission_major_group WHERE source_subject=? AND source_file=? AND source_row_no=? LIMIT 1")) {
            ps.setString(1, row.get("source_subject"));
            ps.setString(2, row.get("source_file"));
            setIntOrNull(ps, 3, row.get("source_row_no"));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static void upsertPredictionLine(Connection connection, Map<String, String> row, ImportStats stats) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO prediction_line(" +
                        "predict_year, province, school_code, school_name, subject_type, major_group, major_group_full, " +
                        "school_level, filing_score, filing_rank, predict_score, predict_low, predict_high, predict_range, " +
                        "major_direction, major_category, school_province, confidence" +
                        ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, 2026);
            ps.setString(2, "河南");
            ps.setString(3, row.get("school_code"));
            ps.setString(4, row.get("school_name"));
            ps.setString(5, row.get("subject_type"));
            ps.setString(6, row.get("major_group"));
            ps.setString(7, row.get("major_group_full"));
            ps.setString(8, row.get("school_level"));
            setIntOrNull(ps, 9, row.get("group_min_score"));
            setIntOrNull(ps, 10, row.get("min_rank"));
            ps.setInt(11, parseInt(row.get("predict_score"), parseInt(row.get("group_min_score"), 0)));
            ps.setInt(12, parseInt(row.get("predict_low"), parseInt(row.get("group_min_score"), 0)));
            ps.setInt(13, parseInt(row.get("predict_high"), parseInt(row.get("group_min_score"), 0)));
            ps.setString(14, row.get("predict_range"));
            ps.setString(15, row.get("majors"));
            ps.setString(16, row.get("major_category"));
            ps.setString(17, row.get("school_province"));
            ps.setString(18, row.get("confidence"));
            ps.executeUpdate();
            stats.insertedPredictionRows++;
        }
    }

    private static Long findPredictionLineId(Connection connection, Map<String, String> row) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM prediction_line WHERE predict_year=2026 AND province='河南' AND school_code=? AND school_name=? " +
                        "AND subject_type=? AND school_level=? AND major_group=? AND major_group_full=? LIMIT 1")) {
            ps.setString(1, row.get("school_code"));
            ps.setString(2, row.get("school_name"));
            ps.setString(3, row.get("subject_type"));
            ps.setString(4, row.get("school_level"));
            ps.setString(5, row.get("major_group"));
            ps.setString(6, row.get("major_group_full"));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static void setIntOrNull(PreparedStatement ps, int index, String value) throws SQLException {
        if (hasText(value)) {
            ps.setInt(index, parseInt(value, 0));
        } else {
            ps.setNull(index, java.sql.Types.INTEGER);
        }
    }

    private static int parseInt(String value, int fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class ImportStats {
        int totalRows;
        int insertedAdmissionRows;
        int updatedAdmissionRows;
        int insertedPredictionRows;
        int updatedPredictionRows;
        int skippedPredictionRows;

        String toJson() {
            return "{\n" +
                    "  \"totalRows\": " + totalRows + ",\n" +
                    "  \"insertedAdmissionRows\": " + insertedAdmissionRows + ",\n" +
                    "  \"updatedAdmissionRows\": " + updatedAdmissionRows + ",\n" +
                    "  \"insertedPredictionRows\": " + insertedPredictionRows + ",\n" +
                    "  \"updatedPredictionRows\": " + updatedPredictionRows + ",\n" +
                    "  \"skippedPredictionRows\": " + skippedPredictionRows + "\n" +
                    "}";
        }
    }
}
