package modelo;

import java.time.LocalDateTime;

public class Movimiento {
    private Integer id;
    private Integer cuentaId;
    private String tipo; // CARGO, ABONO
    private Double monto;
    private Double saldoAnterior;
    private Double saldoNuevo;
    private LocalDateTime fecha;
    private String descripcion;
    private String referenciaId;

    public Movimiento() {}

    public Movimiento(Integer cuentaId, String tipo, Double monto, Double saldoAnterior,
                      Double saldoNuevo, String descripcion) {
        this(cuentaId, tipo, monto, saldoAnterior, saldoNuevo, descripcion, null);
    }

    public Movimiento(Integer cuentaId, String tipo, Double monto, Double saldoAnterior,
                      Double saldoNuevo, String descripcion, String referenciaId) {
        this.cuentaId = cuentaId;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoAnterior = saldoAnterior;
        this.saldoNuevo = saldoNuevo;
        this.descripcion = descripcion;
        this.referenciaId = referenciaId;
        this.fecha = LocalDateTime.now();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCuentaId() { return cuentaId; }
    public void setCuentaId(Integer cuentaId) { this.cuentaId = cuentaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Double getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(Double saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public Double getSaldoNuevo() { return saldoNuevo; }
    public void setSaldoNuevo(Double saldoNuevo) { this.saldoNuevo = saldoNuevo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
}
