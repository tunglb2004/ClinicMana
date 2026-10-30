import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqlRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java SqlRunner <jdbcUrl> <user> <password> <sqlFile|--query> [query]");
            System.exit(1);
        }

        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String modeOrFile = args[3];

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);
            if ("--query".equals(modeOrFile)) {
                if (args.length < 5) {
                    throw new IllegalArgumentException("Missing query text.");
                }
                runQuery(connection, args[4]);
            } else {
                runSqlFile(connection, Path.of(modeOrFile));
            }
            connection.commit();
        }
    }

    private static void runSqlFile(Connection connection, Path path) throws IOException, SQLException {
        String sql = Files.readString(path, StandardCharsets.UTF_8);
        List<String> statements = splitStatements(sql);
        int executed = 0;
        try (Statement statement = connection.createStatement()) {
            for (String rawStatement : statements) {
                String query = rawStatement.trim();
                if (query.isEmpty()) {
                    continue;
                }
                try {
                    boolean hasResultSet = statement.execute(query);
                    if (hasResultSet) {
                        printResultSet(statement.getResultSet());
                    }
                    executed++;
                } catch (SQLException exception) {
                    System.err.println("Failed statement #" + (executed + 1) + ":");
                    System.err.println(query);
                    throw exception;
                }
            }
        }
        System.out.println("Executed statements: " + executed);
    }

    private static void runQuery(Connection connection, String query) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(query);
            if (hasResultSet) {
                printResultSet(statement.getResultSet());
            } else {
                System.out.println("Update count: " + statement.getUpdateCount());
            }
        }
    }

    private static void printResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData meta = resultSet.getMetaData();
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                System.out.print("\t");
            }
            System.out.print(meta.getColumnLabel(i));
        }
        System.out.println();

        while (resultSet.next()) {
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    System.out.print("\t");
                }
                Object value = resultSet.getObject(i);
                System.out.print(value == null ? "NULL" : value.toString());
            }
            System.out.println();
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (lineComment) {
                if (c == '\n' || c == '\r') {
                    lineComment = false;
                    current.append(c);
                }
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (!singleQuote && !doubleQuote && c == '-' && next == '-') {
                lineComment = true;
                i++;
                continue;
            }
            if (!singleQuote && !doubleQuote && c == '/' && next == '*') {
                blockComment = true;
                i++;
                continue;
            }
            if (c == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
            } else if (c == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
            }

            if (c == ';' && !singleQuote && !doubleQuote) {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            statements.add(current.toString());
        }
        return statements;
    }
}
