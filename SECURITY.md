# Seguridad

No publique credenciales, contraseñas, tokens ni archivos `.env`. Configure la
base de datos y el correo mediante variables de entorno, usando `.env.example`
solo como referencia.

Este proyecto académico todavía no está preparado para exponerse directamente
a Internet: la autenticación existente usa tokens simples y las contraseñas de
la base de datos no están cifradas. Ejecútelo únicamente en un entorno local o
una red controlada hasta migrar la autenticación a Spring Security, sesiones
seguras y hash de contraseñas con BCrypt o Argon2.

Si descubre una vulnerabilidad, comuníquela de forma privada al responsable del
repositorio y no abra un issue público con detalles explotables.
