# Austral Pilot

Sistema web de almacén y compras para Transportes Austral Group. El backend
implementa el flujo definido en el documento del proyecto: productos e
inventario por almacén, órdenes de compra con autorización gerencial,
recepciones e incidencias, requerimientos de Taller, salidas con evidencia,
anulaciones, reaperturas, auditoría y reportes.

## Arquitectura

- Java 17 y Spring Boot 4
- Spring Security con autenticación HTTP Basic y BCrypt
- Spring Data JPA para usuarios, roles y permisos
- JDBC transaccional para procedimientos de inventario
- MySQL 8
- Maven Wrapper

El esquema físico contiene 22 tablas, dos vistas, cinco procedimientos
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
| Productos | `GET/POST /productos` |
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

## Ejecutar pruebas

```powershell
.\mvnw.cmd test
```

La prueba automatizada valida que el esquema mantenga al menos 12 tablas y que
incluya procedimientos, triggers, trazabilidad y hash de contraseñas.

## Colaboración

Antes de trabajar ejecute `git pull`. Cree una rama por cambio y no confirme
archivos `.env`, contraseñas, evidencias, datos personales, `target` ni cachés.
Use pull requests para revisar los cambios antes de incorporarlos a `main`.
