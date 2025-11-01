package modelo;

import java.time.LocalDateTime;

public class Usuario {
    private String id; // MongoDB usa ObjectId como String
    private String nombre;
    private String apellido; // Agregado apellido
    private String email;
    private String passwordHash;
    private String rol; // Campo directo: ADMINISTRADOR, TECNICO, USUARIO
    private Boolean activo; // Cambiado de estado String a Boolean
    private LocalDateTime fechaRegistro;

    public Usuario() {
        this.activo = true; // Por defecto activo
        this.fechaRegistro = LocalDateTime.now(); // Fecha automática
    }

    public Usuario(String nombre, String apellido, String email, String passwordHash, String rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = true;
        this.fechaRegistro = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
