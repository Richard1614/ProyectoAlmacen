package com.example.demo.dto;

public class ProductoDTO {
      private String nomProducto;
      private Double precioPrd;
      private Integer stockMinimo;
      private String materialesJson;

      public ProductoDTO() {}

      public String getNomProducto() { return nomProducto; }
      public void setNomProducto(String nomProducto) { this.nomProducto = nomProducto; }

      public Double getPrecioPrd() { return precioPrd; }
      public void setPrecioPrd(Double precioPrd) { this.precioPrd = precioPrd; }

      public Integer getStockMinimo() { return stockMinimo; }
      public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

      public String getMaterialesJson() { return materialesJson; }
      public void setMaterialesJson(String materialesJson) { this.materialesJson = materialesJson; }
}