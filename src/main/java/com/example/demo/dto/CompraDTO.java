package com.example.demo.dto;

  public class CompraDTO {
      private String idCotizacion;
      private String idProveedor;
      private Integer idUsuario;
      private String detalleJson;

      public CompraDTO() {}

      public String getIdCotizacion() { return idCotizacion; }
      public void setIdCotizacion(String idCotizacion) { this.idCotizacion = idCotizacion; }

      public String getIdProveedor() { return idProveedor; }
      public void setIdProveedor(String idProveedor) { this.idProveedor = idProveedor; }

      public Integer getIdUsuario() { return idUsuario; }
      public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

      public String getDetalleJson() { return detalleJson; }
      public void setDetalleJson(String detalleJson) { this.detalleJson = detalleJson; }
  }