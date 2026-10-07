package services;

import exceptions.ErrorConexionCassandraException;
import utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Simple wrapper that shells out to {@code cqlsh -e "<query>"} using a read-only Cassandra role.
 * The raw stdout/stderr are returned so the UI can mimic a terminal experience.
 */
public class CqlTerminalService {

    private static final long DEFAULT_TIMEOUT_MS = 10_000L;
    private static final String[] FORBIDDEN_KEYWORDS = {
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE",
            "CREATE", "REPLACE", "GRANT", "REVOKE", "MERGE",
            "CALL", "BEGIN", "COMMIT", "ROLLBACK"
    };

    private static CqlTerminalService instance;

    private final String cqlshExecutable;
    private final String host;
    private final String port;
    private final String user;
    private final String password;
    private final long timeoutMs;
    private final String defaultKeyspace;

    private CqlTerminalService() {
        ConfigLoader config = ConfigLoader.getInstance();
        this.cqlshExecutable = config.getProperty("cassandra.cqlsh.path", "cqlsh");
        this.host = config.getProperty("cassandra.host", "127.0.0.1");
        this.port = config.getProperty("cassandra.port", "9042");
        this.user = config.getProperty("cassandra.readonly.user", "terminal_ro");
        this.password = config.getProperty("cassandra.readonly.password", "");
        this.defaultKeyspace = config.getProperty("cassandra.default.keyspace",
                config.getProperty("cassandra.keyspace", null));
        long timeout;
        try {
            timeout = Long.parseLong(config.getProperty("cassandra.cqlsh.timeout.ms",
                    String.valueOf(DEFAULT_TIMEOUT_MS)));
        } catch (NumberFormatException nfe) {
            timeout = DEFAULT_TIMEOUT_MS;
        }
        this.timeoutMs = timeout;
    }

    public static synchronized CqlTerminalService getInstance() {
        if (instance == null) {
            instance = new CqlTerminalService();
        }
        return instance;
    }

    public Result runCommand(String cql) throws ErrorConexionCassandraException {
        if (cql == null || cql.isBlank()) {
            throw new IllegalArgumentException("El comando CQL no puede estar vacío");
        }

        List<String> command = new ArrayList<>(Arrays.asList(
                cqlshExecutable, host, port
        ));
        if (defaultKeyspace != null && !defaultKeyspace.isBlank()) {
            command.add("--keyspace");
            command.add(defaultKeyspace);
        }
        command.addAll(Arrays.asList("-u", user, "-p", password, "-e", cql));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                String timeoutMsg = "ERROR: Timeout tras " + timeoutMs + " ms";
                if (!stderr.isBlank()) {
                    stderr = stderr + System.lineSeparator() + timeoutMsg;
                } else {
                    stderr = timeoutMsg;
                }
                return new Result(-1, stdout, stderr, true);
            }

            return new Result(process.exitValue(), stdout, stderr, false);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ErrorConexionCassandraException("La ejecución fue interrumpida", ie);
        } catch (IOException ioe) {
            String detail = ioe.getMessage() != null ? ioe.getMessage() : "causa desconocida";
            throw new ErrorConexionCassandraException("No se pudo ejecutar cqlsh: " + detail, ioe);
        }
    }

    public boolean isWriteLike(String query) {
        if (query == null) {
            return false;
        }
        String upper = query.toUpperCase(Locale.ROOT);
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upper.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    public static class Result {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final boolean timedOut;

        public Result(int exitCode, String stdout, String stderr, boolean timedOut) {
            this.exitCode = exitCode;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
            this.timedOut = timedOut;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public boolean isTimedOut() {
            return timedOut;
        }
    }
}
