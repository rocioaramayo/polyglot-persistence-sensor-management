package modelo;

import java.time.LocalDateTime;

public class Mensaje {
    private String id;
    private String remitenteId;
    private String destinatarioId;
    private String contenido;
    private LocalDateTime fechaHora;
    private String tipo; // privado, grupal
    private Boolean leido;

    public Mensaje() {}

    public Mensaje(String remitenteId, String destinatarioId, String contenido, String tipo) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.contenido = contenido;
        this.tipo = tipo;
        this.fechaHora = LocalDateTime.now();
        this.leido = false;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRemitenteId() { return remitenteId; }
    public void setRemitenteId(String remitenteId) { this.remitenteId = remitenteId; }

    public void setRemitente(String remitente) { this.remitenteId = remitente; }

    public String getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(String destinatarioId) { this.destinatarioId = destinatarioId; }

    public void setDestinatario(String destinatario) { this.destinatarioId = destinatario; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public void setFecha(LocalDateTime fecha) { this.fechaHora = fecha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Boolean getLeido() { return leido; }
    public void setLeido(Boolean leido) { this.leido = leido; }
}
