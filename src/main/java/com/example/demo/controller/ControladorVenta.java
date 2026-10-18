package com.example.demo.controller;

  import java.util.List;
  import org.springframework.http.HttpHeaders;
  import org.springframework.http.MediaType;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.*;
  import com.example.demo.dto.VentaDTO;
  import com.example.demo.service.Servicio;
  import com.example.demo.service.ServicioVenta;

  @RestController
  @RequestMapping("/api/venta")
  @CrossOrigin("*")
  public class ControladorVenta {

      private final Servicio servicio;
      private final ServicioVenta servicioVenta;

      public ControladorVenta(Servicio servicio, ServicioVenta servicioVenta) {
          this.servicio = servicio;
          this.servicioVenta = servicioVenta;
      }

      @PostMapping("/registrar")
      public String registrar(@RequestBody VentaDTO dto) {
          return servicio.registrarVenta(dto);
      }

      @GetMapping("/productos")
      public List<Object[]> listarProductos() {
          return servicio.listarProductosConPrecio();
      }

      @GetMapping("/pdf/{idVenta}")
      public ResponseEntity<byte[]> descargarPdf(@PathVariable String idVenta) throws Exception {
          byte[] pdf = servicioVenta.generarPdfVenta(idVenta);
          return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=venta_" + idVenta + ".pdf")
              .contentType(MediaType.APPLICATION_PDF)
              .body(pdf);
      }
      
      @GetMapping("/reporte")
      public List<Object[]> reporte() {
          return servicio.listarReporteVentas();
      }
  }