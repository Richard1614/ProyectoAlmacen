package com.example.demo.dto;

  public class CotizacionDTO {
      private Integer idUsuario;
      private String materialesJson;
      private String proveedoresJson;

      public CotizacionDTO() {}

      public Integer getIdUsuario() { return idUsuario; }
      public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

      public String getMaterialesJson() { return materialesJson; }
      public void setMaterialesJson(String materialesJson) { this.materialesJson = materialesJson; }

      public String getProveedoresJson() { return proveedoresJson; }
      public void setProveedoresJson(String proveedoresJson) { this.proveedoresJson = proveedoresJson; }
  }