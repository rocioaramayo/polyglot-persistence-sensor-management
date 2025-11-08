package ui.components;

import exceptions.ErrorConexionCassandraException;
import services.CqlTerminalService;
import utils.ConfigLoader;
import utils.ConfigLoader;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;

/**
 * Panel reutilizable que emula una terminal para ejecutar comandos CQL mediante cqlsh.
 * Mantiene el tiempo de sesión para poder facturar el uso desde frames que lo contengan.
 */
public class CassandraTerminalPanel extends JPanel {

    private static final int SESSION_TICK_MS = 1000;

    private final JTextArea outputArea;
    private final JTextField commandField;
    private final JButton ejecutarBtn;
    private final JButton limpiarBtn;
    private final JLabel sessionLabel;
    private final JLabel statusLabel;
    private final javax.swing.Timer sessionTimer;
    private final CqlTerminalService terminalService;
    private final String prompt;

    private long sessionStartMs = -1L;
    private long accumulatedMs = 0L;

    public CassandraTerminalPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        terminalService = CqlTerminalService.getInstance();
        prompt = buildPrompt();

        outputArea = buildOutputArea();
        commandField = buildCommandField();
        ejecutarBtn = new JButton("Ejecutar");
        limpiarBtn = new JButton("Limpiar");
        sessionLabel = new JLabel("00:00:00");
        statusLabel = new JLabel("Listo");
        statusLabel.setForeground(new Color(0x22AA22));

        sessionTimer = new javax.swing.Timer(SESSION_TICK_MS, e -> updateSessionLabel());

        add(buildInputPanel(), BorderLayout.NORTH);
        add(buildOutputPanel(), BorderLayout.CENTER);

        ejecutarBtn.addActionListener(e -> ejecutarComando());
        limpiarBtn.addActionListener(e -> {
            outputArea.setText("");
            escribirBienvenida();
            statusLabel.setText("Buffer limpio");
            statusLabel.setForeground(new Color(0x22AA22));
            commandField.requestFocusInWindow();
        });

        sessionTimer.setRepeats(true);
        sessionTimer.stop();

        escribirBienvenida();
    }

    private JPanel buildInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(new TitledBorder("Terminal Cassandra (solo lectura)"));

        JPanel commandRow = new JPanel(new BorderLayout(6, 6));
        JLabel promptLabel = new JLabel("Comando:");
        promptLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandRow.add(promptLabel, BorderLayout.WEST);
        commandRow.add(commandField, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(new JLabel("Tiempo sesión:"));
        buttons.add(sessionLabel);
        buttons.add(limpiarBtn);
        buttons.add(ejecutarBtn);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.add(statusLabel, BorderLayout.WEST);
        JLabel hintLabel = new JLabel("Enter = ejecutar, Esc = limpiar línea, CTL+L = limpiar salida");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statusRow.add(hintLabel, BorderLayout.EAST);

        inputPanel.add(commandRow, BorderLayout.NORTH);
        inputPanel.add(buttons, BorderLayout.CENTER);
        inputPanel.add(statusRow, BorderLayout.SOUTH);
        return inputPanel;
    }

    private JScrollPane buildOutputPanel() {
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(new TitledBorder("Salida cruda (stdout/stderr)"));
        outputArea.setText("");
        return scrollPane;
    }

    private JTextArea buildOutputArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(0x111317));
        area.setForeground(new Color(0xDCE3E7));
        area.setCaretColor(Color.WHITE);
        area.setMargin(new Insets(8, 8, 8, 8));
        return area;
    }

    private JTextField buildCommandField() {
        JTextField field = new JTextField();
        field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        field.setBackground(new Color(0x1E1E1E));
        field.setForeground(new Color(0xF4F4F4));
        field.setCaretColor(Color.WHITE);
        field.setToolTipText("Escribí solo consultas CQL (ej. SELECT * FROM mediciones LIMIT 10;). No incluyas 'cqlsh'.");
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3C3F41)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        field.addActionListener(e -> ejecutarBtn.doClick());
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    field.setText("");
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L) {
                    limpiarBtn.doClick();
                    e.consume();
                }
            }
        });
        return field;
    }

    private void ejecutarComando() {
        String comando = commandField.getText().trim();
        if (comando.isEmpty()) {
            mostrarEstado("Ingresá un comando primero", new Color(0xCC8800));
            return;
        }

        appendLine(prompt + comando);

        if (pareceComandoCqlsh(comando)) {
            appendLine("! Ingresá únicamente la sentencia CQL, sin prefijos 'cqlsh' ni parámetros -e/-u/-p.");
            mostrarEstado("Remové 'cqlsh' del texto ingresado", Color.ORANGE);
            commandField.setText("");
            return;
        }

        if (terminalService.isWriteLike(comando)) {
            appendLine("! Solo lectura: comando bloqueado.");
            mostrarEstado("Comando bloqueado por política de solo lectura", Color.ORANGE);
            commandField.setText("");
            return;
        }

        ejecutarBtn.setEnabled(false);
        commandField.setEnabled(false);
        statusLabel.setText("Ejecutando...");
        statusLabel.setForeground(new Color(0x2080FF));

        SwingWorker<CqlTerminalService.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected CqlTerminalService.Result doInBackground() throws Exception {
                return terminalService.runCommand(comando);
            }

            @Override
            protected void done() {
                ejecutarBtn.setEnabled(true);
                commandField.setEnabled(true);
                commandField.setText("");
                commandField.requestFocusInWindow();
                try {
                    CqlTerminalService.Result result = get();
                    imprimirResultado(result);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    appendLine("! Ejecución interrumpida.");
                    mostrarEstado("Ejecución interrumpida", Color.RED);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    procesarError(cause != null ? cause : ex);
                }
            }
        };
        worker.execute();
    }

    private void imprimirResultado(CqlTerminalService.Result result) {
        StringBuilder block = new StringBuilder();
        String stdout = result.getStdout();
        String stderr = result.getStderr();
        if (!stdout.isBlank()) {
            block.append(stdout.trim()).append(System.lineSeparator());
        }
        if (!stderr.isBlank()) {
            block.append(stderr.trim()).append(System.lineSeparator());
        }
        if (result.isTimedOut()) {
            block.append("! Tiempo excedido al ejecutar el comando.").append(System.lineSeparator());
        }
        block.append(String.format("(exit code %d)", result.getExitCode()));
        appendLine(block.toString());
        mostrarEstado("Comando completado", new Color(0x22AA22));
    }

    private void procesarError(Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : error.toString();
        appendLine("! Error ejecutando comando: " + message);
        if (message != null && message.contains("No se pudo ejecutar cqlsh")) {
            appendLine("! Verifica que el binario 'cqlsh' exista o especificá cassandra.cqlsh.path en application.properties.");
        }
        if (error instanceof ErrorConexionCassandraException) {
            mostrarEstado("Error Cassandra", Color.RED);
        } else {
            mostrarEstado("Error general", Color.RED);
        }
    }

    private void mostrarEstado(String texto, Color color) {
        statusLabel.setText(texto);
        statusLabel.setForeground(color);
    }

    private void appendLine(String text) {
        if (outputArea.getDocument().getLength() > 0) {
            outputArea.append(System.lineSeparator());
        }
        outputArea.append(text + System.lineSeparator());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void escribirBienvenida() {
        outputArea.append("Polyglot Cassandra Terminal - SOLO LECTURA" + System.lineSeparator());
        outputArea.append("Los comandos se ejecutan via cqlsh -e y se muestra stdout/stderr tal cual." + System.lineSeparator());
        outputArea.append("Se aplica automáticamente el keyspace configurado; no es necesario ejecutar USE ..." + System.lineSeparator());
        outputArea.append("---------------------------------------------------------------------" + System.lineSeparator());
    }

    // --- Control de sesión / facturación ---

    public void startSessionTracking() {
        if (sessionStartMs < 0) {
            sessionStartMs = System.currentTimeMillis();
        }
        if (!sessionTimer.isRunning()) {
            sessionTimer.start();
        }
        updateSessionLabel();
    }

    public void pauseSessionTracking() {
        if (sessionStartMs >= 0) {
            accumulatedMs += System.currentTimeMillis() - sessionStartMs;
            sessionStartMs = -1L;
        }
        if (sessionTimer.isRunning()) {
            sessionTimer.stop();
        }
        updateSessionLabel();
    }

    public long stopAndConsumeMillis() {
        pauseSessionTracking();
        long total = accumulatedMs;
        accumulatedMs = 0L;
        updateSessionLabel();
        return total;
    }

    public long peekAccumulatedMillis() {
        long total = accumulatedMs;
        if (sessionStartMs >= 0) {
            total += System.currentTimeMillis() - sessionStartMs;
        }
        return total;
    }

    private void updateSessionLabel() {
        long millis = peekAccumulatedMillis();
        sessionLabel.setText(formatearDuracion(millis));
    }

    private String formatearDuracion(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private boolean pareceComandoCqlsh(String texto) {
        String normalized = texto.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.startsWith("cqlsh")
                || normalized.contains(" --keyspace ")
                || normalized.contains(" -e ")
                || normalized.contains(" -u ")
                || normalized.contains(" -p ");
    }

    private String buildPrompt() {
        ConfigLoader config = ConfigLoader.getInstance();
        String keyspace = config.getProperty("cassandra.default.keyspace",
                config.getProperty("cassandra.keyspace", null));
        if (keyspace == null || keyspace.isBlank()) {
            return "cqlsh> ";
        }
        return "cqlsh:" + keyspace + "> ";
    }
}
