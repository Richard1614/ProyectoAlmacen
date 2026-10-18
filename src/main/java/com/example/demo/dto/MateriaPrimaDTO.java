package com.example.demo.dto;

public class MateriaPrimaDTO {
	private String op;             // 👈 AGREGADO: Para indicarle al procedimiento si es 'I', 'U' o 'D'
    private String idMateriaPrima; // Mapea VARCHAR(10)
    private String nombreMp;       // Mapea VARCHAR(100)
    private String unidadMp;       // Mapea VARCHAR(30)
    private Double stockMp;        // Mapea DECIMAL(10,2) -> Usamos Double para facilitar el manejo con JS
    private Double precioUnitario; // Mapea DECIMAL(10,2)
    private Double stockMinimo;    // Mapea DECIMAL(10,2)

    // Constructor vacío obligatorio para Jackson/Spring
    public MateriaPrimaDTO() {}

    // Constructor con todos los campos
    public MateriaPrimaDTO(String op, String idMateriaPrima, String nombreMp, String unidadMp, 
                           Double stockMp, Double precioUnitario, Double stockMinimo) {
        this.op = op;
        this.idMateriaPrima = idMateriaPrima;
        this.nombreMp = nombreMp;
        this.unidadMp = unidadMp;
        this.stockMp = stockMp;
        this.precioUnitario = precioUnitario;
        this.stockMinimo = stockMinimo;
    }

    // Getters y Setters
    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public String getIdMateriaPrima() { return idMateriaPrima; }
    public void setIdMateriaPrima(String idMateriaPrima) { this.idMateriaPrima = idMateriaPrima; }

    public String getNombreMp() { return nombreMp; }
    public void setNombreMp(String nombreMp) { this.nombreMp = nombreMp; }

    public String getUnidadMp() { return unidadMp; }
    public void setUnidadMp(String unidadMp) { this.unidadMp = unidadMp; }

    public Double getStockMp() { return stockMp; }
    public void setStockMp(Double stockMp) { this.stockMp = stockMp; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Double getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Double stockMinimo) { this.stockMinimo = stockMinimo; }
}
