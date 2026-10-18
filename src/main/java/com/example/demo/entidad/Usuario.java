package com.example.demo.entidad;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    private String usuario;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "correo", nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;

    @Column(name = "token")
    private Integer token;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    public Usuario() {}


    public Usuario(String usuario, String rol, String correo, String telefono, String contrasena, Integer token) {
        this.usuario = usuario;
        this.rol = rol;
        this.correo = correo;
        this.telefono = telefono;
        this.contrasena = contrasena;
        this.token = token;
    }



    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }


    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }


    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }


    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }


    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }


    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }


    public Integer getToken() {
        return token;
    }

    public void setToken(Integer token) {
        this.token = token;
    }
    
    public String getEstado() { 
    	return estado; 
    }
    
    public void setEstado(String estado) { 
    	this.estado = estado; 
    }

}