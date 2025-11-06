package modelo;

import java.time.LocalDateTime;

public class Factura {
    private Integer id;
    private String solicitudId;
    private String usuarioId;
    private LocalDateTime fechaEmision;
    private Double monto;
    private String estado; // pendiente, pagada, vencida
    private String descripcion;
    // Datos de facturacion (snapshot)
    private String nombreFacturacion;
    private String apellidoFacturacion;
    private String direccionFacturacion;
    private String telefonoFacturacion;

    public Factura() {}

    public Factura(String usuarioId, Double monto, String descripcion) {
        this.usuarioId = usuarioId;
        this.monto = monto;
        this.descripcion = descripcion;
        this.estado = "pendiente";
        this.fechaEmision = LocalDateTime.now();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSolicitudId() { return solicitudId; }
    public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public void setFecha(LocalDateTime fecha) { this.fechaEmision = fecha; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreFacturacion() { return nombreFacturacion; }
    public void setNombreFacturacion(String nombreFacturacion) { this.nombreFacturacion = nombreFacturacion; }

    public String getApellidoFacturacion() { return apellidoFacturacion; }
    public void setApellidoFacturacion(String apellidoFacturacion) { this.apellidoFacturacion = apellidoFacturacion; }

    public String getDireccionFacturacion() { return direccionFacturacion; }
    public void setDireccionFacturacion(String direccionFacturacion) { this.direccionFacturacion = direccionFacturacion; }

    public String getTelefonoFacturacion() { return telefonoFacturacion; }
    public void setTelefonoFacturacion(String telefonoFacturacion) { this.telefonoFacturacion = telefonoFacturacion; }
}
