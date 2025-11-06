package modelo;

public class CuentaCorriente {
    private Integer id;
    private String usuarioId;
    private Double saldo;
    private Double limite;

    public CuentaCorriente() {}

    public CuentaCorriente(String usuarioId) {
        this.usuarioId = usuarioId;
        this.saldo = 0.0;
        this.limite = 1000.0;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public Double getLimite() { return limite; }
    public void setLimite(Double limite) { this.limite = limite; }
}
