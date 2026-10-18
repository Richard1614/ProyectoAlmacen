package com.example.demo.controller;

  import org.springframework.web.bind.annotation.*;
  import com.example.demo.dto.ProveedorDTO;
  import com.example.demo.service.Servicio;
  import com.example.demo.entidad.*;

import java.util.List;
import java.util.Map;

  @RestController
  @RequestMapping("/api/proveedor")
  @CrossOrigin("*")
  public class ControladorProveedor {
      private final Servicio servicio;

      public ControladorProveedor(Servicio servicio) {
          this.servicio = servicio;
      }

      @PostMapping("/guardar")
      public Map<String, String> guardar(@RequestBody ProveedorDTO dto) {
          String mensaje = servicio.guardarProveedor(dto);
          if ("OK".equals(mensaje)) {
              return Map.of("status", "ok");
          } else {
              return Map.of("status", "error", "message", mensaje);
          }
      }
      
      @GetMapping("/listar")
      public List<Proveedor> listarProveedores() {
          return servicio.listarProveedores();
      }
  }