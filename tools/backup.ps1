param(
    [Parameter(Mandatory=$true)][string]$MySqlBin,
    [Parameter(Mandatory=$true)][string]$Destino,
    [string]$Usuario='root',
    [string]$Base='austral_almacen',
    [string]$Evidencias=(Join-Path $PSScriptRoot '../data/evidencias')
)
$ErrorActionPreference='Stop'
if (!(Test-Path -LiteralPath $Evidencias -PathType Container)) { throw 'No existe el directorio de evidencias. Revise la ruta antes del respaldo.' }
$carpeta=Join-Path $Destino (Get-Date -Format 'yyyyMMdd-HHmmss')
New-Item -ItemType Directory -Path $carpeta -ErrorAction Stop | Out-Null
$sql=Join-Path $carpeta 'database.sql'
& (Join-Path $MySqlBin 'mysqldump.exe') --user=$Usuario -p --single-transaction --routines --triggers --events --hex-blob --result-file=$sql $Base
if ($LASTEXITCODE -ne 0) { throw 'Falló el respaldo SQL. El respaldo no está completo.' }
Copy-Item -LiteralPath $Evidencias -Destination (Join-Path $carpeta 'evidencias') -Recurse
Get-ChildItem -LiteralPath $carpeta -Recurse -File | Get-FileHash -Algorithm SHA256 | Export-Csv -LiteralPath (Join-Path $carpeta 'sha256.csv') -NoTypeInformation
Write-Output "Respaldo completo: $carpeta"
