# Austral Pilot

Aplicación Java/Spring Boot para gestión de producción, compras y ventas.

## Requisitos

- Java 17
- MySQL

## Configuración local

La aplicación lee su configuración desde variables de entorno. Consulte
`.env.example` para conocer los nombres disponibles. No copie credenciales
reales dentro de archivos versionados.

En PowerShell puede definirlas para la sesión actual:

```powershell
$env:DB_USERNAME = "usuario_local"
$env:DB_PASSWORD = "su_contrasena_local"
$env:MAIL_USERNAME = "cuenta@gmail.com"
$env:MAIL_PASSWORD = "contrasena_de_aplicacion"
.\mvnw.cmd spring-boot:run
```

Las variables de correo son necesarias solo para enviar cotizaciones por Gmail.

## Colaboración

1. Cree una rama para cada cambio.
2. No confirme archivos `.env`, credenciales ni datos personales.
3. Abra un pull request para revisión antes de integrar cambios.
4. Ejecute `.\mvnw.cmd test` antes de enviar el pull request.

## Advertencia de despliegue

Publicar el código fuente en GitHub no ejecuta la aplicación ni genera cargos.
Sin embargo, la autenticación actual es solo apta para desarrollo local. Lea
`SECURITY.md` antes de desplegarla en un servidor accesible desde Internet.
