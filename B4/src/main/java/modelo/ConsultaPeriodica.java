package modelo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa una ejecución periódica aprobada por un técnico.
 * Guarda la metadata necesaria para que el scheduler reprograme la solicitud
 * sin volver a pasar por la interfaz humana.
 */
public class ConsultaPeriodica {
    private String id;
    private String solicitudBaseId;
    private String procesoId;
    private String tipoProceso;
    private String usuarioId;
    private String tecnicoId;
    private String nombre;
    private String descripcion;
    private Map<String, Object> parametros = new HashMap<>();
    private String periodicidad;
    private long intervaloMinutos;
    private LocalDateTime proximaEjecucion;
    private LocalDateTime ultimaEjecucion;
    private boolean activo = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSolicitudBaseId() {
        return solicitudBaseId;
    }

    public void setSolicitudBaseId(String solicitudBaseId) {
        this.solicitudBaseId = solicitudBaseId;
    }

    public String getProcesoId() {
        return procesoId;
    }

    public void setProcesoId(String procesoId) {
        this.procesoId = procesoId;
    }

    public String getTipoProceso() {
        return tipoProceso;
    }

    public void setTipoProceso(String tipoProceso) {
        this.tipoProceso = tipoProceso;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(String tecnicoId) {
        this.tecnicoId = tecnicoId;
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

    public Map<String, Object> getParametros() {
        return parametros;
    }

    public void setParametros(Map<String, Object> parametros) {
        this.parametros = parametros != null ? parametros : new HashMap<>();
    }

    public String getPeriodicidad() {
        return periodicidad;
    }

    public void setPeriodicidad(String periodicidad) {
        this.periodicidad = periodicidad;
    }

    public long getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(long intervaloMinutos) {
        this.intervaloMinutos = intervaloMinutos;
    }

    public LocalDateTime getProximaEjecucion() {
        return proximaEjecucion;
    }

    public void setProximaEjecucion(LocalDateTime proximaEjecucion) {
        this.proximaEjecucion = proximaEjecucion;
    }

    public LocalDateTime getUltimaEjecucion() {
        return ultimaEjecucion;
    }

    public void setUltimaEjecucion(LocalDateTime ultimaEjecucion) {
        this.ultimaEjecucion = ultimaEjecucion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
