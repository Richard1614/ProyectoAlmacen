package com.example.demo.entidad;

  import jakarta.persistence.*;

  @Entity
  @Table(name = "receta")
  public class Receta {

      @Id
      @Column(name = "id_receta")
      private String idReceta;

      @Column(name = "costo_produccion")
      private Double costoProduccion;

      public Receta() {}

      public String getIdReceta() { return idReceta; }
      public Double getCostoProduccion() { return costoProduccion; }
  }