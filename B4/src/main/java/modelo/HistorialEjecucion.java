package modelo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class HistorialEjecucion {
    private String solicitudId;
    private LocalDateTime fechaEjecucion;
    private String id;
    private String procesoId;
    private Integer usuarioId;
    private Integer tecnicoId;
    private String estado; // INICIADO, EN_PROGRESO, COMPLETADO, FALLIDO
    private String resultado;
    private Map<String, String> parametros;
    private Set<String> sensoresUtilizados;
    private Long registrosProcesados;
    private Long tiempoEjecucionMs;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String errores;
    private String observaciones;

    public HistorialEjecucion() {}

    public HistorialEjecucion(String solicitudId, String procesoId, Integer usuarioId, Integer tecnicoId) {
        this.solicitudId = solicitudId;
        this.procesoId = procesoId;
        this.usuarioId = usuarioId;
        this.tecnicoId = tecnicoId;
        this.estado = "INICIADO";
        this.fechaEjecucion = LocalDateTime.now();
        this.fechaInicio = LocalDateTime.now();
    }

    // Getters y Setters
    public String getSolicitudId() { return solicitudId; }
    public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }

    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public void setFechaEjecucion(LocalDateTime fechaEjecucion) { this.fechaEjecucion = fechaEjecucion; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProcesoId() { return procesoId; }
    public void setProcesoId(String procesoId) { this.procesoId = procesoId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public Integer getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Integer tecnicoId) { this.tecnicoId = tecnicoId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public Map<String, String> getParametros() { return parametros; }
    public void setParametros(Map<String, String> parametros) { this.parametros = parametros; }

    public Set<String> getSensoresUtilizados() { return sensoresUtilizados; }
    public void setSensoresUtilizados(Set<String> sensoresUtilizados) { this.sensoresUtilizados = sensoresUtilizados; }

    public Long getRegistrosProcesados() { return registrosProcesados; }
    public void setRegistrosProcesados(Long registrosProcesados) { this.registrosProcesados = registrosProcesados; }

    public Long getTiempoEjecucionMs() { return tiempoEjecucionMs; }
    public void setTiempoEjecucionMs(Long tiempoEjecucionMs) { this.tiempoEjecucionMs = tiempoEjecucionMs; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getErrores() { return errores; }
    public void setErrores(String errores) { this.errores = errores; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
