# Trazabilidad entre requisitos y código

Esta matriz relaciona los veinte requisitos funcionales del documento con la
implementación operativa de Austral Pilot y su interfaz web por roles.

| Requisito | Implementación |
|---|---|
| RF-01 Autenticación | Spring Security HTTP Basic, `SecurityUserDetailsService` y BCrypt |
| RF-02 Usuarios roles y permisos | Tablas `usuario`, `rol`, `permiso`, `rol_permiso`; alta/estado de usuarios, cambio de rol y edición de permisos por rol en la interfaz |
| RF-03 Catálogo | Tabla `producto`; API GET POST PUT `/api/v1/productos` |
| RF-04 Stock por almacén | Tablas `almacen`, `inventario`; GET `/api/v1/inventario` |
| RF-05 Órdenes de compra | `orden_compra`, `detalle_orden_compra`; API `/api/v1/compras/ordenes` |
| RF-06 Autorización gerencial | `autorizacion_oc`, permiso `OC_AUTORIZAR`, `sp_decidir_oc` |
| RF-07 Proveedores | Tabla `proveedor`; registro, consulta y edición desde la interfaz |
| RF-08 Recepciones y guía | `recepcion`, `detalle_recepcion`; API `/api/v1/recepciones` |
| RF-09 Incremento de stock | `sp_confirmar_recepcion` dentro de transacción |
| RF-10 Incidencias | `incidencia_recepcion`; registro, seguimiento de acción y resolución en módulo propio |
| RF-11 Requerimientos de Taller | `requerimiento`, `detalle_requerimiento`; API `/api/v1/taller/requerimientos` |
| RF-12 Órdenes de salida | `orden_salida`; API `/api/v1/salidas` |
| RF-13 Evidencia firmada | `evidencia_salida`; carga JPG PNG PDF con límite y SHA-256 |
| RF-14 Entrega y descuento | `sp_confirmar_salida`; exige evidencia y evita doble confirmación |
| RF-15 Anulación controlada | `solicitud_anulacion`, `sp_anular_movimiento` y movimiento reverso |
| RF-16 Reapertura de OC | `solicitud_reapertura_oc`, autorización separada, historial, edición de versión y `sp_reabrir_oc` |
| RF-17 Auditoría | Tabla `auditoria`, vista `vw_trazabilidad_stock` y pantalla de auditoría |
| RF-18 Reportes | Paneles de inventario, compras y salidas con exportación CSV |
| RF-19 Stock mínimo | Vista `vw_stock_alerta` y ruta `/reportes/stock-bajo` |
| RF-20 Estados | Estados y detalles visibles en órdenes, recepciones, requerimientos, incidencias y salidas |

Los requisitos no funcionales de seguridad, permisos, integridad transaccional,
auditoría, control de archivos y modularidad están cubiertos por la configuración
de seguridad, las restricciones de MySQL, los procedimientos almacenados, la
carga controlada de evidencias y la separación controlador servicio persistencia.
