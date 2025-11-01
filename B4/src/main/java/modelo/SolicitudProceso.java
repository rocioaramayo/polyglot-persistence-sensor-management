package modelo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

public class SolicitudProceso {
    private String id; // MongoDB usa ObjectId como String
    private String usuarioId; // Referencia al usuario en MongoDB
    private String procesoId; // Referencia al proceso en MongoDB
    private String tecnicoAsignadoId; // Agregado para asignar técnico
    private LocalDateTime fechaSolicitud;
    private String estado; // pendiente, aprobado, en_ejecucion, completado, rechazado
    private Map<String, Object> parametros; // Cambiado de String a Map para facilitar manejo
    private String resultado; // Resultado del proceso ejecutado

    public SolicitudProceso() {
        this.parametros = new HashMap<>();
    }

    public SolicitudProceso(String usuarioId, String procesoId, Map<String, Object> parametros) {
        this.usuarioId = usuarioId;
        this.procesoId = procesoId;
        this.parametros = parametros != null ? parametros : new HashMap<>();
        this.estado = "pendiente";
        this.fechaSolicitud = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getProcesoId() { return procesoId; }
    public void setProcesoId(String procesoId) { this.procesoId = procesoId; }

    public String getTecnicoAsignadoId() { return tecnicoAsignadoId; }
    public void setTecnicoAsignadoId(String tecnicoAsignadoId) { this.tecnicoAsignadoId = tecnicoAsignadoId; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Map<String, Object> getParametros() { return parametros; }
    public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
}
