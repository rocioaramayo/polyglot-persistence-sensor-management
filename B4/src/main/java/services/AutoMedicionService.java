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
                // Generar siempre, independientemente del estado: simulación continua
                double t = 15 + rnd.nextDouble() * 30; // 15-45°C
                double h = 30 + rnd.nextDouble() * 60; // 30-90%
                MedicionService.getInstance().registrarMedicion(s.getId(), t, h);
            }
        } catch (ErrorConexionCassandraException ignored) {
            // Evitamos cortar el scheduler por errores puntuales
        } catch (Exception ignored) {
        }
    }
}

