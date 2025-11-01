package modelo;

import java.time.LocalDateTime;

public class Factura {
    private Integer id;
    private Integer usuarioId;
    private LocalDateTime fechaEmision;
    private Double monto;
    private String estado; // pendiente, pagada, vencida
    private String descripcion;

    public Factura() {}

    public Factura(Integer usuarioId, Double monto, String descripcion) {
        this.usuarioId = usuarioId;
        this.monto = monto;
        this.descripcion = descripcion;
        this.estado = "pendiente";
        this.fechaEmision = LocalDateTime.now();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public void setFecha(LocalDateTime fecha) { this.fechaEmision = fecha; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
