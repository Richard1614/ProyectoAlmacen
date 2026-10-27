# Respaldo y recuperación

El respaldo comprende tanto MySQL como el directorio de evidencias. Ambos deben
capturarse sin escrituras concurrentes. Detener la aplicación antes de ejecutar
el respaldo y reanudarla después. Restringir el acceso a la carpeta de destino:
el archivo contiene datos del sistema y hashes de contraseñas.

```powershell
./tools/backup.ps1 -MySqlBin C:/xampp/mysql/bin -Destino D:/Respaldos/Austral
```

El script solicita la contraseña mediante `mysqldump -p`; no la almacena ni la
incluye en argumentos. Copiar el respaldo a un segundo dispositivo con acceso
restringido. Propuesta operativa: respaldo diario y antes de migraciones,
retención de 7 diarios y 4 semanales, a confirmar por el equipo.

Para recuperar, detener la aplicación y usar primero un entorno vacío de
recuperación. La importación SQL contiene las tablas, vistas, triggers y
procedimientos. El destino no debe contener tablas del sistema anterior.

```powershell
./tools/restore.ps1 -MySqlBin C:/xampp/mysql/bin -Respaldo D:/Respaldos/Austral/FECHA
```

Después de importar, copiar `evidencias/` al directorio indicado por
`EVIDENCE_DIRECTORY`. Los registros conservan rutas absolutas: si se cambia de
ubicación, actualizar `ruta_segura` a la nueva raíz dentro del entorno restaurado
antes de habilitar usuarios. Comprobar conteos de movimientos, stock y SHA-256 de
las evidencias y descargar una desde la aplicación. Registrar fecha y resultado.

Los scripts no se han ejecutado contra la base del usuario. La recuperación
debe ensayarse antes de considerar RNF-07 validado.
