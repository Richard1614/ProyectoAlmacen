package com.example.demo.almacen.servicio;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AlmacenService {
    private static final List<String> MIME_PERMITIDOS = List.of("image/jpeg", "image/png", "application/pdf");
    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcTemplate simpleJdbc;
    private final Path evidenceDirectory;
    private final PasswordEncoder passwordEncoder;
    private final com.example.demo.almacen.repositorio.ProductoRepository productosRepository;

    public AlmacenService(NamedParameterJdbcTemplate jdbc, JdbcTemplate simpleJdbc,
            PasswordEncoder passwordEncoder, @Value("${app.evidence.directory}") String evidenceDirectory,
            com.example.demo.almacen.repositorio.ProductoRepository productosRepository) {
        this.jdbc = jdbc;
        this.simpleJdbc = simpleJdbc;
        this.passwordEncoder = passwordEncoder;
        this.productosRepository = productosRepository;
        this.evidenceDirectory = Path.of(evidenceDirectory).toAbsolutePath().normalize();
    }

    public long usuarioId(String correo) {
        Long id = simpleJdbc.queryForObject("SELECT id_usuario FROM usuario WHERE correo=? AND activo=TRUE", Long.class, correo);
        if (id == null) throw new IllegalArgumentException("Usuario no encontrado");
        return id;
    }

    public Map<String,Object> sesion(String correo) {
        Map<String,Object> sesion = simpleJdbc.queryForMap("SELECT u.id_usuario,u.nombre,u.correo,r.nombre AS rol FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.correo=?", correo);
        sesion.put("permisos", simpleJdbc.queryForList("SELECT p.codigo FROM usuario u JOIN rol_permiso rp ON rp.id_rol=u.id_rol JOIN permiso p ON p.id_permiso=rp.id_permiso WHERE u.correo=? ORDER BY p.codigo", String.class, correo));
        return sesion;
    }

    public List<Map<String,Object>> usuarios() {
        return simpleJdbc.queryForList("SELECT u.id_usuario,u.nombre,u.correo,u.telefono,u.activo,r.nombre AS rol,u.creado_en FROM usuario u JOIN rol r ON r.id_rol=u.id_rol ORDER BY u.nombre");
    }

    public List<Map<String,Object>> rolesPermisos() {
        return simpleJdbc.queryForList("SELECT r.id_rol,r.nombre AS rol,p.id_permiso,p.codigo AS permiso,p.descripcion,(rp.id_permiso IS NOT NULL) AS asignado FROM rol r CROSS JOIN permiso p LEFT JOIN rol_permiso rp ON rp.id_rol=r.id_rol AND rp.id_permiso=p.id_permiso ORDER BY r.nombre,p.codigo");
    }

    @Transactional
    public void cambiarRolUsuario(long id, String nombreRol, long autorId) {
        String rolActual = simpleJdbc.queryForObject("SELECT r.nombre FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=? FOR UPDATE", String.class, id);
        Long rolNuevo = simpleJdbc.queryForObject("SELECT id_rol FROM rol WHERE nombre=?", Long.class, nombreRol);
        if ("ADMINISTRADOR".equals(rolActual) && !"ADMINISTRADOR".equals(nombreRol)) {
            Integer administradores = simpleJdbc.queryForObject("SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.activo=TRUE AND r.nombre='ADMINISTRADOR' AND u.id_usuario<>?", Integer.class, id);
            if (administradores == null || administradores == 0) throw new IllegalArgumentException("Debe quedar al menos un administrador activo");
        }
        simpleJdbc.update("UPDATE usuario SET id_rol=? WHERE id_usuario=?", rolNuevo, id);
        auditar(autorId, "CAMBIAR_ROL_USUARIO", "USUARIO", id, "{\"rol\":\"" + nombreRol + "\"}");
    }

    @Transactional
    public void actualizarPermisosRol(long rolId, List<Long> permisos, long autorId) {
        String nombreRol = simpleJdbc.queryForObject("SELECT nombre FROM rol WHERE id_rol=? FOR UPDATE", String.class, rolId);
        List<Long> ids = permisos == null ? List.of() : permisos.stream().distinct().toList();
        if (permisos != null && ids.size() != permisos.size()) throw new IllegalArgumentException("La lista contiene permisos repetidos");
        if ("ADMINISTRADOR".equals(nombreRol)) {
            Long permisoAdmin = simpleJdbc.queryForObject("SELECT id_permiso FROM permiso WHERE codigo='USUARIOS_ADMINISTRAR'", Long.class);
            if (!ids.contains(permisoAdmin)) throw new IllegalArgumentException("El rol Administrador debe conservar el permiso de administración de usuarios");
        }
        if (!ids.isEmpty()) {
            List<Long> encontrados = jdbc.queryForList("SELECT id_permiso FROM permiso WHERE id_permiso IN (:ids)", new MapSqlParameterSource("ids", ids), Long.class);
            if (encontrados.size() != ids.size()) throw new IllegalArgumentException("Uno o más permisos no existen");
        }
        simpleJdbc.update("DELETE FROM rol_permiso WHERE id_rol=?", rolId);
        for (Long permisoId : ids) simpleJdbc.update("INSERT INTO rol_permiso(id_rol,id_permiso) VALUES(?,?)", rolId, permisoId);
        auditar(autorId, "ACTUALIZAR_PERMISOS_ROL", "ROL", rolId, "{\"permisos\":" + ids.size() + "}");
    }

    @Transactional
    public long crearUsuario(String nombre, String correo, String telefono, String password, String rol, long autorId) {
        Long rolId = simpleJdbc.queryForObject("SELECT id_rol FROM rol WHERE nombre=?", Long.class, rol);
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO usuario(id_rol,nombre,correo,telefono,password_hash) VALUES(:rol,:nombre,:correo,:telefono,:hash)",
            new MapSqlParameterSource().addValue("rol",rolId).addValue("nombre",nombre).addValue("correo",correo.toLowerCase())
                .addValue("telefono",telefono).addValue("hash",passwordEncoder.encode(password)), key, new String[]{"id_usuario"});
        long id=key.getKey().longValue();
        auditar(autorId,"CREAR_USUARIO","USUARIO",id,"{\"rol\":\""+rol+"\"}");
        return id;
    }

    @Transactional
    public void cambiarEstadoUsuario(long id, boolean activo, long autorId) {
        if (!activo) {
            String rol = simpleJdbc.queryForObject("SELECT r.nombre FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=? FOR UPDATE", String.class, id);
            if ("ADMINISTRADOR".equals(rol)) {
                Integer administradores = simpleJdbc.queryForObject("SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.activo=TRUE AND r.nombre='ADMINISTRADOR' AND u.id_usuario<>?", Integer.class, id);
                if (administradores == null || administradores == 0) throw new IllegalArgumentException("Debe quedar al menos un administrador activo");
            }
        }
        simpleJdbc.update("UPDATE usuario SET activo=? WHERE id_usuario=?",activo,id);
        auditar(autorId,"CAMBIAR_ESTADO_USUARIO","USUARIO",id,"{\"activo\":"+activo+"}");
    }

    public List<Map<String,Object>> proveedores() {
        return simpleJdbc.queryForList("SELECT id_proveedor,ruc,razon_social,contacto,correo,telefono,direccion,activo FROM proveedor ORDER BY razon_social");
    }

    @Transactional
    public long crearProveedor(String ruc,String razonSocial,String contacto,String correo,String telefono,String direccion,long usuarioId) {
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update("INSERT INTO proveedor(ruc,razon_social,contacto,correo,telefono,direccion) VALUES(:ruc,:razon,:contacto,:correo,:telefono,:direccion)",
            new MapSqlParameterSource().addValue("ruc",ruc).addValue("razon",razonSocial).addValue("contacto",contacto)
                .addValue("correo",correo).addValue("telefono",telefono).addValue("direccion",direccion),key,new String[]{"id_proveedor"});
        long id=key.getKey().longValue(); auditar(usuarioId,"CREAR_PROVEEDOR","PROVEEDOR",id,"{\"ruc\":\""+ruc+"\"}"); return id;
    }

    @Transactional
    public void actualizarProveedor(long id,String razonSocial,String contacto,String correo,String telefono,String direccion,boolean activo,long usuarioId) {
        int filas=simpleJdbc.update("UPDATE proveedor SET razon_social=?,contacto=?,correo=?,telefono=?,direccion=?,activo=? WHERE id_proveedor=?",razonSocial,contacto,correo,telefono,direccion,activo,id);
        if(filas==0) throw new IllegalArgumentException("Proveedor no encontrado");
        auditar(usuarioId,"ACTUALIZAR_PROVEEDOR","PROVEEDOR",id,"{\"activo\":"+activo+"}");
    }

    public List<Map<String,Object>> almacenes() { return simpleJdbc.queryForList("SELECT id_almacen,codigo,nombre,ubicacion,activo FROM almacen ORDER BY nombre"); }

    public List<Map<String,Object>> ordenesCompra() {
        return simpleJdbc.queryForList("SELECT oc.id_orden_compra,oc.numero,oc.id_proveedor,p.razon_social,oc.estado,oc.fecha_emision,oc.moneda,oc.total,oc.observacion,oc.version,u.nombre AS creada_por,oc.creado_en FROM orden_compra oc JOIN proveedor p ON p.id_proveedor=oc.id_proveedor JOIN usuario u ON u.id_usuario=oc.creada_por ORDER BY oc.creado_en DESC");
    }

    public List<Map<String,Object>> detalleOrdenCompra(long id) {
        return simpleJdbc.queryForList("SELECT d.id_detalle_oc,d.id_producto,p.codigo,p.nombre,d.cantidad,d.precio_unitario,d.subtotal FROM detalle_orden_compra d JOIN producto p ON p.id_producto=d.id_producto WHERE d.id_orden_compra=? ORDER BY p.nombre", id);
    }

    public List<Map<String,Object>> productos() {
        return simpleJdbc.queryForList("SELECT id_producto,codigo,nombre,descripcion,unidad_medida,stock_minimo,activo FROM producto ORDER BY nombre");
    }

    @Transactional
    public long crearProducto(String codigo, String nombre, String descripcion, String unidad, BigDecimal stockMinimo, long usuarioId) {
        var producto = new com.example.demo.almacen.modelo.Producto();
        producto.setCodigo(codigo); producto.setNombre(nombre); producto.setDescripcion(descripcion);
        producto.setUnidadMedida(unidad); producto.setStockMinimo(stockMinimo);
        long id = productosRepository.saveAndFlush(producto).getId();
        auditar(usuarioId, "CREAR_PRODUCTO", "PRODUCTO", id, "{\"codigo\":\"" + codigo.replace("\"", "") + "\"}");
        return id;
    }

    @Transactional
    public void actualizarProducto(long id,String nombre,String descripcion,String unidad,BigDecimal stockMinimo,boolean activo,long usuarioId) {
        var producto=productosRepository.findById(id).orElseThrow(()->new IllegalArgumentException("Producto no encontrado"));
        producto.setNombre(nombre); producto.setDescripcion(descripcion); producto.setUnidadMedida(unidad);
        producto.setStockMinimo(stockMinimo); producto.setActivo(activo);
        productosRepository.saveAndFlush(producto);
        auditar(usuarioId,"ACTUALIZAR_PRODUCTO","PRODUCTO",id,"{\"activo\":"+activo+"}");
    }

    public List<Map<String,Object>> inventario(Long almacenId) {
        String sql = "SELECT i.id_inventario,p.codigo,p.nombre,a.codigo AS almacen,COALESCE(i.stock_actual,0) AS stock_actual,p.stock_minimo," +
            "CASE WHEN COALESCE(i.stock_actual,0)=0 THEN 'AGOTADO' WHEN i.stock_actual<=p.stock_minimo THEN 'BAJO' ELSE 'NORMAL' END nivel " +
            "FROM producto p CROSS JOIN almacen a LEFT JOIN inventario i ON i.id_producto=p.id_producto AND i.id_almacen=a.id_almacen " +
            "WHERE p.activo=TRUE AND a.activo=TRUE AND (:almacen IS NULL OR a.id_almacen=:almacen) ORDER BY p.nombre";
        return jdbc.queryForList(sql, new MapSqlParameterSource().addValue("almacen", almacenId));
    }

    @Transactional
    public long crearOrdenCompra(long proveedorId, LocalDate fecha, String moneda, String observacion,
            List<LineaCompra> lineas, long usuarioId) {
        if (lineas == null || lineas.isEmpty()) throw new IllegalArgumentException("La orden requiere al menos un producto");
        String numero = "OC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO orden_compra(numero,id_proveedor,creada_por,estado,fecha_emision,moneda,observacion) " +
                "VALUES(:numero,:proveedor,:usuario,'PENDIENTE_APROBACION',:fecha,:moneda,:observacion)",
            new MapSqlParameterSource().addValue("numero", numero).addValue("proveedor", proveedorId).addValue("usuario", usuarioId)
                .addValue("fecha", fecha).addValue("moneda", moneda).addValue("observacion", observacion), key, new String[]{"id_orden_compra"});
        long id = key.getKey().longValue();
        for (LineaCompra linea : lineas) {
            jdbc.update("INSERT INTO detalle_orden_compra(id_orden_compra,id_producto,cantidad,precio_unitario) VALUES(:oc,:producto,:cantidad,:precio)",
                Map.of("oc", id, "producto", linea.productoId(), "cantidad", linea.cantidad(), "precio", linea.precioUnitario()));
        }
        auditar(usuarioId, "CREAR_OC", "ORDEN_COMPRA", id, "{\"numero\":\"" + numero + "\"}");
        return id;
    }

    @Transactional
    public void actualizarOrdenCompra(long ordenId,long proveedorId,LocalDate fecha,String moneda,String observacion,List<LineaCompra> lineas,long usuarioId) {
        String estado=simpleJdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=? FOR UPDATE",String.class,ordenId);
        if(!"REABIERTA".equals(estado)) throw new IllegalArgumentException("Solo se puede modificar una OC reabierta");
        if(lineas==null||lineas.isEmpty()) throw new IllegalArgumentException("La orden requiere al menos un producto");
        guardarVersionOrden(ordenId,usuarioId);
        simpleJdbc.update("DELETE FROM detalle_orden_compra WHERE id_orden_compra=?",ordenId);
        simpleJdbc.update("UPDATE orden_compra SET id_proveedor=?,fecha_emision=?,moneda=?,observacion=?,estado=? WHERE id_orden_compra=?",proveedorId,fecha,moneda,observacion,"PENDIENTE_APROBACION",ordenId);
        for(LineaCompra linea:lineas) simpleJdbc.update("INSERT INTO detalle_orden_compra(id_orden_compra,id_producto,cantidad,precio_unitario) VALUES(?,?,?,?)",ordenId,linea.productoId(),linea.cantidad(),linea.precioUnitario());
        auditar(usuarioId,"ACTUALIZAR_OC","ORDEN_COMPRA",ordenId,"{\"requiereNuevaAprobacion\":"+true+"}");
    }

    @Transactional
    public void decidirOc(long ordenId, long usuarioId, String decision, String comentario) {
        simpleJdbc.update("CALL sp_decidir_oc(?,?,?,?)", ordenId, usuarioId, decision, comentario);
    }

    @Transactional
    public long crearRecepcion(long ordenId, long almacenId, String guia, String observacion,
            List<LineaRecepcion> lineas, long usuarioId) {
        if (lineas == null || lineas.isEmpty()) throw new IllegalArgumentException("La recepcion requiere detalle");
        String estado=simpleJdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=? FOR UPDATE",String.class,ordenId);
        if(!List.of("APROBADA","PARCIAL").contains(estado)) throw new IllegalArgumentException("La OC no está aprobada");
        String numero = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO recepcion(numero,id_orden_compra,id_almacen,guia_remision,recibida_por,observacion) " +
                "VALUES(:numero,:oc,:almacen,:guia,:usuario,:observacion)",
            new MapSqlParameterSource().addValue("numero", numero).addValue("oc", ordenId).addValue("almacen", almacenId)
                .addValue("guia", guia).addValue("usuario", usuarioId).addValue("observacion", observacion), key, new String[]{"id_recepcion"});
        long id = key.getKey().longValue();
        for (LineaRecepcion linea : lineas) {
            jdbc.update("INSERT INTO detalle_recepcion(id_recepcion,id_producto,cantidad_recibida,cantidad_conforme,cantidad_rechazada) " +
                    "VALUES(:recepcion,:producto,:recibida,:conforme,:rechazada)",
                Map.of("recepcion", id, "producto", linea.productoId(), "recibida", linea.recibida(), "conforme", linea.conforme(), "rechazada", linea.rechazada()));
        }
        auditar(usuarioId,"CREAR_RECEPCION","RECEPCION",id,"{}");
        return id;
    }

    @Transactional
    public void confirmarRecepcion(long id, long usuarioId) { simpleJdbc.update("CALL sp_confirmar_recepcion(?,?)", id, usuarioId); }

    public List<Map<String,Object>> recepciones() {
        return simpleJdbc.queryForList("SELECT r.id_recepcion,r.numero,oc.numero AS orden_compra,a.nombre AS almacen,r.guia_remision,r.estado,r.fecha_recepcion,u.nombre AS recibida_por FROM recepcion r JOIN orden_compra oc ON oc.id_orden_compra=r.id_orden_compra JOIN almacen a ON a.id_almacen=r.id_almacen JOIN usuario u ON u.id_usuario=r.recibida_por ORDER BY r.fecha_recepcion DESC");
    }

    public List<Map<String,Object>> detalleRecepcion(long id) {
        return simpleJdbc.queryForList("SELECT d.id_detalle_recepcion,d.id_producto,p.codigo,p.nombre,d.cantidad_recibida,d.cantidad_conforme,d.cantidad_rechazada FROM detalle_recepcion d JOIN producto p ON p.id_producto=d.id_producto WHERE d.id_recepcion=? ORDER BY p.nombre", id);
    }

    @Transactional
    public long registrarIncidencia(long recepcionId, Long productoId, String tipo, String descripcion, String accion, long usuarioId) {
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update("INSERT INTO incidencia_recepcion(id_recepcion,id_producto,tipo,descripcion,accion,registrada_por) VALUES(:recepcion,:producto,:tipo,:descripcion,:accion,:usuario)",
            new MapSqlParameterSource().addValue("recepcion",recepcionId).addValue("producto",productoId).addValue("tipo",tipo)
                .addValue("descripcion",descripcion).addValue("accion",accion).addValue("usuario",usuarioId),key,new String[]{"id_incidencia"});
        long id=key.getKey().longValue();
        auditar(usuarioId,"REGISTRAR_INCIDENCIA","INCIDENCIA_RECEPCION",id,"{}");
        return id;
    }

    public List<Map<String,Object>> incidenciasRecepcion() {
        return simpleJdbc.queryForList("SELECT i.id_incidencia,r.numero AS recepcion,p.codigo AS producto,i.tipo,i.descripcion,i.accion,i.estado,u.nombre AS registrada_por,i.creado_en FROM incidencia_recepcion i JOIN recepcion r ON r.id_recepcion=i.id_recepcion LEFT JOIN producto p ON p.id_producto=i.id_producto JOIN usuario u ON u.id_usuario=i.registrada_por ORDER BY i.creado_en DESC");
    }

    @Transactional
    public void resolverIncidencia(long id,String estado,String accion,long usuarioId) {
        if(!List.of("ABIERTA","EN_GESTION","RESUELTA").contains(estado)) throw new IllegalArgumentException("Estado de incidencia inválido");
        int filas=simpleJdbc.update("UPDATE incidencia_recepcion SET estado=?,accion=? WHERE id_incidencia=?",estado,accion,id);
        if(filas==0) throw new IllegalArgumentException("Incidencia no encontrada");
        auditar(usuarioId,"ACTUALIZAR_INCIDENCIA","INCIDENCIA_RECEPCION",id,"{\"estado\":\""+estado+"\",\"accion\":\""+accion+"\"}");
    }

    @Transactional
    public long crearRequerimiento(String placa, String tipo, String prioridad, String motivo,
            List<LineaRequerimiento> lineas, long usuarioId) {
        if (lineas == null || lineas.isEmpty()) throw new IllegalArgumentException("El requerimiento requiere detalle");
        String numero = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO requerimiento(numero,solicitado_por,placa_unidad,tipo_mantenimiento,prioridad,estado,motivo) " +
                "VALUES(:numero,:usuario,:placa,:tipo,:prioridad,'ENVIADO',:motivo)",
            new MapSqlParameterSource().addValue("numero",numero).addValue("usuario",usuarioId).addValue("placa",placa)
                .addValue("tipo",tipo).addValue("prioridad",prioridad).addValue("motivo",motivo), key, new String[]{"id_requerimiento"});
        long id = key.getKey().longValue();
        for (LineaRequerimiento linea : lineas) {
            jdbc.update("INSERT INTO detalle_requerimiento(id_requerimiento,id_producto,cantidad_solicitada) VALUES(:req,:producto,:cantidad)",
                Map.of("req",id,"producto",linea.productoId(),"cantidad",linea.cantidad()));
        }
        auditar(usuarioId,"CREAR_REQUERIMIENTO","REQUERIMIENTO",id,"{}");
        return id;
    }

    public List<Map<String,Object>> requerimientos() {
        return simpleJdbc.queryForList("SELECT r.id_requerimiento,r.numero,u.nombre AS solicitado_por,r.placa_unidad,r.tipo_mantenimiento,r.prioridad,r.estado,r.motivo,r.creado_en FROM requerimiento r JOIN usuario u ON u.id_usuario=r.solicitado_por ORDER BY r.creado_en DESC");
    }

    public List<Map<String,Object>> detalleRequerimiento(long id) {
        return simpleJdbc.queryForList("SELECT d.id_detalle_requerimiento,d.id_producto,p.codigo,p.nombre,d.cantidad_solicitada,d.cantidad_atendida FROM detalle_requerimiento d JOIN producto p ON p.id_producto=d.id_producto WHERE d.id_requerimiento=? ORDER BY p.nombre", id);
    }

    @Transactional
    public long prepararSalida(long requerimientoId, long almacenId, String entregadaA, long usuarioId) {
        String estado = simpleJdbc.queryForObject("SELECT estado FROM requerimiento WHERE id_requerimiento=? FOR UPDATE",String.class,requerimientoId);
        if (!List.of("ENVIADO","ATENDIDO_PARCIAL").contains(estado)) throw new IllegalArgumentException("Requerimiento no elegible para entrega");
        Integer existentes=simpleJdbc.queryForObject("SELECT COUNT(*) FROM orden_salida WHERE id_requerimiento=? AND estado<>'ANULADA'",Integer.class,requerimientoId);
        if(existentes!=null && existentes>0) throw new IllegalArgumentException("El requerimiento ya tiene una salida vigente");
        String numero = "SAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO orden_salida(numero,id_requerimiento,id_almacen,preparada_por,entregada_a) VALUES(:numero,:req,:almacen,:usuario,:entrega)",
            new MapSqlParameterSource().addValue("numero",numero).addValue("req",requerimientoId).addValue("almacen",almacenId)
                .addValue("usuario",usuarioId).addValue("entrega",entregadaA), key, new String[]{"id_orden_salida"});
        long id=key.getKey().longValue();
        auditar(usuarioId,"PREPARAR_SALIDA","ORDEN_SALIDA",id,"{}");
        return id;
    }

    public List<Map<String,Object>> salidas() {
        return simpleJdbc.queryForList("SELECT s.id_orden_salida,s.numero,r.numero AS requerimiento,a.nombre AS almacen,u.nombre AS preparada_por,s.entregada_a,s.estado,s.fecha_preparacion,s.fecha_entrega,(SELECT COUNT(*) FROM evidencia_salida e WHERE e.id_orden_salida=s.id_orden_salida) AS evidencias FROM orden_salida s JOIN requerimiento r ON r.id_requerimiento=s.id_requerimiento JOIN almacen a ON a.id_almacen=s.id_almacen JOIN usuario u ON u.id_usuario=s.preparada_por ORDER BY s.fecha_preparacion DESC");
    }

    @Transactional
    public long guardarEvidencia(long salidaId, MultipartFile archivo, long usuarioId) throws IOException {
        String estado = simpleJdbc.queryForObject("SELECT estado FROM orden_salida WHERE id_orden_salida=? FOR UPDATE",String.class,salidaId);
        if (!"PREPARADA".equals(estado)) throw new IllegalArgumentException("Solo se adjunta evidencia antes de confirmar");
        if (archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Archivo vacio o mayor de 5 MB");
        String mime = archivo.getContentType();
        if (!MIME_PERMITIDOS.contains(mime)) throw new IllegalArgumentException("Solo se admite JPG, PNG o PDF");
        byte[] bytes = archivo.getBytes();
        validarContenido(bytes,mime);
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException("No se pudo calcular la huella del archivo", e); }
        String extension = switch (mime) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; default -> ".pdf"; };
        Files.createDirectories(evidenceDirectory);
        Path destino = evidenceDirectory.resolve(UUID.randomUUID() + extension).normalize();
        if (!destino.startsWith(evidenceDirectory)) throw new IllegalArgumentException("Ruta de evidencia invalida");
        Files.write(destino, bytes, StandardOpenOption.CREATE_NEW);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) try { Files.deleteIfExists(destino); } catch(IOException ignored) { }
                }
            });
        try {
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO evidencia_salida(id_orden_salida,nombre_original,ruta_segura,tipo_mime,tamano_bytes,sha256,cargada_por) " +
                    "VALUES(:salida,:nombre,:ruta,:mime,:tamano,:sha,:usuario)",
                new MapSqlParameterSource().addValue("salida",salidaId).addValue("nombre",nombreSeguro(archivo.getOriginalFilename()))
                    .addValue("ruta",destino.toString()).addValue("mime",mime).addValue("tamano",bytes.length).addValue("sha",hash).addValue("usuario",usuarioId),
                key, new String[]{"id_evidencia"});
            long id=key.getKey().longValue();
            auditar(usuarioId,"CARGAR_EVIDENCIA","EVIDENCIA_SALIDA",id,"{}");
            return id;
        } catch (DataAccessException ex) { Files.deleteIfExists(destino); throw ex; }
    }

    @Transactional
    public void confirmarSalida(long id, long usuarioId) { simpleJdbc.update("CALL sp_confirmar_salida(?,?)", id, usuarioId); }

    @Transactional
    public void reabrirOc(long id, long solicitante, long autorizador, String motivo, boolean nuevaAprobacion) {
        simpleJdbc.update("CALL sp_reabrir_oc(?,?,?,?,?)", id, solicitante, autorizador, motivo, nuevaAprobacion);
    }

    @Transactional
    public long solicitarReapertura(long ordenId,long solicitante,String motivo,boolean nuevaAprobacion) {
        String estado=simpleJdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=? FOR UPDATE",String.class,ordenId);
        if(!List.of("APROBADA","RECHAZADA").contains(estado)) throw new IllegalArgumentException("La OC no es elegible para reapertura");
        Integer pendientes=simpleJdbc.queryForObject("SELECT COUNT(*) FROM solicitud_reapertura_oc WHERE id_orden_compra=? AND estado='PENDIENTE'",Integer.class,ordenId);
        if(pendientes!=null&&pendientes>0) throw new IllegalArgumentException("Ya existe una solicitud pendiente");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update("INSERT INTO solicitud_reapertura_oc(id_orden_compra,solicitada_por,motivo,requiere_nueva_aprobacion) VALUES(:oc,:usuario,:motivo,:nueva)",new MapSqlParameterSource().addValue("oc",ordenId).addValue("usuario",solicitante).addValue("motivo",motivo).addValue("nueva",nuevaAprobacion),key,new String[]{"id_solicitud_reapertura"});
        long id=key.getKey().longValue(); auditar(solicitante,"SOLICITAR_REAPERTURA","ORDEN_COMPRA",ordenId,"{\"solicitud\":"+id+"}"); return id;
    }

    public List<Map<String,Object>> solicitudesReapertura() {
        return simpleJdbc.queryForList("SELECT s.id_solicitud_reapertura,s.id_orden_compra,oc.numero,s.motivo,s.requiere_nueva_aprobacion,s.estado,us.nombre AS solicitada_por,ua.nombre AS autorizada_por,s.solicitada_en,s.resuelta_en FROM solicitud_reapertura_oc s JOIN orden_compra oc ON oc.id_orden_compra=s.id_orden_compra JOIN usuario us ON us.id_usuario=s.solicitada_por LEFT JOIN usuario ua ON ua.id_usuario=s.autorizada_por ORDER BY s.solicitada_en DESC");
    }

    @Transactional
    public void resolverReapertura(long solicitudId,long autorizador,boolean aprobar) {
        Map<String,Object> s=simpleJdbc.queryForMap("SELECT id_orden_compra,solicitada_por,motivo,requiere_nueva_aprobacion,estado FROM solicitud_reapertura_oc WHERE id_solicitud_reapertura=? FOR UPDATE",solicitudId);
        if(!"PENDIENTE".equals(s.get("estado"))) throw new IllegalArgumentException("La solicitud ya fue resuelta");
        Object valorNueva=s.get("requiere_nueva_aprobacion");
        boolean nueva=valorNueva instanceof Boolean b ? b : valorNueva instanceof Number n && n.intValue()!=0;
        if(aprobar) reabrirOc(((Number)s.get("id_orden_compra")).longValue(),((Number)s.get("solicitada_por")).longValue(),autorizador,String.valueOf(s.get("motivo")),nueva);
        simpleJdbc.update("UPDATE solicitud_reapertura_oc SET estado=?,autorizada_por=?,resuelta_en=NOW() WHERE id_solicitud_reapertura=?",aprobar?"APROBADA":"RECHAZADA",autorizador,solicitudId);
    }

    @Transactional
    public void anularMovimiento(long id, long autorizador, String motivo) {
        simpleJdbc.update("CALL sp_anular_movimiento(?,?,?)", id, autorizador, motivo);
    }

    @Transactional
    public long solicitarAnulacion(long movimientoId,long solicitante,String motivo) {
        Map<String,Object> movimiento=simpleJdbc.queryForMap("SELECT tipo,anulado FROM movimiento_inventario WHERE id_movimiento=? FOR UPDATE",movimientoId);
        if(Boolean.TRUE.equals(movimiento.get("anulado")) || !List.of("INGRESO","SALIDA").contains(movimiento.get("tipo"))) throw new IllegalArgumentException("Movimiento no anulable");
        Integer pendientes=simpleJdbc.queryForObject("SELECT COUNT(*) FROM solicitud_anulacion WHERE id_movimiento=? AND estado='PENDIENTE'",Integer.class,movimientoId);
        if(pendientes!=null && pendientes>0) throw new IllegalArgumentException("Ya existe una solicitud pendiente");
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update("INSERT INTO solicitud_anulacion(id_movimiento,solicitada_por,motivo) VALUES(:movimiento,:usuario,:motivo)",
            new MapSqlParameterSource().addValue("movimiento",movimientoId).addValue("usuario",solicitante).addValue("motivo",motivo),key,new String[]{"id_solicitud"});
        return key.getKey().longValue();
    }

    @Transactional
    public void resolverAnulacion(long solicitudId,long autorizador,boolean aprobar) {
        Map<String,Object> solicitud=simpleJdbc.queryForMap("SELECT id_movimiento,motivo,estado FROM solicitud_anulacion WHERE id_solicitud=? FOR UPDATE",solicitudId);
        if(!"PENDIENTE".equals(solicitud.get("estado"))) throw new IllegalArgumentException("La solicitud ya fue resuelta");
        if(aprobar) simpleJdbc.update("CALL sp_anular_movimiento(?,?,?)",((Number)solicitud.get("id_movimiento")).longValue(),autorizador,solicitud.get("motivo"));
        simpleJdbc.update("UPDATE solicitud_anulacion SET estado=?,autorizada_por=?,resuelta_en=NOW() WHERE id_solicitud=?",aprobar?"APROBADA":"RECHAZADA",autorizador,solicitudId);
    }

    public List<Map<String,Object>> alertasStock() { return simpleJdbc.queryForList("SELECT * FROM vw_stock_alerta WHERE nivel<>'NORMAL' ORDER BY nivel,nombre"); }
    public List<Map<String,Object>> trazabilidad() { return simpleJdbc.queryForList("SELECT * FROM vw_trazabilidad_stock ORDER BY id_reapertura DESC LIMIT 1000"); }
    public List<Map<String,Object>> reporteCompras() { return simpleJdbc.queryForList("SELECT oc.numero,p.razon_social,oc.fecha_emision,oc.estado,oc.moneda,oc.total,oc.version FROM orden_compra oc JOIN proveedor p ON p.id_proveedor=oc.id_proveedor ORDER BY oc.fecha_emision DESC"); }
    public List<Map<String,Object>> reporteSalidas() { return simpleJdbc.queryForList("SELECT s.numero,r.numero AS requerimiento,r.placa_unidad,s.entregada_a,s.estado,s.fecha_entrega,a.nombre AS almacen FROM orden_salida s JOIN requerimiento r ON r.id_requerimiento=s.id_requerimiento JOIN almacen a ON a.id_almacen=s.id_almacen ORDER BY s.fecha_preparacion DESC"); }
    public List<Map<String,Object>> movimientos() { return simpleJdbc.queryForList("SELECT m.id_movimiento,p.codigo,p.nombre,a.codigo AS almacen,m.tipo,m.cantidad,m.saldo_anterior,m.saldo_nuevo,m.origen_tipo,m.origen_id,m.fecha,m.anulado FROM movimiento_inventario m JOIN inventario i ON i.id_inventario=m.id_inventario JOIN producto p ON p.id_producto=i.id_producto JOIN almacen a ON a.id_almacen=i.id_almacen ORDER BY m.fecha DESC LIMIT 1000"); }
    public List<Map<String,Object>> solicitudesAnulacion() { return simpleJdbc.queryForList("SELECT s.id_solicitud,s.id_movimiento,s.motivo,s.estado,s.solicitada_en,s.resuelta_en,u.nombre AS solicitada_por,ua.nombre AS autorizada_por FROM solicitud_anulacion s JOIN usuario u ON u.id_usuario=s.solicitada_por LEFT JOIN usuario ua ON ua.id_usuario=s.autorizada_por ORDER BY s.solicitada_en DESC"); }
    public List<Map<String,Object>> auditoria() { return simpleJdbc.queryForList("SELECT a.id_auditoria,a.accion,a.entidad,a.entidad_id,a.detalle,a.fecha,a.ip,u.nombre AS usuario,u.correo FROM auditoria a LEFT JOIN usuario u ON u.id_usuario=a.id_usuario ORDER BY a.fecha DESC LIMIT 1000"); }


    public static void validarContenido(byte[] bytes,String mime) {
        boolean valido = switch(mime) {
            case "image/jpeg" -> bytes.length>=3 && (bytes[0]&255)==255 && (bytes[1]&255)==216 && (bytes[2]&255)==255;
            case "image/png" -> bytes.length>=8 && java.util.Arrays.equals(java.util.Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "application/pdf" -> bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
            default -> false;
        };
        if(!valido) throw new IllegalArgumentException("El contenido no corresponde al formato declarado");
    }

    private void guardarVersionOrden(long id,long usuario) {
        simpleJdbc.update("INSERT IGNORE INTO historial_orden_compra(id_orden_compra,version,datos,registrada_por) " +
            "SELECT o.id_orden_compra,o.version-1,JSON_OBJECT('numero',o.numero,'proveedorId',o.id_proveedor,'fecha',o.fecha_emision,'moneda',o.moneda,'total',o.total,'observacion',o.observacion,'lineas'," +
            "(SELECT JSON_ARRAYAGG(JSON_OBJECT('productoId',d.id_producto,'cantidad',d.cantidad,'precioUnitario',d.precio_unitario)) FROM detalle_orden_compra d WHERE d.id_orden_compra=o.id_orden_compra)),? FROM orden_compra o WHERE o.id_orden_compra=?",usuario,id);
    }

    public List<Map<String,Object>> historialOrden(long id) {
        return simpleJdbc.queryForList("SELECT h.version,h.datos,h.fecha,u.nombre AS usuario FROM historial_orden_compra h JOIN usuario u ON u.id_usuario=h.registrada_por WHERE h.id_orden_compra=? ORDER BY h.version",id);
    }

    public List<Map<String,Object>> evidencias(long salida) {
        return simpleJdbc.queryForList("SELECT id_evidencia,nombre_original,tipo_mime,tamano_bytes,sha256,cargada_en FROM evidencia_salida WHERE id_orden_salida=? ORDER BY cargada_en",salida);
    }

    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> descargarEvidencia(long id) throws IOException {
        Map<String,Object> e=simpleJdbc.queryForMap("SELECT ruta_segura,nombre_original,tipo_mime FROM evidencia_salida WHERE id_evidencia=?",id);
        Path path=Path.of((String)e.get("ruta_segura")).toRealPath();
        if(!path.startsWith(evidenceDirectory.toRealPath())) throw new IllegalArgumentException("Ruta inválida");
        return org.springframework.http.ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.parseMediaType((String)e.get("tipo_mime")))
            .header("Content-Disposition",org.springframework.http.ContentDisposition.attachment().filename((String)e.get("nombre_original")).build().toString())
            .header("X-Content-Type-Options","nosniff")
            .body(new org.springframework.core.io.FileSystemResource(path));
    }

    private void auditar(long usuarioId, String accion, String entidad, long entidadId, String json) {
        simpleJdbc.update("INSERT INTO auditoria(id_usuario,accion,entidad,entidad_id,detalle) VALUES(?,?,?,?,?)", usuarioId,accion,entidad,entidadId,json);
    }

    private String nombreSeguro(String original) {
        if (original == null || original.isBlank()) return "evidencia";
        return Path.of(original).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record LineaCompra(long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {}
    public record LineaRecepcion(long productoId, BigDecimal recibida, BigDecimal conforme, BigDecimal rechazada) {}
    public record LineaRequerimiento(long productoId, BigDecimal cantidad) {}
}
