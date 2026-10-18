package com.example.demo.entidad;

  import jakarta.persistence.*;

  @Entity
  @Table(name = "proveedor")
  public class Proveedor {

      @Id
      @Column(name = "id_proveedor")
      private String idProveedor;

      @Column(name = "nombre_proveedor")
      private String nombreProveedor;

      @Column(name = "ruc")
      private String ruc;

      @Column(name = "telefono")
      private String telefono;

      @Column(name = "direccion")
      private String direccion;

      @Column(name = "correo")
      private String correo;

      public Proveedor() {}

      public String getIdProveedor() { return idProveedor; }
      public String getNombreProveedor() { return nombreProveedor; }
      public String getRuc() { return ruc; }
      public String getTelefono() { return telefono; }
      public String getDireccion() { return direccion; }
      public String getCorreo() { return correo; }
  }