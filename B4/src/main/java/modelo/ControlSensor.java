package modelo;

import java.time.LocalDateTime;
import java.util.List;

public class ControlSensor {
    private String id; // ObjectId de MongoDB
    private String sensorId;
    private Integer tecnicoId;
    private LocalDateTime fechaRevision;
    private String estadoSensor; // ACTIVO, INACTIVO, FALLA, MANTENIMIENTO
    private String observaciones;
    private List<String> accionesRealizadas;
    private LocalDateTime proximaRevision;

    public ControlSensor() {}

    public ControlSensor(String sensorId, Integer tecnicoId, String estadoSensor) {
        this.sensorId = sensorId;
        this.tecnicoId = tecnicoId;
        this.estadoSensor = estadoSensor;
        this.fechaRevision = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public Integer getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Integer tecnicoId) { this.tecnicoId = tecnicoId; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public String getEstadoSensor() { return estadoSensor; }
    public void setEstadoSensor(String estadoSensor) { this.estadoSensor = estadoSensor; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<String> getAccionesRealizadas() { return accionesRealizadas; }
    public void setAccionesRealizadas(List<String> accionesRealizadas) { this.accionesRealizadas = accionesRealizadas; }

    public LocalDateTime getProximaRevision() { return proximaRevision; }
    public void setProximaRevision(LocalDateTime proximaRevision) { this.proximaRevision = proximaRevision; }
}
