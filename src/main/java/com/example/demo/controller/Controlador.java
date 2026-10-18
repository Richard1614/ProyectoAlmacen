package com.example.demo.controller;


import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.MateriaPrimaDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.entidad.MateriaPrima;
import com.example.demo.service.Servicio;


@RestController
@RequestMapping("/api/usuario")
@CrossOrigin("*")
public class Controlador {
	private final Servicio servicio;
	
	public Controlador(Servicio servicio) {
		this.servicio = servicio;
	}
	
	@PostMapping("/iniciar")
	public String iniciar(@RequestBody UsuarioDTO dto) {
		return servicio.IniciarSecion(dto);
	}
	
	@PostMapping("/valida")
    public String validar_token_datos(@RequestBody UsuarioDTO dto) {
        return servicio.Validar_tonek_y_datos(dto);
    }
	
	@PostMapping("/valida_id_usuario")
    public String validar_token_datos_id_usuario(@RequestBody UsuarioDTO dto) {
        return servicio.Validar_tonek_y_idusuario(dto);
    }
	
	@PostMapping("/valida_token")
    public String validar_token_(@RequestBody UsuarioDTO dto) {
        return servicio.Validar_token(dto);
    }
	
	@PostMapping("/valida_rol")
    public String validar_token_y_rol(@RequestBody UsuarioDTO dto) {
        return servicio.Validar_rol(dto);
    }
	
	@PostMapping("/registrar_materia_prima")
	public String registrar_materia_prima(@RequestBody MateriaPrimaDTO dto) {
		return servicio.Registrar_Materia_Prima(dto);
	}
	
	@GetMapping("/materias-primas")
	  public List<MateriaPrima> listarMateriasPrimas() {
	      return servicio.listarMateriasPrimas();
	}
	
	@PostMapping("/registrar")
	  public Map<String, String> registrarUsuario(@RequestBody UsuarioDTO dto) {
	      String mensaje = servicio.registrarUsuario(dto);
	      if ("OK".equals(mensaje)) {
	          return Map.of("status", "ok");
	      } else {
	          return Map.of("status", "error", "message", mensaje);
	      }
	  }

	  @GetMapping("/listar")
	  public List<Object[]> listarUsuarios() {
	      return servicio.listarUsuarios();
	  }
	  
	  @PostMapping("/cambiar-estado")
	  public String cambiarEstado(@RequestBody UsuarioDTO dto) {
	      return servicio.cambiarEstadoUsuario(dto.getIdUsuario());
	  }
	
	
}
///api/usuario/iniciar
//  /registrar_materia_prima
