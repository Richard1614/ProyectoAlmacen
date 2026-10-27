# Validación del proyecto contra el documento V2

El sistema implementa los módulos RF-01 a RF-20. La presencia del código no
equivale a una validación funcional con MySQL. Esta guía distingue las pruebas
automáticas sin base de datos de los escenarios que requieren una instalación
de prueba. No se han inventado resultados, capturas, responsables ni costos.

## Preparación

1. Crear una base de pruebas con `01_schema.sql` y `02_seed.sql`. En una
   instalación anterior aplicar `03_requirements_update.sql` y
   `04_integridad_historial.sql`; no volver a ejecutar el esquema inicial.
2. Configurar las variables del README, arrancar y crear cuentas de Almacén,
   Compras, Gerencia y Taller desde el administrador.
3. Ejecutar `./mvnw.cmd test` y conservar los informes de `target/surefire-reports`.
4. Realizar los casos siguientes con productos y documentos ficticios.

| Caso | Escenario y comprobación | Cobertura automática sin MySQL |
|---|---|---|
| PF-01 | Iniciar sesión con credenciales correctas e incorrectas | Hash BCrypt y API anónima |
| PF-02 | Crear producto y comprobar catálogo y alerta de stock cero | Requiere base de pruebas |
| PF-03 | Crear OC con productos y comprobar PENDIENTE_APROBACION | Rechazo de detalle vacío y cantidades nulas |
| PF-04 | Gerencia aprueba o rechaza; queda auditoría | Requiere base de pruebas |
| PF-05 | Almacén recibe 10 unidades conformes de una OC aprobada; stock +10 | Acceso de Almacén y rechazo de OC no aprobada |
| PF-06 | Recibir 8 conformes y 2 rechazadas; stock +8 e incidencia registrada | Requiere base de pruebas |
| PF-07 | Taller crea requerimiento y Almacén consulta sus detalles | Acceso al catálogo y rechazo de requerimiento vacío |
| PF-08 | Preparar salida, adjuntar orden firmada y confirmar; stock disminuye una vez | Rechazo de requerimiento atendido |
| PF-09 | Confirmar sin stock, incluido un producto sin fila de inventario; no cambiar nada | Requiere base de pruebas |
| PF-10 | Solicitar anulación, autorizar y comprobar reverso, stock y estado | Rechazo de reversos y solicitudes ya resueltas |
| PF-11 | Solicitar reapertura, autorizar, editar y consultar versión anterior | Edición bloqueada sin reapertura y nueva aprobación obligatoria |
| PF-12 | Taller intenta aprobar una OC o descargar evidencia | Acceso denegado por el servidor |
| PF-13 | Confirmar salida sin evidencia; rechazar y conservar stock | Requiere base de pruebas |
| PF-14 | Repetir confirmación de recepción/salida; no duplicar movimientos | Requiere base de pruebas |

Para cada ejecución registrar fecha, rol, datos iniciales, resultado esperado,
resultado obtenido y una captura. Guardar capturas sin datos personales en
`docs/evidencias/`. Ningún caso se considera aprobado hasta observar su resultado.

## Escenarios adicionales de integridad

- Dos confirmaciones simultáneas de la misma salida: una sola entrega.
- Recepción de producto ajeno a la OC o por encima del saldo: rechazo.
- Salida con dos productos, segundo sin inventario: rollback de ambos.
- Fallo al resolver una solicitud después del procedimiento: rollback conjunto.
- Archivo con MIME de PNG y contenido HTML: rechazo.
- Descarga de evidencia con otro perfil: 403.
- Reapertura y edición de cantidades/precios: conservar versión anterior y exigir
  aprobación aunque el cliente intente desactivarla.
- Anular un ingreso consumido: rechazo sin alterar el stock.

## Requisitos no funcionales

| Requisito | Implementación o criterio de aceptación |
|---|---|
| RNF-01 | Spring Security, BCrypt costo 12, usuarios desactivados sin acceso |
| RNF-02 | Permisos verificados mediante `@PreAuthorize`; pruebas de acceso |
| RNF-03 | Spring controla la transacción completa; bloqueos y restricciones SQL. Pendiente ensayo con MySQL |
| RNF-04 | Formularios, validaciones y mensajes de error; revisión manual pendiente |
| RNF-05 | Probar Chrome, Edge y Firefox a 390 px y 1366 px; pendiente |
| RNF-06 | Auditoría de altas y cambios críticos y movimientos trazables |
| RNF-07 | Procedimiento y scripts de respaldo/restauración en `docs/operacion.md` |
| RNF-08 | Límite 5 MB, firmas de formato, ruta privada y descarga con permisos |
| RNF-09 | Objetivo de aceptación: p95 de consultas menor a 2 s con 10 usuarios concurrentes y 1000 movimientos, en infraestructura registrada. Medición pendiente |
| RNF-10 | Controladores, servicios, modelos, repositorios, SQL y presentación separados |

## Correspondencia con la arquitectura del documento

La entrada HTML se sirve con Thymeleaf desde `templates/index.html`. JavaScript
gestiona las interacciones y consume la API. Spring Data JPA se usa para usuarios,
roles, permisos y escritura del catálogo. Los procedimientos de inventario se
invocan por JDBC dentro de la misma transacción Spring. MySQL almacena los datos;
las evidencias permanecen en un directorio privado del servidor.

El modelo conceptual conserva las doce entidades del documento. Las tablas
auxiliares añaden autorizaciones, detalles de recepción, evidencias, solicitudes,
historial y auditoría. No es necesario reducir el modelo físico a doce tablas.

## Aspectos del informe que requieren información del equipo

El presupuesto 3.2, responsables reales, cronograma ejecutado, capturas y
resultados de pruebas requieren datos del equipo. No son funcionalidades que
puedan completarse mediante código ni deben presentarse como ejecutadas.
