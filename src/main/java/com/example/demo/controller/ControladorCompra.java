package com.example.demo.controller;

  import java.util.List;
  import org.springframework.web.bind.annotation.*;
  import com.example.demo.dto.CompraDTO;
  import com.example.demo.service.Servicio;

  @RestController
  @RequestMapping("/api/compra")
  @CrossOrigin("*")
  public class ControladorCompra {
      private final Servicio servicio;

      public ControladorCompra(Servicio servicio) {
          this.servicio = servicio;
      }		

      @GetMapping("/cotizaciones-pendientes")
      public List<Object[]> cotizacionesPendientes() {
          return servicio.listarCotizacionesPendientes();
      }

      @GetMapping("/detalle")
      public List<Object[]> detalle(@RequestParam String idCotizacion) {
          return servicio.obtenerDetallePorCotizacion(idCotizacion);
      }

      @PostMapping("/registrar")
      public String registrar(@RequestBody CompraDTO dto) {
          servicio.registrarCompra(dto);
          return "OK";
      }
      
      @GetMapping("/reporte")
      public List<Object[]> reporte() {
          return servicio.listarReporteCompras();
      }
  }