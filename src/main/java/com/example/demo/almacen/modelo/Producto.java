package com.example.demo.almacen.modelo;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name="producto")
public class Producto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_producto")
    private Long id;
    @Column(nullable=false,length=30,unique=true) private String codigo;
    @Column(nullable=false,length=160) private String nombre;
    @Column(length=300) private String descripcion;
    @Column(name="unidad_medida",nullable=false,length=30) private String unidadMedida;
    @Column(name="stock_minimo",nullable=false,precision=14,scale=3) private BigDecimal stockMinimo;
    @Column(nullable=false) private boolean activo=true;
    public Long getId(){return id;}
    public String getCodigo(){return codigo;}
    public void setCodigo(String value){codigo=value;}
    public String getNombre(){return nombre;}
    public void setNombre(String value){nombre=value;}
    public String getDescripcion(){return descripcion;}
    public void setDescripcion(String value){descripcion=value;}
    public String getUnidadMedida(){return unidadMedida;}
    public void setUnidadMedida(String value){unidadMedida=value;}
    public BigDecimal getStockMinimo(){return stockMinimo;}
    public void setStockMinimo(BigDecimal value){stockMinimo=value;}
    public boolean isActivo(){return activo;}
    public void setActivo(boolean value){activo=value;}
}
