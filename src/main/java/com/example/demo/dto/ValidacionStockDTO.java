 package com.example.demo.dto;

public class ValidacionStockDTO {
      private String idProducto;
      private Integer cantidad;

      public ValidacionStockDTO() {}

      public String getIdProducto() { return idProducto; }
      public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

      public Integer getCantidad() { return cantidad; }
      public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
 }