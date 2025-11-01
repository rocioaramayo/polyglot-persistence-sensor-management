package modelo;

import java.time.LocalDateTime;
import java.util.List;

public class Grupo {
    private String id; // ObjectId de MongoDB
    private String nombre;
    private String descripcion;
    private List<String> miembros; // IDs de usuarios
    private List<String> administradores; // IDs de usuarios administradores
    private LocalDateTime fechaCreacion;
    private Boolean activo;

    public Grupo() {}

    public Grupo(String nombre, String descripcion, List<String> miembros) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.miembros = miembros;
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<String> getMiembros() { return miembros; }
    public void setMiembros(List<String> miembros) { this.miembros = miembros; }

    public List<String> getAdministradores() { return administradores; }
    public void setAdministradores(List<String> administradores) { this.administradores = administradores; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
