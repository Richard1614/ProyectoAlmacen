package com.example.demo.repository;

import com.example.demo.dto.CompraDTO;
import com.example.demo.dto.CotizacionDTO;
import com.example.demo.dto.MateriaPrimaDTO;
import com.example.demo.dto.ProduccionDTO;
import com.example.demo.dto.ProductoDTO;
import com.example.demo.dto.ProveedorDTO;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.ValidacionStockDTO;
import com.example.demo.dto.VentaDTO;
import com.example.demo.entidad.*;
import jakarta.persistence.*;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
@Repository
public class Repositorio {
	@PersistenceContext
	private EntityManager entityManager;
	@Transactional
	public String IniciarSecion_Re(UsuarioDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("LOGIN");
		
		sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(3, Integer.class, ParameterMode.IN);
		
		sp.setParameter(1, dto.getCorreo());
		sp.setParameter(2, dto.getContrasena());
		sp.setParameter(3, dto.getToken());
		sp.execute();
		List<?> resultado= sp.getResultList();
		
		if(!resultado.isEmpty()) {
			return (String) resultado.get(0);
		}
		
		return null;
	}
	
	@Transactional
	public String valida_y_obtencion(UsuarioDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("validar_y_obtencion_nombre");
		
		sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		
		sp.setParameter(1, dto.getToken());
		sp.execute();
		List<?> resultado= sp.getResultList();
		
		if(!resultado.isEmpty()) {
			return (String) resultado.get(0);
		}
		
		return null;
	}
	
	@Transactional
	public String valida_y_obtencion_idusuario(UsuarioDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("validar_y_obtencion_id_usuario");
		
		sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		
		sp.setParameter(1, dto.getToken());
		sp.execute();
		List<?> resultado= sp.getResultList();
		
		if(!resultado.isEmpty()) {
			return String.valueOf(resultado.get(0));
		}
		
		return null;
	}
	
	@Transactional
	public String valida_token(UsuarioDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("validar_y_obtencion_token");
		
		sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		
		sp.setParameter(1, dto.getToken());
		sp.execute();
		List<?> resultado= sp.getResultList();
		
		if(!resultado.isEmpty()) {
			return String.valueOf(resultado.get(0));
		}
		
		return null;
	}
	
	@Transactional
	public String valida_y_obtener_rol(UsuarioDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("validar_y_obtencion_rol");
		
		sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		
		sp.setParameter(1, dto.getToken());
		sp.execute();
		List<?> resultado= sp.getResultList();
		
		if(!resultado.isEmpty()) {
			return String.valueOf(resultado.get(0));
		}
		
		return null;
	}
	
	@Transactional
	public String validarStockMp(ValidacionStockDTO dto) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("validar_stock_mp");
	      sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN);
	      sp.setParameter(1, dto.getIdProducto());
	      sp.setParameter(2, dto.getCantidad());
	      sp.execute();
	      List<?> resultado = sp.getResultList();
	      if(!resultado.isEmpty()) {
				return (String) resultado.get(0);
			}
			
			return null;
	  }
	
	
	@Transactional
	public String Registrar_materia_Prima_RE(MateriaPrimaDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("registrar_materia_prima");
		
		sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN); 
		sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN); 
		sp.registerStoredProcedureParameter(4, String.class, ParameterMode.IN); 
		sp.registerStoredProcedureParameter(5, Double.class, ParameterMode.IN); 
		sp.registerStoredProcedureParameter(6, Double.class, ParameterMode.IN); 
		sp.registerStoredProcedureParameter(7, Double.class, ParameterMode.IN); 
		

		sp.setParameter(1, dto.getOp());
		sp.setParameter(2, dto.getIdMateriaPrima());
		sp.setParameter(3, dto.getNombreMp());
		sp.setParameter(4, dto.getUnidadMp());
		sp.setParameter(5, dto.getStockMp());
		sp.setParameter(6, dto.getPrecioUnitario());
		sp.setParameter(7, dto.getStockMinimo());
		sp.execute();
		
		return null;
	}
	
	
	public List<MateriaPrima> listarMateriasPrimas() {
	      return entityManager.createQuery("SELECT m FROM MateriaPrima m", MateriaPrima.class).getResultList();
	  }
	
	public List<Object[]> listarProductos() {
	      return entityManager.createQuery(
	          "SELECT p.idProducto, p.nomProducto, p.stockMinimo, r.costoProduccion " +
	          "FROM Producto p JOIN Receta r ON p.idReceta = r.idReceta",
	          Object[].class
	      ).getResultList();
	  }
	
	public List<Proveedor> listarProveedores() {
	      return entityManager.createQuery("SELECT p FROM Proveedor p", Proveedor.class).getResultList();
	  }
	
	
	@Transactional
	  public void registrarProductoCompleto(ProductoDTO dto) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("registrar_producto_completo");

	      sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, Double.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(3, Integer.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);

	      sp.setParameter(1, dto.getNomProducto());
	      sp.setParameter(2, dto.getPrecioPrd());
	      sp.setParameter(3, dto.getStockMinimo());
	      sp.setParameter(4, dto.getMaterialesJson());

	      sp.execute();
	  }
	
	
	@Transactional
	  public void registrarTandaProduccion(ProduccionDTO dto) {
	      StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("registrar_tanda_produccion");
	      sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
	      sp.setParameter(1, dto.getIdUsuario());
	      sp.setParameter(2, dto.getProductosJson());
	      sp.execute();
	  }
	
	
	@Transactional
	  public String administrar_Proveedor(ProveedorDTO dto) {
		StoredProcedureQuery sp =entityManager.createStoredProcedureQuery("administrar_proveedor");
		sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);


		sp.setParameter(1, dto.getOp());
		sp.setParameter(2, dto.getIdProveedor());
		sp.setParameter(3, dto.getNombreProveedor());
		sp.setParameter(4, dto.getRuc());
		sp.setParameter(5, dto.getTelefono());
		sp.setParameter(6, dto.getDireccion());
		sp.setParameter(7, dto.getCorreo());

		sp.execute();


		String mensajeSalida = (String) sp.getOutputParameterValue(8);
		return mensajeSalida;

	}
	
	
	@Transactional
	  public String registrarCotizacion(CotizacionDTO dto) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("registrar_cotizacion");
	      sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(4, String.class, ParameterMode.OUT);
	      sp.setParameter(1, dto.getIdUsuario());
	      sp.setParameter(2, dto.getMaterialesJson());
	      sp.setParameter(3, dto.getProveedoresJson());
	      sp.execute();
	      return (String) sp.getOutputParameterValue(4);
	  }
	


	public List<Object[]> obtenerDetalleCotizacion(String idCotizacion) {
	      return entityManager.createNativeQuery(
	          "SELECT cd.id_materia_prima, mp.nombre_mp, mp.stock_minimo, mp.precio_unitario, cd.cantidad_sol, cd.precio_propi " +
	          "FROM cotizacion_detalle cd " +
	          "JOIN materia_prima mp ON cd.id_materia_prima = mp.id_materia_prima " +
	          "WHERE cd.id_cotizacion = ?1"
	      ).setParameter(1, idCotizacion).getResultList();
	  }

	  public List<Object[]> obtenerProveedoresCotizacion(String idCotizacion) {
	      return entityManager.createNativeQuery(
	          "SELECT p.nombre_proveedor, p.correo FROM cotizacion c " +
	          "JOIN proveedor p ON c.id_proveedor = p.id_proveedor " +
	          "WHERE c.id_cotizacion = ?1"
	      ).setParameter(1, idCotizacion).getResultList();
	  }
	  
	  
	  public List<Object[]> listarCotizacionesPendientes() {
	      return entityManager.createNativeQuery(
	          "SELECT c.id_cotizacion, c.id_proveedor, p.nombre_proveedor " +
	          "FROM cotizacion c JOIN proveedor p ON c.id_proveedor = p.id_proveedor " +
	          "WHERE c.estado = 'ENVIADO'"
	      ).getResultList();
	  }

	  public List<Object[]> obtenerDetallePorCotizacion(String idCotizacion) {
	      return entityManager.createNativeQuery(
	          "SELECT cd.id_materia_prima, mp.nombre_mp, mp.stock_minimo, mp.precio_unitario, cd.cantidad_sol, cd.precio_propi " +
	          "FROM cotizacion_detalle cd " +
	          "JOIN materia_prima mp ON cd.id_materia_prima = mp.id_materia_prima " +
	          "WHERE cd.id_cotizacion = ?1"
	      ).setParameter(1, idCotizacion).getResultList();
	  }

	  @Transactional
	  public String registrarCompra(CompraDTO dto) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("registrar_compra");
	      sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(3, Integer.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);
	      sp.setParameter(1, dto.getIdCotizacion());
	      sp.setParameter(2, dto.getIdProveedor());
	      sp.setParameter(3, dto.getIdUsuario());
	      sp.setParameter(4, dto.getDetalleJson());
	      sp.execute();
	      return (String) sp.getOutputParameterValue(5);
	  }
	  
	  public List<Object[]> listarProductosConPrecio() {
	      return entityManager.createNativeQuery(
	          "SELECT id_producto, nom_producto, precio_prd, stock_prd FROM producto WHERE stock_prd > 0"
	      ).getResultList();
	  }
	  
	  @Transactional
	  public String registrarVenta(VentaDTO dto) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("registrar_venta");
	      sp.registerStoredProcedureParameter(1,  Integer.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2,  String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(3,  String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(4,  String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(5,  String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(6,  String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(7,  Double.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(8,  Double.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(9,  Double.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(10, String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(11, String.class,  ParameterMode.IN);
	      sp.registerStoredProcedureParameter(12, String.class,  ParameterMode.OUT);

	      sp.setParameter(1,  dto.getIdUsuario());
	      sp.setParameter(2,  dto.getTipoComprobante());
	      sp.setParameter(3,  dto.getTipoDocumento());
	      sp.setParameter(4,  dto.getNumDocumento());
	      sp.setParameter(5,  dto.getClienteNombre());
	      sp.setParameter(6,  dto.getClienteDireccion());
	      sp.setParameter(7,  dto.getTotalGravado().doubleValue());
	      sp.setParameter(8,  dto.getIgv().doubleValue());
	      sp.setParameter(9,  dto.getTotalPagar().doubleValue());
	      sp.setParameter(10, dto.getMetodoPago());
	      sp.setParameter(11, dto.getDetalleJson());
	      sp.execute();
	      return (String) sp.getOutputParameterValue(12);
	  }
	  
	  
	  public List<Object[]> obtenerDetalleVenta(String idVenta) {
	      return entityManager.createNativeQuery(
	          "SELECT v.num_comprobante, v.tipo_comprobante, v.fecha, v.hora, " +
	          "v.cliente_nombre, v.num_documento, v.tipo_documento, v.cliente_direccion, " +
	          "v.metodo_pago, v.total_gravado, v.igv, v.total_pagar, " +
	          "vp.id_producto, p.nom_producto, vp.cantidad, vp.precio_unitario, vp.subtotal " +
	          "FROM venta v " +
	          "JOIN venta_producto vp ON v.id_venta = vp.id_venta " +
	          "JOIN producto p ON vp.id_producto = p.id_producto " +
	          "WHERE v.id_venta = ?1"
	      ).setParameter(1, idVenta).getResultList();
	  }
	  
	  public List<Object[]> listarReporteVentas() {
	      return entityManager.createNativeQuery(
	          "SELECT v.id_venta, v.num_comprobante, v.tipo_comprobante, v.fecha, " +
	          "v.cliente_nombre, v.num_documento, v.metodo_pago, v.total_pagar, v.estado, " +
	          "u.usuario AS vendedor, " +
	          "GROUP_CONCAT(p.nom_producto ORDER BY p.nom_producto SEPARATOR ', ') AS productos " +
	          "FROM venta v " +
	          "JOIN usuario u ON v.id_usuario = u.id_usuario " +
	          "JOIN venta_producto vp ON v.id_venta = vp.id_venta " +
	          "JOIN producto p ON vp.id_producto = p.id_producto " +
	          "GROUP BY v.id_venta, v.num_comprobante, v.tipo_comprobante, v.fecha, " +
	          "v.cliente_nombre, v.num_documento, v.metodo_pago, v.total_pagar, v.estado, u.usuario " +
	          "ORDER BY v.fecha DESC"
	      ).getResultList();
	  }
	  
	  public List<Object[]> listarReporteCompras() {
	      return entityManager.createNativeQuery(
	          "SELECT c.id_compra, c.id_cotizacion, p.nombre_proveedor, c.fecha_compra, " +
	          "c.total, u.usuario AS logistico, " +
	          "GROUP_CONCAT(mp.nombre_mp ORDER BY mp.nombre_mp SEPARATOR ', ') AS materiales " +
	          "FROM compra c " +
	          "JOIN proveedor p ON c.id_proveedor = p.id_proveedor " +
	          "JOIN usuario u ON c.id_usuario = u.id_usuario " +
	          "JOIN compra_detalle cd ON c.id_compra = cd.id_compra " +
	          "JOIN materia_prima mp ON cd.id_materia_prima = mp.id_materia_prima " +
	          "GROUP BY c.id_compra, c.id_cotizacion, p.nombre_proveedor, c.fecha_compra, c.total, u.usuario " +
	          "ORDER BY c.fecha_compra DESC"
	      ).getResultList();
	  }
	  
	  public List<Object[]> listarReporteCotizaciones() {
	      return entityManager.createNativeQuery(
	          "SELECT c.id_cotizacion, p.nombre_proveedor, c.fecha_solicitud, " +
	          "c.costo_total, c.estado, u.usuario AS logistico, " +
	          "GROUP_CONCAT(mp.nombre_mp ORDER BY mp.nombre_mp SEPARATOR ', ') AS materiales " +
	          "FROM cotizacion c " +
	          "JOIN proveedor p ON c.id_proveedor = p.id_proveedor " +
	          "JOIN usuario u ON c.id_usuario = u.id_usuario " +
	          "JOIN cotizacion_detalle cd ON c.id_cotizacion = cd.id_cotizacion " +
	          "JOIN materia_prima mp ON cd.id_materia_prima = mp.id_materia_prima " +
	          "GROUP BY c.id_cotizacion, p.nombre_proveedor, c.fecha_solicitud, c.costo_total, c.estado, u.usuario " +
	          "ORDER BY c.fecha_solicitud DESC"
	      ).getResultList();
	  }
	  
	  public List<Object[]> listarReporteProduccion() {
	      return entityManager.createNativeQuery(
	          "SELECT pr.id_produccion, pr.fecha_produccion, " +
	          "p.nom_producto, pr.cantidad_fabricada, pr.costo_total, " +
	          "u.usuario AS logistico " +
	          "FROM produccion pr " +
	          "JOIN producto p ON pr.id_producto = p.id_producto " +
	          "JOIN usuario u ON pr.id_usuario = u.id_usuario " +
	          "ORDER BY pr.fecha_produccion DESC, pr.id_produccion"
	      ).getResultList();
	  }
	  
	  @Transactional
	  public String registrarUsuario(UsuarioDTO dto) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("registrar_usuario");
	      sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(5, String.class, ParameterMode.OUT);
	      sp.setParameter(1, dto.getUsuario());
	      sp.setParameter(2, dto.getRol());
	      sp.setParameter(3, dto.getTelefono());
	      sp.setParameter(4, dto.getContrasena());
	      sp.execute();
	      return (String) sp.getOutputParameterValue(5);
	  }

	  public List<Object[]> listarUsuarios() {
	      return entityManager.createNativeQuery(
	          "SELECT id_usuario, usuario, rol, correo, telefono, estado FROM usuario ORDER BY rol, usuario"
	      ).getResultList();
	  }

	  @Transactional
	  public String cambiarEstadoUsuario(Integer idUsuario) {
	      StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("cambiar_estado_usuario");
	      sp.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
	      sp.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT);
	      sp.setParameter(1, idUsuario);
	      sp.execute();
	      return (String) sp.getOutputParameterValue(2);
	  }
	
}


