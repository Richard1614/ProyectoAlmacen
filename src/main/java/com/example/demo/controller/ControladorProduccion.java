package com.example.demo.controller;

  import java.util.List;
  import org.springframework.web.bind.annotation.*;
  import com.example.demo.service.Servicio;

  @RestController
  @RequestMapping("/api/produccion")
  @CrossOrigin("*")
  public class ControladorProduccion {

      private final Servicio servicio;

      public ControladorProduccion(Servicio servicio) {
          this.servicio = servicio;
      }

      @GetMapping("/reporte")
      public List<Object[]> reporte() {
          return servicio.listarReporteProduccion();
      }
  }