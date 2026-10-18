package com.example.demo.dto;

  public class VentaDTO {

      private int idUsuario;
      private String tipoComprobante;
      private String tipoDocumento;
      private String numDocumento;
      private String clienteNombre;
      private String clienteDireccion;
      private Double totalGravado;
      private Double igv;
      private Double totalPagar;
      private String metodoPago;
      private String detalleJson;

      // Constructor vacío obligatorio para Jackson/Spring
      public VentaDTO() {}

      // Getters y Setters
      public int getIdUsuario() { return idUsuario; }
      public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

      public String getTipoComprobante() { return tipoComprobante; }
      public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

      public String getTipoDocumento() { return tipoDocumento; }
      public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

      public String getNumDocumento() { return numDocumento; }
      public void setNumDocumento(String numDocumento) { this.numDocumento = numDocumento; }

      public String getClienteNombre() { return clienteNombre; }
      public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

      public String getClienteDireccion() { return clienteDireccion; }
      public void setClienteDireccion(String clienteDireccion) { this.clienteDireccion = clienteDireccion; }

      public Double getTotalGravado() { return totalGravado; }
      public void setTotalGravado(Double totalGravado) { this.totalGravado = totalGravado; }

      public Double getIgv() { return igv; }
      public void setIgv(Double igv) { this.igv = igv; }

      public Double getTotalPagar() { return totalPagar; }
      public void setTotalPagar(Double totalPagar) { this.totalPagar = totalPagar; }

      public String getMetodoPago() { return metodoPago; }
      public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

      public String getDetalleJson() { return detalleJson; }
      public void setDetalleJson(String detalleJson) { this.detalleJson = detalleJson; }
  }