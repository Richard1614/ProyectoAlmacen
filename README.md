# Austral Pilot

Sistema web de almacén y compras para Transportes Austral Group. El backend
implementa el flujo definido en el documento del proyecto: productos e
inventario por almacén, órdenes de compra con autorización gerencial,
recepciones e incidencias, requerimientos de Taller, salidas con evidencia,
anulaciones, reaperturas, auditoría y reportes.

## Arquitectura

- Java 17 y Spring Boot 4
- Spring Security con autenticación HTTP Basic y BCrypt
- Spring Data JPA para autenticación y perfiles de usuario
- JDBC transaccional para procedimientos de inventario
- MySQL 8
- Interfaz responsive en HTML, CSS y JavaScript, con Bootstrap 5.3.8 y recursos visuales locales
- Maven Wrapper

El esquema físico contiene 24 tablas, dos vistas, cinco procedimientos
almacenados y cinco triggers. Está en `database/01_schema.sql`.
La correspondencia completa con RF-01 a RF-20 está en
`docs/trazabilidad-requisitos.md`.

## Preparar MySQL

Ejecute `database/01_schema.sql` completo desde MySQL Workbench. El script crea
la base `austral_almacen`, los objetos de datos, roles, permisos y el almacén
principal. No crea contraseñas públicas.

## Configurar el entorno

Use `.env.example` únicamente como referencia. En PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/austral_almacen?useSSL=false&serverTimezone=America/Lima"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "SU_CLAVE_MYSQL"
$env:ADMIN_EMAIL = "admin@localhost.test"
$env:ADMIN_PASSWORD = "UNA_CLAVE_SEGURA_DE_12_O_MAS"
$env:ADMIN_NAME = "Administrador"
.\mvnw.cmd spring-boot:run
```

El primer inicio crea el administrador con hash BCrypt. Después retire
`ADMIN_PASSWORD` de la sesión o del entorno; no la guarde en Git.

## API principal

Todas las rutas usan el prefijo `/api/v1` y validan permisos en el servidor.

| Proceso | Rutas principales |
|---|---|
| Sesión | `GET /sesion` |
| Usuarios y permisos | `GET/POST /usuarios`, `PATCH /usuarios/{id}/rol`, `PUT /roles/{id}/permisos` |
| Productos | `GET/POST /productos`, `PUT /productos/{id}` |
| Inventario | `GET /inventario` |
| Compras | `POST /compras/ordenes`, `POST /compras/ordenes/{id}/decision` |
| Reapertura | `POST /compras/ordenes/{id}/reabrir` |
| Recepción | `POST /recepciones`, `POST /recepciones/{id}/confirmar` |
| Taller | `POST /taller/requerimientos` |
| Salidas | `POST /salidas`, evidencia y confirmación |
| Anulaciones | `POST /movimientos/{id}/anular` |
| Reportes | `GET /reportes/stock-bajo`, `GET /reportes/trazabilidad` |

Las evidencias admiten JPG, PNG o PDF, con un máximo de 5 MB. Se guardan fuera
de los recursos públicos en `data/evidencias`, carpeta ignorada por Git.
Bootstrap se carga desde jsDelivr y la aplicación conserva sus estilos propios
como respaldo visual. La imagen de bienvenida está incluida en los recursos del
proyecto y no requiere conexión a un servicio de fotografías.

## Ejecutar pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas verifican permisos de la API, validaciones, hash de contraseñas,
reglas de edición, solicitudes y controles de evidencias. Los escenarios de
integración MySQL se omiten si no se configura `TEST_MYSQL_URL`. Para ejecutarlos:

```powershell
$env:TEST_MYSQL_URL = "jdbc:mysql://localhost:3306/"
$env:TEST_MYSQL_USER = "USUARIO_DE_PRUEBAS"
$env:TEST_MYSQL_PASSWORD = "CLAVE_DE_PRUEBAS"
.\mvnw.cmd test
```

La cuenta de pruebas necesita crear y eliminar bases. La suite crea una base
aleatoria `austral_test_*` y elimina únicamente esa base al terminar. No utiliza
`austral_almacen`. Ver escenarios y criterios en `docs/validacion-funcional.md`.

La interfaz se sirve mediante Thymeleaf; las interacciones usan JavaScript y la
API. JPA gestiona autenticación y escritura del catálogo; JDBC ejecuta los
procedimientos de inventario dentro de transacciones Spring compartidas.

En instalaciones existentes aplicar `database/04_integridad_historial.sql`
después de `03_requirements_update.sql`. Hacer primero un respaldo según
`docs/operacion.md`. La migración añade historial y reemplaza procedimientos,
sin borrar datos. Toda edición de OC vuelve a aprobación gerencial y conserva
la versión anterior. El historial anterior a esta migración no se puede reconstruir.

## Colaboración

Antes de trabajar ejecute `git pull`. Cree una rama por cambio y no confirme
archivos `.env`, contraseñas, evidencias, datos personales, `target` ni cachés.
Use pull requests para revisar los cambios antes de incorporarlos a `main`.
