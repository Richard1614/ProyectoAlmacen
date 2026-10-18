package com.example.demo.entidad;

import jakarta.persistence.*;

@Entity
@Table(name = "materia_prima")
public class MateriaPrima {

    @Id
    @Column(name = "id_materia_prima", length = 10)
    private String idMateriaPrima;

    @Column(name = "nombre_mp", nullable = false, length = 100)
    private String nombreMp;

    @Column(name = "unidad_mp", length = 30)
    private String unidadMp;

    @Column(name = "stock_mp")
    private Double stockMp;

    @Column(name = "precio_unitario")
    private Double precioUnitario;

    @Column(name = "stock_minimo")
    private Double stockMinimo;


    public MateriaPrima() {}


    public MateriaPrima(String nombreMp, String unidadMp, Double stockMp, Double precioUnitario, Double stockMinimo) {
        this.nombreMp = nombreMp;
        this.unidadMp = unidadMp;
        this.stockMp = stockMp;
        this.precioUnitario = precioUnitario;
        this.stockMinimo = stockMinimo;
    }



    public String getIdMateriaPrima() {
        return idMateriaPrima;
    }

    public void setIdMateriaPrima(String idMateriaPrima) {
        this.idMateriaPrima = idMateriaPrima;
    }


    public String getNombreMp() {
        return nombreMp;
    }

    public void setNombreMp(String nombreMp) {
        this.nombreMp = nombreMp;
    }


    public String getUnidadMp() {
        return unidadMp;
    }

    public void setUnidadMp(String unidadMp) {
        this.unidadMp = unidadMp;
    }


    public Double getStockMp() {
        return stockMp;
    }

    public void setStockMp(Double stockMp) {
        this.stockMp = stockMp;
    }


    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }


    public Double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}