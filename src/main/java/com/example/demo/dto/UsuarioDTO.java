package com.example.demo.dto;

public class UsuarioDTO {
    private Integer idUsuario; // 👈 CAMBIADO: de int a Integer
    private String usuario;
    private String rol;
    private String correo;
    private String telefono;
    private String contrasena;
    private Integer token;     // 👈 CAMBIADO: de int a Integer

    public UsuarioDTO() {}

    public UsuarioDTO(Integer idUsuario, String usuario, String rol, String correo, String telefono, String contrasena, Integer token) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.rol = rol;
        this.correo = correo;
        this.telefono = telefono;
        this.contrasena = contrasena;
        this.token = token;
    }

    // Getters y Setters
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public Integer getToken() { return token; }
    public void setToken(Integer token) { this.token = token; }
}
