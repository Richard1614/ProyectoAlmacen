package com.example.demo.dto;

  public class ProduccionDTO {
      private Integer idUsuario;
      private String productosJson;

      public ProduccionDTO() {}

      public Integer getIdUsuario() { return idUsuario; }
      public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

      public String getProductosJson() { return productosJson; }
      public void setProductosJson(String productosJson) { this.productosJson = productosJson; }
  }