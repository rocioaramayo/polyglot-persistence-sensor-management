package services;

import exceptions.ErrorConexionMySQLException;
import utils.ConfigLoader;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula y registra cargos por uso del terminal Cassandra basÃ¡ndose en la duraciÃ³n de la sesiÃ³n.
 */
public class TerminalUsageService {

    private static final double DEFAULT_RATE_PER_MINUTE = 0.25; // USD
    private static final long MIN_BILLABLE_MILLIS = 5_000; // ignorar sesiones muy breves

    private static TerminalUsageService instance;

    private final double ratePerMinute;

    private TerminalUsageService() {
        ConfigLoader config = ConfigLoader.getInstance();
        double rate;
        try {
            rate = Double.parseDouble(config.getProperty("terminal.rate.per.minute",
                    String.valueOf(DEFAULT_RATE_PER_MINUTE)));
        } catch (NumberFormatException nfe) {
            rate = DEFAULT_RATE_PER_MINUTE;
        }
        this.ratePerMinute = rate;
    }

    public static synchronized TerminalUsageService getInstance() {
        if (instance == null) {
            instance = new TerminalUsageService();
        }
        return instance;
    }

    public void registrarUso(String usuarioId, long sessionMillis) throws ErrorConexionMySQLException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return;
        }
        if (sessionMillis < MIN_BILLABLE_MILLIS) {
            return;
        }

        double minutes = sessionMillis / 60000d;
        double rawAmount = minutes * ratePerMinute;
        double amount = roundCurrency(Math.max(rawAmount, ratePerMinute / 60d)); // cobra al menos 1 min proporcional

        String descripcion = "Uso terminal Cassandra (" + formatearDuracion(sessionMillis) + ")";
        FacturaService.getInstance().crearFactura(usuarioId, amount, descripcion);
    }

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatearDuracion(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
