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

    public AlmacenService(NamedParameterJdbcTemplate jdbc, JdbcTemplate simpleJdbc,
            PasswordEncoder passwordEncoder, @Value("${app.evidence.directory}") String evidenceDirectory) {
        this.jdbc = jdbc;
        this.simpleJdbc = simpleJdbc;
        this.passwordEncoder = passwordEncoder;
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
        return simpleJdbc.queryForList("SELECT r.nombre AS rol,p.codigo AS permiso,p.descripcion FROM rol r LEFT JOIN rol_permiso rp ON rp.id_rol=r.id_rol LEFT JOIN permiso p ON p.id_permiso=rp.id_permiso ORDER BY r.nombre,p.codigo");
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
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO producto(codigo,nombre,descripcion,unidad_medida,stock_minimo) VALUES(:codigo,:nombre,:descripcion,:unidad,:minimo)",
            new MapSqlParameterSource().addValue("codigo", codigo).addValue("nombre", nombre).addValue("descripcion", descripcion)
                .addValue("unidad", unidad).addValue("minimo", stockMinimo), key, new String[]{"id_producto"});
        long id = key.getKey().longValue();
        auditar(usuarioId, "CREAR_PRODUCTO", "PRODUCTO", id, "{\"codigo\":\"" + codigo.replace("\"", "") + "\"}");
        return id;
    }

    @Transactional
    public void actualizarProducto(long id,String nombre,String descripcion,String unidad,BigDecimal stockMinimo,boolean activo,long usuarioId) {
        int filas=simpleJdbc.update("UPDATE producto SET nombre=?,descripcion=?,unidad_medida=?,stock_minimo=?,activo=? WHERE id_producto=?",nombre,descripcion,unidad,stockMinimo,activo,id);
        if(filas==0) throw new IllegalArgumentException("Producto no encontrado");
        auditar(usuarioId,"ACTUALIZAR_PRODUCTO","PRODUCTO",id,"{\"activo\":"+activo+"}");
    }

    public List<Map<String,Object>> inventario(Long almacenId) {
        String sql = "SELECT i.id_inventario,p.codigo,p.nombre,a.codigo AS almacen,i.stock_actual,p.stock_minimo," +
            "CASE WHEN i.stock_actual=0 THEN 'AGOTADO' WHEN i.stock_actual<=p.stock_minimo THEN 'BAJO' ELSE 'NORMAL' END nivel " +
            "FROM inventario i JOIN producto p ON p.id_producto=i.id_producto JOIN almacen a ON a.id_almacen=i.id_almacen " +
            "WHERE (:almacen IS NULL OR i.id_almacen=:almacen) ORDER BY p.nombre";
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
        Boolean nueva=simpleJdbc.queryForObject("SELECT requiere_nueva_aprobacion FROM reapertura_oc WHERE id_orden_compra=? ORDER BY fecha DESC LIMIT 1",Boolean.class,ordenId);
        simpleJdbc.update("DELETE FROM detalle_orden_compra WHERE id_orden_compra=?",ordenId);
        simpleJdbc.update("UPDATE orden_compra SET id_proveedor=?,fecha_emision=?,moneda=?,observacion=?,estado=? WHERE id_orden_compra=?",proveedorId,fecha,moneda,observacion,Boolean.TRUE.equals(nueva)?"PENDIENTE_APROBACION":"APROBADA",ordenId);
        for(LineaCompra linea:lineas) simpleJdbc.update("INSERT INTO detalle_orden_compra(id_orden_compra,id_producto,cantidad,precio_unitario) VALUES(?,?,?,?)",ordenId,linea.productoId(),linea.cantidad(),linea.precioUnitario());
        auditar(usuarioId,"ACTUALIZAR_OC","ORDEN_COMPRA",ordenId,"{\"requiereNuevaAprobacion\":"+Boolean.TRUE.equals(nueva)+"}");
    }

    @Transactional
    public void decidirOc(long ordenId, long usuarioId, String decision, String comentario) {
        simpleJdbc.update("CALL sp_decidir_oc(?,?,?,?)", ordenId, usuarioId, decision, comentario);
    }

    @Transactional
    public long crearRecepcion(long ordenId, long almacenId, String guia, String observacion,
            List<LineaRecepcion> lineas, long usuarioId) {
        if (lineas == null || lineas.isEmpty()) throw new IllegalArgumentException("La recepcion requiere detalle");
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
        return key.getKey().longValue();
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
        String numero = "SAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO orden_salida(numero,id_requerimiento,id_almacen,preparada_por,entregada_a) VALUES(:numero,:req,:almacen,:usuario,:entrega)",
            new MapSqlParameterSource().addValue("numero",numero).addValue("req",requerimientoId).addValue("almacen",almacenId)
                .addValue("usuario",usuarioId).addValue("entrega",entregadaA), key, new String[]{"id_orden_salida"});
        return key.getKey().longValue();
    }

    public List<Map<String,Object>> salidas() {
        return simpleJdbc.queryForList("SELECT s.id_orden_salida,s.numero,r.numero AS requerimiento,a.nombre AS almacen,u.nombre AS preparada_por,s.entregada_a,s.estado,s.fecha_preparacion,s.fecha_entrega,(SELECT COUNT(*) FROM evidencia_salida e WHERE e.id_orden_salida=s.id_orden_salida) AS evidencias FROM orden_salida s JOIN requerimiento r ON r.id_requerimiento=s.id_requerimiento JOIN almacen a ON a.id_almacen=s.id_almacen JOIN usuario u ON u.id_usuario=s.preparada_por ORDER BY s.fecha_preparacion DESC");
    }

    @Transactional
    public long guardarEvidencia(long salidaId, MultipartFile archivo, long usuarioId) throws IOException {
        if (archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Archivo vacio o mayor de 5 MB");
        String mime = archivo.getContentType();
        if (!MIME_PERMITIDOS.contains(mime)) throw new IllegalArgumentException("Solo se admite JPG, PNG o PDF");
        byte[] bytes = archivo.getBytes();
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException("No se pudo calcular la huella del archivo", e); }
        String extension = switch (mime) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; default -> ".pdf"; };
        Files.createDirectories(evidenceDirectory);
        Path destino = evidenceDirectory.resolve(UUID.randomUUID() + extension).normalize();
        if (!destino.startsWith(evidenceDirectory)) throw new IllegalArgumentException("Ruta de evidencia invalida");
        Files.write(destino, bytes, StandardOpenOption.CREATE_NEW);
        try {
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO evidencia_salida(id_orden_salida,nombre_original,ruta_segura,tipo_mime,tamano_bytes,sha256,cargada_por) " +
                    "VALUES(:salida,:nombre,:ruta,:mime,:tamano,:sha,:usuario)",
                new MapSqlParameterSource().addValue("salida",salidaId).addValue("nombre",nombreSeguro(archivo.getOriginalFilename()))
                    .addValue("ruta",destino.toString()).addValue("mime",mime).addValue("tamano",bytes.length).addValue("sha",hash).addValue("usuario",usuarioId),
                key, new String[]{"id_evidencia"});
            return key.getKey().longValue();
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
        String estado=simpleJdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=?",String.class,ordenId);
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
    public List<Map<String,Object>> trazabilidad() { return simpleJdbc.queryForList("SELECT * FROM vw_trazabilidad_stock ORDER BY fecha DESC LIMIT 1000"); }
    public List<Map<String,Object>> reporteCompras() { return simpleJdbc.queryForList("SELECT oc.numero,p.razon_social,oc.fecha_emision,oc.estado,oc.moneda,oc.total,oc.version FROM orden_compra oc JOIN proveedor p ON p.id_proveedor=oc.id_proveedor ORDER BY oc.fecha_emision DESC"); }
    public List<Map<String,Object>> reporteSalidas() { return simpleJdbc.queryForList("SELECT s.numero,r.numero AS requerimiento,r.placa_unidad,s.entregada_a,s.estado,s.fecha_entrega,a.nombre AS almacen FROM orden_salida s JOIN requerimiento r ON r.id_requerimiento=s.id_requerimiento JOIN almacen a ON a.id_almacen=s.id_almacen ORDER BY s.fecha_preparacion DESC"); }
    public List<Map<String,Object>> movimientos() { return simpleJdbc.queryForList("SELECT m.id_movimiento,p.codigo,p.nombre,a.codigo AS almacen,m.tipo,m.cantidad,m.saldo_anterior,m.saldo_nuevo,m.origen_tipo,m.origen_id,m.fecha,m.anulado FROM movimiento_inventario m JOIN inventario i ON i.id_inventario=m.id_inventario JOIN producto p ON p.id_producto=i.id_producto JOIN almacen a ON a.id_almacen=i.id_almacen ORDER BY m.fecha DESC LIMIT 1000"); }
    public List<Map<String,Object>> solicitudesAnulacion() { return simpleJdbc.queryForList("SELECT s.id_solicitud,s.id_movimiento,s.motivo,s.estado,s.solicitada_en,s.resuelta_en,u.nombre AS solicitada_por,ua.nombre AS autorizada_por FROM solicitud_anulacion s JOIN usuario u ON u.id_usuario=s.solicitada_por LEFT JOIN usuario ua ON ua.id_usuario=s.autorizada_por ORDER BY s.solicitada_en DESC"); }
    public List<Map<String,Object>> auditoria() { return simpleJdbc.queryForList("SELECT a.id_auditoria,a.accion,a.entidad,a.entidad_id,a.detalle,a.fecha,a.ip,u.nombre AS usuario,u.correo FROM auditoria a LEFT JOIN usuario u ON u.id_usuario=a.id_usuario ORDER BY a.fecha DESC LIMIT 1000"); }

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
