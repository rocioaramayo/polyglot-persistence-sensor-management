package modelo;

import java.time.LocalDateTime;
import java.util.List;

public class Rol {
    private String id; // ObjectId de MongoDB
    private String nombre; // ADMINISTRADOR, TECNICO, USUARIO
    private String descripcion;
    private List<String> permisos;
    private LocalDateTime fechaCreacion;

    public Rol() {}

    public Rol(String nombre, String descripcion, List<String> permisos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.permisos = permisos;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<String> getPermisos() { return permisos; }
    public void setPermisos(List<String> permisos) { this.permisos = permisos; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
