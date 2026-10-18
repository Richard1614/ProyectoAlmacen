package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.CompraDTO;
import com.example.demo.dto.CotizacionDTO;
import com.example.demo.dto.MateriaPrimaDTO;
import com.example.demo.dto.ProduccionDTO;
import com.example.demo.dto.ProductoDTO;
import com.example.demo.dto.ProveedorDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.ValidacionStockDTO;
import com.example.demo.dto.VentaDTO;
import com.example.demo.entidad.MateriaPrima;
import com.example.demo.entidad.Proveedor;
import com.example.demo.repository.Repositorio;

import org.springframework.transaction.annotation.Transactional;

@Service
public class Servicio {
	private final Repositorio repositorio;
	
	public Servicio(Repositorio repositorio) {
		this.repositorio=repositorio;
	}
	
	@Transactional
	public String IniciarSecion(UsuarioDTO dto) {
		String retorno= repositorio.IniciarSecion_Re(dto);
		return retorno;
	}
	
	public String Validar_tonek_y_datos(UsuarioDTO dto) {
		String retorno= repositorio.valida_y_obtencion(dto);
		return retorno;
    }
	
	public String Validar_tonek_y_idusuario(UsuarioDTO dto) {
		String retorno= repositorio.valida_y_obtencion_idusuario(dto);
		return retorno;
    }
	
	public String Validar_token(UsuarioDTO dto) {
		String retorno= repositorio.valida_token(dto);
		return retorno;
	}
	
	public String Validar_rol(UsuarioDTO dto) {
		String retorno= repositorio.valida_y_obtener_rol(dto);
		return retorno;
	}
	
	public String Registrar_Materia_Prima(MateriaPrimaDTO dto) {
		String retorno= repositorio.Registrar_materia_Prima_RE(dto);
		return retorno;
	}
	
	public List<MateriaPrima> listarMateriasPrimas() {
	      return repositorio.listarMateriasPrimas();
	  }
	
	public void registrarProductoCompleto(ProductoDTO dto) {
	      repositorio.registrarProductoCompleto(dto);
	  }
	
	public List<Object[]> listarProductos() {
	      return repositorio.listarProductos();
	  }
	
	public String validarStockMp(ValidacionStockDTO dto) {
	      return repositorio.validarStockMp(dto);
	  }
	
	public void registrarTandaProduccion(ProduccionDTO dto) {
	      repositorio.registrarTandaProduccion(dto);
	  }
	
	 public String guardarProveedor(ProveedorDTO dto) {
	      return repositorio.administrar_Proveedor(dto);
	  }
	 
	 public List<Proveedor> listarProveedores() {
	      return repositorio.listarProveedores();
	  }
	 
	 public String registrarCotizacion(CotizacionDTO dto) {
	      return repositorio.registrarCotizacion(dto);
	  }
	 
	 public List<Object[]> listarCotizacionesPendientes() {
	      return repositorio.listarCotizacionesPendientes();
	  }

	  public List<Object[]> obtenerDetallePorCotizacion(String idCotizacion) {
	      return repositorio.obtenerDetallePorCotizacion(idCotizacion);
	  }

	  public String registrarCompra(CompraDTO dto) {
	      return repositorio.registrarCompra(dto);
	  }
	  
	  public List<Object[]> listarProductosConPrecio() {
	      return repositorio.listarProductosConPrecio();
	  }
	  
	  public String registrarVenta(VentaDTO dto) {
	      return repositorio.registrarVenta(dto);
	  }
	  
	  public List<Object[]> listarReporteVentas() {
	      return repositorio.listarReporteVentas();
	  }
	  
	  public List<Object[]> listarReporteCompras() {
	      return repositorio.listarReporteCompras();
	  }
	  
	  public List<Object[]> listarReporteCotizaciones() {
	      return repositorio.listarReporteCotizaciones();
	  }
	  
	  public List<Object[]> listarReporteProduccion() {
	      return repositorio.listarReporteProduccion();
	  }
	  
	  public String registrarUsuario(UsuarioDTO dto) {
	      return repositorio.registrarUsuario(dto);
	  }

	  public List<Object[]> listarUsuarios() {
	      return repositorio.listarUsuarios();
	  }
	  
	  public String cambiarEstadoUsuario(Integer idUsuario) {
	      return repositorio.cambiarEstadoUsuario(idUsuario);
	  }
	
	
}

//IniciarSecion