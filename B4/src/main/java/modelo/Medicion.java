package modelo;

import java.time.LocalDateTime;

public class Medicion {
    private String id;
    private String sensorId;
    private LocalDateTime fechaHora;
    private Double temperatura;
    private Double humedad;

    public Medicion() {}

    public Medicion(String sensorId, Double temperatura, Double humedad) {
        this.sensorId = sensorId;
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.fechaHora = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public void setFecha(LocalDateTime fecha) { this.fechaHora = fecha; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public Double getHumedad() { return humedad; }
    public void setHumedad(Double humedad) { this.humedad = humedad; }
}
