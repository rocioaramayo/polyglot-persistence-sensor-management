package modelo;

import java.time.LocalDateTime;

public class Pago {
    private Integer id;
    private Integer facturaId;
    private String usuarioId;
    private Double monto;
    private LocalDateTime fechaPago;
    private String metodoPago;
    private String referencia;
    private String estado; // COMPLETADO, PENDIENTE, FALLIDO

    public Pago() {}

    public Pago(Integer facturaId, String usuarioId, Double monto, String metodoPago) {
        this.facturaId = facturaId;
        this.usuarioId = usuarioId;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.estado = "COMPLETADO";
        this.fechaPago = LocalDateTime.now();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getFacturaId() { return facturaId; }
    public void setFacturaId(Integer facturaId) { this.facturaId = facturaId; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
