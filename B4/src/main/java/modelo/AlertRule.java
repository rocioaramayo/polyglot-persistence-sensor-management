package modelo;

import java.time.LocalDateTime;

public class AlertRule {
    private String id;
    private String nombre;
    private String descripcion;
    private String ciudad;
    private String zona;
    private String pais;
    private String severidad;
    private Double temperaturaMin;
    private Double temperaturaMax;
    private Double humedadMin;
    private Double humedadMax;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private Integer ventanaMinutos;
    private Boolean activo;
    private LocalDateTime ultimaEvaluacion;
    private LocalDateTime ultimaAlerta;
    private LocalDateTime fechaCreacion;

    public AlertRule() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
        this.ventanaMinutos = 60;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
    }

    public Double getTemperaturaMin() {
        return temperaturaMin;
    }

    public void setTemperaturaMin(Double temperaturaMin) {
        this.temperaturaMin = temperaturaMin;
    }

    public Double getTemperaturaMax() {
        return temperaturaMax;
    }

    public void setTemperaturaMax(Double temperaturaMax) {
        this.temperaturaMax = temperaturaMax;
    }

    public Double getHumedadMin() {
        return humedadMin;
    }

    public void setHumedadMin(Double humedadMin) {
        this.humedadMin = humedadMin;
    }

    public Double getHumedadMax() {
        return humedadMax;
    }

    public void setHumedadMax(Double humedadMax) {
        this.humedadMax = humedadMax;
    }

    public LocalDateTime getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDateTime fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDateTime getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDateTime fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Integer getVentanaMinutos() {
        return ventanaMinutos;
    }

    public void setVentanaMinutos(Integer ventanaMinutos) {
        this.ventanaMinutos = ventanaMinutos;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getUltimaEvaluacion() {
        return ultimaEvaluacion;
    }

    public void setUltimaEvaluacion(LocalDateTime ultimaEvaluacion) {
        this.ultimaEvaluacion = ultimaEvaluacion;
    }

    public LocalDateTime getUltimaAlerta() {
        return ultimaAlerta;
    }

    public void setUltimaAlerta(LocalDateTime ultimaAlerta) {
        this.ultimaAlerta = ultimaAlerta;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
