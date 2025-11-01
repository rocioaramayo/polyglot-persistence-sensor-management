package modelo;

import java.time.LocalDateTime;

public class Alerta {
    private String id; // ObjectId de MongoDB
    private String tipo; // SENSOR, CLIMATICA, SISTEMA
    private String sensorId; // ID del sensor (si aplica)
    private String usuarioId; // ID del usuario destinatario (si aplica)
    private LocalDateTime fecha;
    private String descripcion;
    private String severidad; // BAJA, MEDIA, ALTA, CRITICA
    private String estado; // ACTIVA, RESUELTA, IGNORADA
    private LocalDateTime fechaResolucion;
    private String resueltoPor; // ID del usuario que resolvió

    public Alerta() {}

    public Alerta(String tipo, String descripcion, String severidad) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.estado = "ACTIVA";
        this.fecha = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }

    public String getResueltoPor() { return resueltoPor; }
    public void setResueltoPor(String resueltoPor) { this.resueltoPor = resueltoPor; }
}
