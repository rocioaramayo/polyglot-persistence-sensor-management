package simulator;

import modelo.Medicion;
import modelo.Sensor;
import repository.MedicionCassandraDAO;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simula sensores de temperatura y humedad alrededor del mundo
 * Genera mediciones periódicamente y las almacena en Cassandra
 */
public class SensorSimulator {
    private final MedicionCassandraDAO medicionDAO;
    private final ScheduledExecutorService executor;
    private final Map<String, Sensor> sensores;
    private final Random random;
    private volatile boolean running;

    public SensorSimulator(MedicionCassandraDAO medicionDAO) {
        this.medicionDAO = medicionDAO;
        this.executor = Executors.newScheduledThreadPool(5);
        this.sensores = new ConcurrentHashMap<>();
        this.random = new Random();
        this.running = false;
    }

    /**
     * Inicializa sensores en diferentes ubicaciones del mundo
     */
    public void inicializarSensores() {
        String[][] ubicaciones = {
            {"SENSOR_NYC_01", "New York", "USA", "40.7128", "-74.0060"},
            {"SENSOR_LON_01", "London", "UK", "51.5074", "-0.1278"},
            {"SENSOR_TYO_01", "Tokyo", "Japan", "35.6762", "139.6503"},
            {"SENSOR_SYD_01", "Sydney", "Australia", "-33.8688", "151.2093"},
            {"SENSOR_BUE_01", "Buenos Aires", "Argentina", "-34.6037", "-58.3816"}
        };

        for (String[] ubicacion : ubicaciones) {
            Sensor sensor = new Sensor();
            sensor.setId(ubicacion[0]);
            sensor.setUbicacion(ubicacion[1]);
            sensor.setPais(ubicacion[2]);
            sensor.setLatitud(Double.parseDouble(ubicacion[3]));
            sensor.setLongitud(Double.parseDouble(ubicacion[4]));
            sensor.setFechaInstalacion(LocalDateTime.now());
            sensor.setActivo(true);
            sensores.put(sensor.getId(), sensor);
        }
        System.out.println("[SIMULATOR] " + sensores.size() + " sensores inicializados");
    }

    /**
     * Inicia la simulación de mediciones
     */
    public void iniciarSimulacion() {
        if (running) return;
        running = true;

        for (Sensor sensor : sensores.values()) {
            executor.scheduleAtFixedRate(
                () -> generarMedicion(sensor),
                0,
                5,
                TimeUnit.SECONDS
            );
        }
        System.out.println("[SIMULATOR] Simulación iniciada");
    }

    /**
     * Genera una medición aleatoria para un sensor
     */
    private void generarMedicion(Sensor sensor) {
        try {
            Medicion medicion = new Medicion();
            medicion.setId(UUID.randomUUID().toString());
            medicion.setSensorId(sensor.getId());
            medicion.setTemperatura(15 + random.nextDouble() * 30); // 15-45°C
            medicion.setHumedad(30 + random.nextDouble() * 60); // 30-90%
            medicion.setFecha(LocalDateTime.now());

            medicionDAO.guardar(medicion);
            System.out.println("[SIMULATOR] Medición guardada: " + sensor.getId() + 
                             " - Temp: " + String.format("%.2f", medicion.getTemperatura()) + 
                             "°C, Humedad: " + String.format("%.2f", medicion.getHumedad()) + "%");
        } catch (Exception e) {
            System.err.println("[SIMULATOR ERROR] " + e.getMessage());
        }
    }

    /**
     * Detiene la simulación
     */
    public void detenerSimulacion() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[SIMULATOR] Simulación detenida");
    }

    public Map<String, Sensor> getSensores() {
        return new HashMap<>(sensores);
    }

    public boolean isRunning() {
        return running;
    }
}
