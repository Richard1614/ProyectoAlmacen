package com.example.demo.almacen.controlador;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.almacen.servicio.AlmacenService;
import com.example.demo.almacen.servicio.AlmacenService.LineaCompra;
import com.example.demo.almacen.servicio.AlmacenService.LineaRecepcion;
import com.example.demo.almacen.servicio.AlmacenService.LineaRequerimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/v1")
public class AlmacenApiController {
    private final AlmacenService service;
    public AlmacenApiController(AlmacenService service) { this.service = service; }

    @GetMapping("/sesion")
    public Map<String,Object> sesion(Principal principal) { return service.sesion(principal.getName()); }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public List<Map<String,Object>> usuarios() { return service.usuarios(); }

    @GetMapping("/roles-permisos")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public List<Map<String,Object>> rolesPermisos() { return service.rolesPermisos(); }

    @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public Map<String,Long> crearUsuario(@Valid @RequestBody UsuarioRequest r,Principal p) {
        return Map.of("id",service.crearUsuario(r.nombre(),r.correo(),r.telefono(),r.password(),r.rol(),userId(p)));
    }

    @PatchMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public void estadoUsuario(@PathVariable long id,@RequestBody EstadoRequest r,Principal p) { service.cambiarEstadoUsuario(id,r.activo(),userId(p)); }

    @PatchMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public void rolUsuario(@PathVariable long id,@Valid @RequestBody RolUsuarioRequest r,Principal p) { service.cambiarRolUsuario(id,r.rol(),userId(p)); }

    @PutMapping("/roles/{id}/permisos")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public void permisosRol(@PathVariable long id,@Valid @RequestBody PermisosRolRequest r,Principal p) { service.actualizarPermisosRol(id,r.permisos(),userId(p)); }

    @GetMapping("/proveedores")
    @PreAuthorize("hasAnyAuthority('OC_REGISTRAR','RECEPCION_REGISTRAR')")
    public List<Map<String,Object>> proveedores() { return service.proveedores(); }

    @PostMapping("/proveedores") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('OC_REGISTRAR')")
    public Map<String,Long> crearProveedor(@Valid @RequestBody ProveedorRequest r,Principal p) {
        return Map.of("id",service.crearProveedor(r.ruc(),r.razonSocial(),r.contacto(),r.correo(),r.telefono(),r.direccion(),userId(p)));
    }

    @PutMapping("/proveedores/{id}")
    @PreAuthorize("hasAuthority('OC_REGISTRAR')")
    public void actualizarProveedor(@PathVariable long id,@Valid @RequestBody ProveedorUpdateRequest r,Principal p) {
        service.actualizarProveedor(id,r.razonSocial(),r.contacto(),r.correo(),r.telefono(),r.direccion(),r.activo(),userId(p));
    }

    @GetMapping("/almacenes")
    @PreAuthorize("hasAuthority('INVENTARIO_CONSULTAR')")
    public List<Map<String,Object>> almacenes() { return service.almacenes(); }

    @GetMapping("/productos")
    @PreAuthorize("hasAnyAuthority('INVENTARIO_CONSULTAR','REQUERIMIENTO_REGISTRAR','OC_REGISTRAR')")
    public List<Map<String,Object>> productos() { return service.productos(); }

    @PostMapping("/productos") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCTOS_ADMINISTRAR')")
    public Map<String,Long> crearProducto(@Valid @RequestBody ProductoRequest r, Principal p) {
        return Map.of("id", service.crearProducto(r.codigo(),r.nombre(),r.descripcion(),r.unidadMedida(),r.stockMinimo(),userId(p)));
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasAuthority('PRODUCTOS_ADMINISTRAR')")
    public void actualizarProducto(@PathVariable long id,@Valid @RequestBody ProductoUpdateRequest r,Principal p) {
        service.actualizarProducto(id,r.nombre(),r.descripcion(),r.unidadMedida(),r.stockMinimo(),r.activo(),userId(p));
    }

    @GetMapping("/inventario")
    @PreAuthorize("hasAuthority('INVENTARIO_CONSULTAR')")
    public List<Map<String,Object>> inventario(@RequestParam(required=false) Long almacenId) { return service.inventario(almacenId); }

    @PostMapping("/compras/ordenes") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('OC_REGISTRAR')")
    public Map<String,Long> crearOc(@Valid @RequestBody OrdenCompraRequest r, Principal p) {
        List<LineaCompra> lineas=r.lineas().stream().map(x->new LineaCompra(x.productoId(),x.cantidad(),x.precioUnitario())).toList();
        return Map.of("id",service.crearOrdenCompra(r.proveedorId(),r.fechaEmision(),r.moneda(),r.observacion(),lineas,userId(p)));
    }

    @PutMapping("/compras/ordenes/{id}")
    @PreAuthorize("hasAuthority('OC_REGISTRAR')")
    public void actualizarOc(@PathVariable long id,@Valid @RequestBody OrdenCompraRequest r,Principal p) {
        List<LineaCompra> lineas=r.lineas().stream().map(x->new LineaCompra(x.productoId(),x.cantidad(),x.precioUnitario())).toList();
        service.actualizarOrdenCompra(id,r.proveedorId(),r.fechaEmision(),r.moneda(),r.observacion(),lineas,userId(p));
    }

    @GetMapping("/compras/ordenes")
    @PreAuthorize("hasAnyAuthority('OC_REGISTRAR','OC_AUTORIZAR','OC_REABRIR','RECEPCION_REGISTRAR')")
    public List<Map<String,Object>> ordenesCompra() { return service.ordenesCompra(); }

    @GetMapping("/compras/ordenes/{id}/detalle")
    @PreAuthorize("hasAnyAuthority('OC_REGISTRAR','OC_AUTORIZAR','OC_REABRIR','RECEPCION_REGISTRAR')")
    public List<Map<String,Object>> detalleOrdenCompra(@PathVariable long id) { return service.detalleOrdenCompra(id); }

    @PostMapping("/compras/ordenes/{id}/decision")
    @PreAuthorize("hasAuthority('OC_AUTORIZAR')")
    public void decidirOc(@PathVariable long id,@Valid @RequestBody DecisionRequest r,Principal p) { service.decidirOc(id,userId(p),r.decision(),r.comentario()); }

    @PostMapping("/compras/ordenes/{id}/reabrir")
    @PreAuthorize("hasAuthority('OC_REABRIR')")
    public void reabrirOc(@PathVariable long id,@Valid @RequestBody ReaperturaRequest r,Principal p) {
        long actual=userId(p); service.reabrirOc(id,actual,actual,r.motivo(),r.requiereNuevaAprobacion());
    }

    @PostMapping("/compras/ordenes/{id}/solicitudes-reapertura") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('OC_REAPERTURA_SOLICITAR','OC_REABRIR')")
    public Map<String,Long> solicitarReapertura(@PathVariable long id,@Valid @RequestBody ReaperturaRequest r,Principal p) {
        return Map.of("id",service.solicitarReapertura(id,userId(p),r.motivo(),r.requiereNuevaAprobacion()));
    }

    @GetMapping("/solicitudes-reapertura")
    @PreAuthorize("hasAnyAuthority('OC_REAPERTURA_SOLICITAR','OC_REAPERTURA_AUTORIZAR','OC_REABRIR')")
    public List<Map<String,Object>> solicitudesReapertura() { return service.solicitudesReapertura(); }

    @PostMapping("/solicitudes-reapertura/{id}/resolver")
    @PreAuthorize("hasAnyAuthority('OC_REAPERTURA_AUTORIZAR','OC_AUTORIZAR')")
    public void resolverReapertura(@PathVariable long id,@RequestBody ResolucionRequest r,Principal p) { service.resolverReapertura(id,userId(p),r.aprobar()); }

    @PostMapping("/recepciones") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RECEPCION_REGISTRAR')")
    public Map<String,Long> crearRecepcion(@Valid @RequestBody RecepcionRequest r,Principal p) {
        List<LineaRecepcion> lineas=r.lineas().stream().map(x->new LineaRecepcion(x.productoId(),x.recibida(),x.conforme(),x.rechazada())).toList();
        return Map.of("id",service.crearRecepcion(r.ordenCompraId(),r.almacenId(),r.guiaRemision(),r.observacion(),lineas,userId(p)));
    }

    @GetMapping("/recepciones")
    @PreAuthorize("hasAuthority('RECEPCION_REGISTRAR')")
    public List<Map<String,Object>> recepciones() { return service.recepciones(); }

    @GetMapping("/recepciones/{id}/detalle")
    @PreAuthorize("hasAuthority('RECEPCION_REGISTRAR')")
    public List<Map<String,Object>> detalleRecepcion(@PathVariable long id) { return service.detalleRecepcion(id); }

    @PostMapping("/recepciones/{id}/confirmar")
    @PreAuthorize("hasAuthority('RECEPCION_REGISTRAR')")
    public void confirmarRecepcion(@PathVariable long id,Principal p) { service.confirmarRecepcion(id,userId(p)); }

    @PostMapping("/recepciones/{id}/incidencias") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RECEPCION_REGISTRAR')")
    public Map<String,Long> incidencia(@PathVariable long id,@Valid @RequestBody IncidenciaRequest r,Principal p) {
        return Map.of("id",service.registrarIncidencia(id,r.productoId(),r.tipo(),r.descripcion(),r.accion(),userId(p)));
    }

    @GetMapping("/incidencias")
    @PreAuthorize("hasAnyAuthority('RECEPCION_REGISTRAR','OC_REGISTRAR')")
    public List<Map<String,Object>> incidencias() { return service.incidenciasRecepcion(); }

    @PatchMapping("/incidencias/{id}")
    @PreAuthorize("hasAnyAuthority('RECEPCION_REGISTRAR','OC_REGISTRAR')")
    public void resolverIncidencia(@PathVariable long id,@Valid @RequestBody IncidenciaEstadoRequest r,Principal p) { service.resolverIncidencia(id,r.estado(),r.accion(),userId(p)); }

    @PostMapping("/taller/requerimientos") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('REQUERIMIENTO_REGISTRAR')")
    public Map<String,Long> crearRequerimiento(@Valid @RequestBody RequerimientoRequest r,Principal p) {
        List<LineaRequerimiento> lineas=r.lineas().stream().map(x->new LineaRequerimiento(x.productoId(),x.cantidad())).toList();
        return Map.of("id",service.crearRequerimiento(r.placaUnidad(),r.tipoMantenimiento(),r.prioridad(),r.motivo(),lineas,userId(p)));
    }

    @GetMapping("/taller/requerimientos")
    @PreAuthorize("hasAnyAuthority('REQUERIMIENTO_REGISTRAR','SALIDA_CONFIRMAR')")
    public List<Map<String,Object>> requerimientos() { return service.requerimientos(); }

    @GetMapping("/taller/requerimientos/{id}/detalle")
    @PreAuthorize("hasAnyAuthority('REQUERIMIENTO_REGISTRAR','SALIDA_CONFIRMAR')")
    public List<Map<String,Object>> detalleRequerimiento(@PathVariable long id) { return service.detalleRequerimiento(id); }

    @PostMapping("/salidas") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SALIDA_CONFIRMAR')")
    public Map<String,Long> prepararSalida(@Valid @RequestBody SalidaRequest r,Principal p) {
        return Map.of("id",service.prepararSalida(r.requerimientoId(),r.almacenId(),r.entregadaA(),userId(p)));
    }

    @GetMapping("/salidas")
    @PreAuthorize("hasAuthority('SALIDA_CONFIRMAR')")
    public List<Map<String,Object>> salidas() { return service.salidas(); }

    @PostMapping(path="/salidas/{id}/evidencias",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SALIDA_CONFIRMAR')")
    public Map<String,Long> evidencia(@PathVariable long id,@RequestPart("archivo") MultipartFile archivo,Principal p) throws Exception {
        return Map.of("id",service.guardarEvidencia(id,archivo,userId(p)));
    }

    @PostMapping("/salidas/{id}/confirmar")
    @PreAuthorize("hasAuthority('SALIDA_CONFIRMAR')")
    public void confirmarSalida(@PathVariable long id,Principal p) { service.confirmarSalida(id,userId(p)); }

    @PostMapping("/movimientos/{id}/anular")
    @PreAuthorize("hasAuthority('MOVIMIENTO_ANULAR')")
    public void anular(@PathVariable long id,@Valid @RequestBody MotivoRequest r,Principal p) { service.anularMovimiento(id,userId(p),r.motivo()); }

    @PostMapping("/movimientos/{id}/solicitudes-anulacion") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SALIDA_CONFIRMAR','RECEPCION_REGISTRAR')")
    public Map<String,Long> solicitarAnulacion(@PathVariable long id,@Valid @RequestBody MotivoRequest r,Principal p) {
        return Map.of("id",service.solicitarAnulacion(id,userId(p),r.motivo()));
    }

    @PostMapping("/solicitudes-anulacion/{id}/resolver")
    @PreAuthorize("hasAuthority('MOVIMIENTO_ANULAR')")
    public void resolverAnulacion(@PathVariable long id,@RequestBody ResolucionRequest r,Principal p) { service.resolverAnulacion(id,userId(p),r.aprobar()); }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAnyAuthority('INVENTARIO_CONSULTAR','MOVIMIENTO_ANULAR')")
    public List<Map<String,Object>> movimientos() { return service.movimientos(); }

    @GetMapping("/solicitudes-anulacion")
    @PreAuthorize("hasAuthority('MOVIMIENTO_ANULAR')")
    public List<Map<String,Object>> solicitudesAnulacion() { return service.solicitudesAnulacion(); }

    @GetMapping("/reportes/stock-bajo")
    @PreAuthorize("hasAnyAuthority('INVENTARIO_CONSULTAR','AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> stockBajo() { return service.alertasStock(); }

    @GetMapping("/reportes/trazabilidad")
    @PreAuthorize("hasAuthority('AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> trazabilidad() { return service.trazabilidad(); }

    @GetMapping("/reportes/compras")
    @PreAuthorize("hasAuthority('AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> reporteCompras() { return service.reporteCompras(); }

    @GetMapping("/reportes/inventario")
    @PreAuthorize("hasAuthority('AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> reporteInventario() { return service.inventario(null); }

    @GetMapping("/reportes/salidas")
    @PreAuthorize("hasAuthority('AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> reporteSalidas() { return service.reporteSalidas(); }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAuthority('AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> auditoria() { return service.auditoria(); }

    @GetMapping("/compras/ordenes/{id}/historial")
    @PreAuthorize("hasAnyAuthority('OC_REGISTRAR','OC_AUTORIZAR','OC_REABRIR','AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> historial(@PathVariable long id) { return service.historialOrden(id); }

    @GetMapping("/salidas/{id}/evidencias")
    @PreAuthorize("hasAnyAuthority('SALIDA_CONFIRMAR','AUDITORIA_CONSULTAR')")
    public List<Map<String,Object>> evidencias(@PathVariable long id) { return service.evidencias(id); }

    @GetMapping("/evidencias/{id}/archivo")
    @PreAuthorize("hasAnyAuthority('SALIDA_CONFIRMAR','AUDITORIA_CONSULTAR')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> archivo(@PathVariable long id) throws java.io.IOException { return service.descargarEvidencia(id); }

    private long userId(Principal p) { return service.usuarioId(p.getName()); }

    public record UsuarioRequest(@NotBlank String nombre,@Email @NotBlank String correo,String telefono,@NotBlank @Size(min=12,max=72) String password,
        @NotBlank @NotNull @Pattern(regexp="ADMINISTRADOR|ALMACEN|COMPRAS|GERENCIA|TALLER") String rol) {}
    public record EstadoRequest(boolean activo) {}
    public record RolUsuarioRequest(@NotBlank @NotNull @Pattern(regexp="ADMINISTRADOR|ALMACEN|COMPRAS|GERENCIA|TALLER") String rol) {}
    public record PermisosRolRequest(@NotNull List<@Positive Long> permisos) {}
    public record ProveedorRequest(@NotNull @Pattern(regexp="\\d{11}") String ruc,@NotBlank String razonSocial,String contacto,@Email String correo,String telefono,String direccion) {}
    public record ProveedorUpdateRequest(@NotBlank String razonSocial,String contacto,@Email String correo,String telefono,String direccion,boolean activo) {}
    public record ProductoRequest(@NotBlank String codigo,@NotBlank String nombre,String descripcion,@NotBlank String unidadMedida,@NotNull @PositiveOrZero BigDecimal stockMinimo) {}
    public record ProductoUpdateRequest(@NotBlank String nombre,String descripcion,@NotBlank String unidadMedida,@NotNull @PositiveOrZero BigDecimal stockMinimo,boolean activo) {}
    public record LineaCompraRequest(@Positive long productoId,@NotNull @Positive BigDecimal cantidad,@NotNull @PositiveOrZero BigDecimal precioUnitario) {}
    public record OrdenCompraRequest(@Positive long proveedorId,@NotNull LocalDate fechaEmision,@NotNull @Pattern(regexp="[A-Z]{3}") String moneda,String observacion,@NotEmpty List<@Valid LineaCompraRequest> lineas) {}
    public record DecisionRequest(@NotNull @Pattern(regexp="APROBADA|RECHAZADA") String decision,String comentario) {}
    public record ReaperturaRequest(@NotBlank String motivo,boolean requiereNuevaAprobacion) {}
    public record LineaRecepcionRequest(@Positive long productoId,@NotNull @Positive BigDecimal recibida,@NotNull @PositiveOrZero BigDecimal conforme,@NotNull @PositiveOrZero BigDecimal rechazada) {}
    public record RecepcionRequest(@Positive long ordenCompraId,@Positive long almacenId,@NotBlank String guiaRemision,String observacion,@NotEmpty List<@Valid LineaRecepcionRequest> lineas) {}
    public record IncidenciaRequest(Long productoId,@NotNull @Pattern(regexp="DANIO|FALTANTE|SOBRANTE|DOCUMENTACION|OTRO") String tipo,@NotBlank String descripcion,@NotNull @Pattern(regexp="CAMBIO|REEMBOLSO|ACEPTACION_PARCIAL|PENDIENTE") String accion) {}
    public record IncidenciaEstadoRequest(@NotNull @Pattern(regexp="ABIERTA|EN_GESTION|RESUELTA") String estado,@NotNull @Pattern(regexp="CAMBIO|REEMBOLSO|ACEPTACION_PARCIAL|PENDIENTE") String accion) {}
    public record LineaRequerimientoRequest(@Positive long productoId,@NotNull @Positive BigDecimal cantidad) {}
    public record RequerimientoRequest(String placaUnidad,@NotNull @Pattern(regexp="PREVENTIVO|CORRECTIVO|SINIESTRO|OTRO") String tipoMantenimiento,@NotNull @Pattern(regexp="BAJA|MEDIA|ALTA|URGENTE") String prioridad,@NotBlank String motivo,@NotEmpty List<@Valid LineaRequerimientoRequest> lineas) {}
    public record SalidaRequest(@Positive long requerimientoId,@Positive long almacenId,@NotBlank String entregadaA) {}
    public record MotivoRequest(@NotBlank @Size(max=500) String motivo) {}
    public record ResolucionRequest(boolean aprobar) {}
}
