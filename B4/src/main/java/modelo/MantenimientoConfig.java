package modelo;

import java.time.LocalDateTime;

public class MantenimientoConfig {
    private String id;
    private String sensorId;
    private Integer frecuenciaDias;
    private LocalDateTime ultimaRevision;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public Integer getFrecuenciaDias() { return frecuenciaDias; }
    public void setFrecuenciaDias(Integer frecuenciaDias) { this.frecuenciaDias = frecuenciaDias; }

    public LocalDateTime getUltimaRevision() { return ultimaRevision; }
    public void setUltimaRevision(LocalDateTime ultimaRevision) { this.ultimaRevision = ultimaRevision; }
}
