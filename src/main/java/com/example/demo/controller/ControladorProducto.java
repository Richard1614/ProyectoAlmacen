package com.example.demo.controller;

  import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ProduccionDTO;
import com.example.demo.dto.ProductoDTO;
import com.example.demo.dto.ValidacionStockDTO;
import com.example.demo.service.Servicio;

  @RestController
  @RequestMapping("/api/producto")
  @CrossOrigin("*")
  public class ControladorProducto {
      private final Servicio servicio;

      public ControladorProducto(Servicio servicio) {
          this.servicio = servicio;
      }

      @PostMapping("/registrar-completo")
      public String registrarCompleto(@RequestBody ProductoDTO dto) {
          servicio.registrarProductoCompleto(dto);
          return "OK";
      }
      
      @GetMapping("/listar")
      public List<Object[]> listarProductos() {
          return servicio.listarProductos();
      }
      
      @PostMapping("/validar-stock")
      public String validarStock(@RequestBody ValidacionStockDTO dto) {
          return servicio.validarStockMp(dto);
      }
      
      @PostMapping("/registrar-tanda")
      public String registrarTanda(@RequestBody ProduccionDTO dto) {
          servicio.registrarTandaProduccion(dto);
          return "OK";
      }
  }
  