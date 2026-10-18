package com.example.demo.dto;

public class ProveedorDTO {
    private String op;
    private String idProveedor;
    private String nombreProveedor;
    private String ruc;
    private String telefono;
    private String direccion;
    private String correo;

    public ProveedorDTO() {}

    // Getters y Setters
    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public String getIdProveedor() { return idProveedor; }
    public void setIdProveedor(String idProveedor) { this.idProveedor = idProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}