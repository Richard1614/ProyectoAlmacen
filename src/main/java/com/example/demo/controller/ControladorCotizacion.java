package com.example.demo.controller;

  import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
  import com.example.demo.dto.CotizacionDTO;
  import com.example.demo.service.Servicio;
import com.example.demo.service.ServicioCorreo;

  @RestController
  @RequestMapping("/api/cotizacion")
  @CrossOrigin("*")
  public class ControladorCotizacion {
      private final Servicio servicio;

      public ControladorCotizacion(Servicio servicio) {
          this.servicio = servicio;
      }

      @Autowired
      private ServicioCorreo servicioCorreo;

      @PostMapping("/registrar")
      public String registrar(@RequestBody CotizacionDTO dto) {
          String idCotizacion = servicio.registrarCotizacion(dto);
          try {
              servicioCorreo.enviarCotizacion(idCotizacion);
          } catch (Exception e) {
              return "Cotizacion registrada pero error al enviar correo: " + e.getMessage();
          }
          return "OK";
      }
      
      @GetMapping("/reporte")
      public List<Object[]> reporte() {
          return servicio.listarReporteCotizaciones();
      }
      
      
      
  }