package services;

import exceptions.ErrorConexionCassandraException;
import modelo.Sensor;
import repository.SensorCassandraDAO;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generador global de mediciones aleatorias para todos los sensores.
 * Corre en background cada N segundos sin detenerse (simulación continua).
 */
public class AutoMedicionService {
    private static AutoMedicionService instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Random rnd = new Random();
    private long periodSeconds = 10; // intervalo por defecto

    private AutoMedicionService() {}

    public static AutoMedicionService getInstance() {
        if (instance == null) instance = new AutoMedicionService();
        return instance;
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::tick, 2, periodSeconds, TimeUnit.SECONDS);
        }
    }

    public void setPeriodSeconds(long seconds) {
        this.periodSeconds = Math.max(1, seconds);
    }

    private void tick() {
        try {
            List<Sensor> sensores = SensorCassandraDAO.getInstance().listarTodos();
            for (Sensor s : sensores) {
                String estado = s.getEstado();
                if (estado != null && !"ACTIVO".equalsIgnoreCase(estado)) {
                    continue; // sensor deshabilitado en Cassandra
                }
                // Simular según tipo de sensor; otros campos quedan en null
                String tipo = s.getTipo() != null ? s.getTipo().toLowerCase() : "temperatura";
                Double t = null;
                Double h = null;
                if ("temperatura".equals(tipo)) {
                    t = 15 + rnd.nextDouble() * 30; // 15-45°C
                } else if ("humedad".equals(tipo)) {
                    h = 30 + rnd.nextDouble() * 60; // 30-90%
                } else {
                    // Tipo desconocido: generar ambas para no dejar vacío
                    t = 15 + rnd.nextDouble() * 30;
                    h = 30 + rnd.nextDouble() * 60;
                }
                MedicionService.getInstance().registrarMedicion(s.getId(), t, h);
            }
        } catch (ErrorConexionCassandraException ignored) {
            // Evitamos cortar el scheduler por errores puntuales
        } catch (Exception ignored) {
        }
    }
}
