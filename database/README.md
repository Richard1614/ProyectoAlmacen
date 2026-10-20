# Base de datos Austral Pilot

El script `01_schema.sql` crea 23 tablas, dos vistas, cinco procedimientos
almacenados y cinco triggers. Incluye roles y permisos, pero no incluye
usuarios ni contraseñas públicas. Es compatible con MySQL 8 y MariaDB/XAMPP.

Ejecute el script una vez con MySQL Workbench o con:

```powershell
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_seed.sql
mysql -u root -p < database/03_requirements_update.sql
```

Después configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL` y
`ADMIN_PASSWORD`. Al iniciar la aplicación por primera vez se crea el usuario
administrador con contraseña BCrypt; luego retire `ADMIN_PASSWORD` del entorno.

`02_seed.sql` puede ejecutarse nuevamente sin duplicar proveedores, productos o
inventarios. El stock comienza en cero para que todo ingreso quede respaldado
por una recepción y un movimiento auditable.

`03_requirements_update.sql` se aplica a instalaciones existentes para añadir
el flujo formal de solicitud y autorización de reaperturas de OC.
