# Seguridad

No publique credenciales, contraseñas, tokens ni archivos `.env`. Configure la
base de datos y el correo mediante variables de entorno, usando `.env.example`
solo como referencia.

La aplicación utiliza Spring Security HTTP Basic, contraseñas BCrypt y permisos
comprobados en el servidor. Configure HTTPS antes de utilizarla fuera de un
entorno local. Las credenciales de MySQL y del administrador inicial se reciben
mediante variables de entorno.

Las evidencias se guardan en un directorio privado, con tamaño máximo de 5 MB,
comprobación de cabecera del formato y descarga sujeta a permisos. La carga no
verifica automáticamente que el documento contenga una firma manuscrita; el
personal de Almacén debe revisar la constancia antes de confirmar.

Los procedimientos SQL deben invocarse dentro de una transacción controlada por
Spring. Si se usan manualmente, iniciar una transacción y confirmar solo si la
operación completa tuvo éxito; ante errores ejecutar ROLLBACK.

Si descubre una vulnerabilidad, comuníquela de forma privada al responsable del
repositorio y no abra un issue público con detalles explotables.
