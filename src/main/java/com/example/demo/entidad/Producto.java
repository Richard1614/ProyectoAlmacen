package com.example.demo.entidad;

  import jakarta.persistence.*;

  @Entity
  @Table(name = "producto")
  public class Producto {

      @Id
      @Column(name = "id_producto")
      private String idProducto;

      @Column(name = "nom_producto")
      private String nomProducto;

      @Column(name = "precio_prd")
      private Double precioPrd;

      @Column(name = "stock_minimo")
      private Integer stockMinimo;

      @Column(name = "id_receta")
      private String idReceta;

      public Producto() {}

      public String getIdProducto() { return idProducto; }
      public String getNomProducto() { return nomProducto; }
      public Double getPrecioPrd() { return precioPrd; }
      public Integer getStockMinimo() { return stockMinimo; }
      public String getIdReceta() { return idReceta; }
  }