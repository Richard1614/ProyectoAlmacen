param(
    [Parameter(Mandatory=$true)][string]$MySqlBin,
    [Parameter(Mandatory=$true)][string]$Respaldo,
    [string]$Usuario='root',
    [string]$Base='austral_almacen'
)
$ErrorActionPreference='Stop'
if ($Base -notmatch '^[A-Za-z0-9_]+$') { throw 'Nombre de base inválido.' }
$sql=(Resolve-Path -LiteralPath (Join-Path $Respaldo 'database.sql')).Path.Replace('\','/')
if ($sql.Contains('"') -or $sql.Contains(';')) { throw 'Ruta de respaldo inválida.' }
Write-Output 'Utilice únicamente una base vacía de recuperación. La aplicación debe estar detenida.'
& (Join-Path $MySqlBin 'mysql.exe') --user=$Usuario -p --database=$Base --execute="source $sql"
if ($LASTEXITCODE -ne 0) { throw 'La restauración SQL no se completó.' }
Write-Output "SQL restaurado. Copie evidencias/ al directorio privado configurado y verifique las rutas y hashes según docs/operacion.md."
