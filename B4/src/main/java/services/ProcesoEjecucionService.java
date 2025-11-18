package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import modelo.Medicion;
import modelo.Proceso;
import modelo.Sensor;
import modelo.SolicitudProceso;
import repository.MedicionCassandraDAO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Servicio encargado de ejecutar los procesos/claves definidos en Mongo y generar
 * un resumen que luego se persiste como resultado y observaciones de la solicitud.
 */
public class ProcesoEjecucionService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int LIMITE_MEDICIONES = 1000;
    private static final String PROCESO_NUEVO_NOMBRE = "Procesos para nuevo";

    private static ProcesoEjecucionService instance;

    private ProcesoEjecucionService() {}

    public static ProcesoEjecucionService getInstance() {
        if (instance == null) {
            instance = new ProcesoEjecucionService();
        }
        return instance;
    }

    public ProcesoResultado generarResultado(Proceso proceso, SolicitudProceso solicitud)
            throws ErrorConexionCassandraException, ErrorConexionMongoException {
        if (proceso == null || solicitud == null) {
            return new ProcesoResultado("No se pudo ejecutar el proceso: datos incompletos.", "Proceso o solicitud nulos.");
        }
        Map<String, Object> params = solicitud.getParametros() != null ? solicitud.getParametros() : Map.of();
        LocalDateTime fechaInicio = parseFecha(params.get("fechaInicio"), true);
        LocalDateTime fechaFin = parseFecha(params.get("fechaFin"), false);
        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            // intercambiar para evitar rangos negativos
            LocalDateTime tmp = fechaInicio;
            fechaInicio = fechaFin;
            fechaFin = tmp;
        }
        Instant inicio = fechaInicio != null ? fechaInicio.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant fin = fechaFin != null ? fechaFin.atZone(ZoneId.systemDefault()).toInstant() : null;

        List<Sensor> sensores = seleccionarSensores(params);
        if (sensores.isEmpty()) {
            return new ProcesoResultado("No se encontraron sensores para los parámetros proporcionados.",
                    "Verificar que existan sensores activos que coincidan con los filtros configurados.");
        }

        List<SensorMetricas> metricasPorSensor = new ArrayList<>();
        long totalMediciones = 0;
        double globalTempMin = Double.POSITIVE_INFINITY;
        double globalTempMax = Double.NEGATIVE_INFINITY;
        double globalHumMin = Double.POSITIVE_INFINITY;
        double globalHumMax = Double.NEGATIVE_INFINITY;
        double globalTempSum = 0;
        double globalHumSum = 0;

        for (Sensor sensor : sensores) {
            List<Medicion> mediciones = MedicionCassandraDAO.getInstance()
                    .obtenerPorSensor(sensor.getId(), inicio, fin, LIMITE_MEDICIONES);
            if (mediciones.isEmpty()) {
                continue;
            }
            SensorMetricas metricas = calcularMetricas(sensor, mediciones);
            metricasPorSensor.add(metricas);
            totalMediciones += metricas.cantidad;

            if (metricas.tempMin < globalTempMin) globalTempMin = metricas.tempMin;
            if (metricas.tempMax > globalTempMax) globalTempMax = metricas.tempMax;
            if (metricas.humMin < globalHumMin) globalHumMin = metricas.humMin;
            if (metricas.humMax > globalHumMax) globalHumMax = metricas.humMax;
            globalTempSum += metricas.tempSum;
            globalHumSum += metricas.humSum;
        }

        if (metricasPorSensor.isEmpty()) {
            return new ProcesoResultado("No se hallaron mediciones en el periodo indicado.",
                    "Los sensores seleccionados no registran datos en el rango especificado.");
        }

        metricasPorSensor.sort(Comparator.comparing(metricas -> metricas.sensorNombre.toLowerCase(Locale.ROOT)));
        double globalTempProm = totalMediciones > 0 ? globalTempSum / totalMediciones : 0;
        double globalHumProm = totalMediciones > 0 ? globalHumSum / totalMediciones : 0;

        String periodoTexto = construirPeriodoTexto(fechaInicio, fechaFin, totalMediciones);
        String resultado = construirDetalle(proceso, metricasPorSensor, periodoTexto,
                globalTempMin, globalTempMax, globalTempProm,
                globalHumMin, globalHumMax, globalHumProm, globalTempSum);

        String observaciones = construirObservaciones(proceso, sensores.size(), totalMediciones, params, metricasPorSensor);
        return new ProcesoResultado(resultado, observaciones);
    }

    private List<Sensor> seleccionarSensores(Map<String, Object> params) throws ErrorConexionCassandraException {
        String sensorIdParam = obtenerParametro(params, "sensorId");
        String ciudad = obtenerParametro(params, "ciudad");
        String zona = obtenerParametro(params, "zona");
        String pais = obtenerParametro(params, "pais");
        String tipoSensor = obtenerParametro(params, "tipoSensor");

        List<Sensor> sensoresSeleccionados = new ArrayList<>();
        if (sensorIdParam != null && !sensorIdParam.isBlank()) {
            Sensor sensor = SensorService.getInstance().obtenerSensor(sensorIdParam);
            if (sensor != null) {
                sensoresSeleccionados.add(sensor);
            }
            return sensoresSeleccionados;
        }

        List<Sensor> todos = SensorService.getInstance().listarTodos();
        for (Sensor sensor : todos) {
            if (sensor.getEstado() != null && !"ACTIVO".equalsIgnoreCase(sensor.getEstado())) {
                continue;
            }
            if (tipoSensor != null && sensor.getTipo() != null && !sensor.getTipo().equalsIgnoreCase(tipoSensor)) {
                continue;
            }
            if (ciudad != null && (sensor.getCiudad() == null || !sensor.getCiudad().equalsIgnoreCase(ciudad))) {
                continue;
            }
            if (pais != null && (sensor.getPais() == null || !sensor.getPais().equalsIgnoreCase(pais))) {
                continue;
            }
            if (zona != null) {
                if (sensor.getZona() == null || !sensor.getZona().equalsIgnoreCase(zona)) {
                    continue;
                }
            }
            sensoresSeleccionados.add(sensor);
        }
        return sensoresSeleccionados;
    }

    private SensorMetricas calcularMetricas(Sensor sensor, List<Medicion> mediciones) {
        SensorMetricas metricas = new SensorMetricas(sensor);
        for (Medicion medicion : mediciones) {
            if (medicion.getTemperatura() != null) {
                double temp = medicion.getTemperatura();
                if (temp < metricas.tempMin) metricas.tempMin = temp;
                if (temp > metricas.tempMax) metricas.tempMax = temp;
                metricas.tempSum += temp;
            }
            if (medicion.getHumedad() != null) {
                double hum = medicion.getHumedad();
                if (hum < metricas.humMin) metricas.humMin = hum;
                if (hum > metricas.humMax) metricas.humMax = hum;
                metricas.humSum += hum;
            }
            metricas.cantidad++;
            if (medicion.getFechaHora() != null) {
                if (metricas.fechaInicio == null || medicion.getFechaHora().isBefore(metricas.fechaInicio)) {
                    metricas.fechaInicio = medicion.getFechaHora();
                }
                if (metricas.fechaFin == null || medicion.getFechaHora().isAfter(metricas.fechaFin)) {
                    metricas.fechaFin = medicion.getFechaHora();
                }
            }
        }
        return metricas;
    }

    private String construirDetalle(Proceso proceso,
                                    List<SensorMetricas> metricas,
                                    String periodo,
                                    double globalTempMin,
                                    double globalTempMax,
                                    double globalTempProm,
                                    double globalHumMin,
                                    double globalHumMax,
                                    double globalHumProm,
                                    double globalTempSum) {
        StringBuilder sb = new StringBuilder();
        sb.append("Proceso: ").append(proceso.getNombre()).append("\n");
        if (proceso.getDescripcion() != null) {
            sb.append(proceso.getDescripcion()).append("\n");
        }
        sb.append("Tipo: ").append(proceso.getTipo()).append("\n");
        if (!periodo.isBlank()) {
            sb.append("Periodo: ").append(periodo).append("\n");
        }
        sb.append("\nResumen global\n");
        sb.append("Temperatura -> min: ").append(formatear(globalTempMin))
                .append(" °C | max: ").append(formatear(globalTempMax))
                .append(" °C | promedio: ").append(formatear(globalTempProm)).append(" °C\n");
        sb.append("Humedad -> min: ").append(formatear(globalHumMin))
                .append(" % | max: ").append(formatear(globalHumMax))
                .append(" % | promedio: ").append(formatear(globalHumProm)).append(" %\n");

        if (esProcesoSumatoria(proceso)) {
            sb.append("Sumatoria de temperaturas: ").append(formatear(globalTempSum)).append(" °C\n");
        }

        sb.append("\nDetalle por sensor:\n");
        for (SensorMetricas metrica : metricas) {
            sb.append("- ").append(metrica.sensorNombre);
            if (metrica.codigo != null && !metrica.codigo.isBlank()) {
                sb.append(" (").append(metrica.codigo).append(")");
            }
            sb.append(" | Ubicación: ");
            sb.append(metrica.ciudad != null ? metrica.ciudad : "-");
            if (metrica.zona != null && !metrica.zona.isBlank()) {
                sb.append(" / ").append(metrica.zona);
            }
            sb.append(" / ").append(metrica.pais != null ? metrica.pais : "-");
            sb.append("\n  Mediciones procesadas: ").append(metrica.cantidad);
            if (metrica.fechaInicio != null && metrica.fechaFin != null) {
                sb.append(" (").append(DATE_TIME_FMT.format(metrica.fechaInicio))
                        .append(" -> ").append(DATE_TIME_FMT.format(metrica.fechaFin)).append(")");
            }
            sb.append("\n  Temperatura -> min: ").append(formatear(metrica.tempMin))
                    .append(" °C | max: ").append(formatear(metrica.tempMax))
                    .append(" °C | promedio: ").append(formatear(metrica.getTempPromedio())).append(" °C");
            sb.append("\n  Humedad -> min: ").append(formatear(metrica.humMin))
                    .append(" % | max: ").append(formatear(metrica.humMax))
                    .append(" % | promedio: ").append(formatear(metrica.getHumPromedio())).append(" %\n\n");
        }
        return sb.toString().trim();
    }

    private String construirObservaciones(Proceso proceso,
                                          int sensores,
                                          long totalMediciones,
                                          Map<String, Object> params,
                                          List<SensorMetricas> metricas) {
        StringBuilder obs = new StringBuilder();
        obs.append("Sensores analizados: ").append(sensores)
           .append(" | Mediciones consideradas: ").append(totalMediciones);
        String ciudad = obtenerParametro(params, "ciudad");
        String pais = obtenerParametro(params, "pais");
        String zona = obtenerParametro(params, "zona");
        if (ciudad != null) {
            obs.append(" | Ciudad: ").append(ciudad);
        }
        if (zona != null) {
            obs.append(" | Zona: ").append(zona);
        }
        if (pais != null) {
            obs.append(" | País: ").append(pais);
        }
        String periodicidad = obtenerParametro(params, "periodicidad");
        if ("PROCESO_PERIODICO".equalsIgnoreCase(proceso.getTipo()) && periodicidad != null) {
            obs.append(" | Periodicidad solicitada: ").append(periodicidad);
        }
        if (metricas.stream().anyMatch(m -> m.cantidad >= LIMITE_MEDICIONES)) {
            obs.append(" | Nota: se alcanzó el límite de ").append(LIMITE_MEDICIONES)
               .append(" mediciones por sensor.");
        }
        return obs.toString();
    }

    private LocalDateTime parseFecha(Object valor, boolean startOfDay) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalDateTime) {
            return (LocalDateTime) valor;
        }
        if (valor instanceof LocalDate) {
            return ((LocalDate) valor).atStartOfDay();
        }
        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(texto);
            return dateTime;
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(texto, DATE_FMT);
            return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(texto, DATE_TIME_FMT);
            return dateTime;
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private String obtenerParametro(Map<String, Object> params, String clave) {
        if (params == null || clave == null) {
            return null;
        }
        Object valor = params.get(clave);
        if (valor == null) {
            return null;
        }
        if (valor instanceof String) {
            String texto = ((String) valor).trim();
            return texto.isEmpty() ? null : texto;
        }
        return Objects.toString(valor, null);
    }

    private String formatear(double valor) {
        if (Double.isInfinite(valor) || Double.isNaN(valor)) {
            return "-";
        }
        return String.format(Locale.US, "%.2f", valor);
    }

    private boolean esProcesoSumatoria(Proceso proceso) {
        return proceso != null && PROCESO_NUEVO_NOMBRE.equalsIgnoreCase(proceso.getNombre());
    }

    private String construirPeriodoTexto(LocalDateTime inicio, LocalDateTime fin, long totalMediciones) {
        if (inicio == null && fin == null) {
            return totalMediciones > 0 ? "últimas mediciones disponibles" : "";
        }
        StringBuilder sb = new StringBuilder();
        if (inicio != null) {
            sb.append(DATE_TIME_FMT.format(inicio));
        }
        sb.append(" -> ");
        if (fin != null) {
            sb.append(DATE_TIME_FMT.format(fin));
        } else {
            sb.append("actualidad");
        }
        return sb.toString();
    }

    private static class SensorMetricas {
        final String sensorId;
        final String sensorNombre;
        final String codigo;
        final String ciudad;
        final String zona;
        final String pais;
        long cantidad = 0;
        double tempMin = Double.POSITIVE_INFINITY;
        double tempMax = Double.NEGATIVE_INFINITY;
        double humMin = Double.POSITIVE_INFINITY;
        double humMax = Double.NEGATIVE_INFINITY;
        double tempSum = 0;
        double humSum = 0;
        LocalDateTime fechaInicio;
        LocalDateTime fechaFin;

        SensorMetricas(Sensor sensor) {
            this.sensorId = sensor.getId();
            this.sensorNombre = sensor.getNombre() != null ? sensor.getNombre() : sensor.getId();
            this.codigo = sensor.getCodigo();
            this.ciudad = sensor.getCiudad();
            this.zona = sensor.getZona();
            this.pais = sensor.getPais();
        }

        double getTempPromedio() {
            return cantidad > 0 ? tempSum / cantidad : 0;
        }

        double getHumPromedio() {
            return cantidad > 0 ? humSum / cantidad : 0;
        }
    }

    public static class ProcesoResultado {
        private final String resultado;
        private final String observaciones;

        public ProcesoResultado(String resultado, String observaciones) {
            this.resultado = resultado;
            this.observaciones = observaciones;
        }

        public String getResultado() {
            return resultado;
        }

        public String getObservaciones() {
            return observaciones;
        }
    }
}
