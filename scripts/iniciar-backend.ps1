param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
if (!$JavaHome -and (Test-Path 'C:/Program Files/Java/jdk-21/bin/javac.exe')) {
    $JavaHome = 'C:/Program Files/Java/jdk-21'
}
if (!$JavaHome -or !(Test-Path (Join-Path $JavaHome 'bin/javac.exe'))) {
    throw 'Informe um JDK 21 em JAVA_HOME ou no parâmetro -JavaHome.'
}
$env:JAVA_HOME = $JavaHome
$arquivoEnv = Join-Path $raizProjeto '.env'
if (Test-Path -LiteralPath $arquivoEnv) {
    foreach ($linha in Get-Content -LiteralPath $arquivoEnv) {
        if ($linha -match '^(DB_PASSWORD|DB_USER|DB_URL|PORT|SEED_ENABLED|JWT_SECRET|SERVER_ADDRESS|FLYWAY_BASELINE_ON_MIGRATE|SESSION_COOKIE_SECURE)=(.*)$') {
            [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
}
if (!$env:DB_PASSWORD) { throw 'Defina DB_PASSWORD ou execute scripts/iniciar-banco.ps1.' }
if (!$env:JWT_SECRET) { throw 'Defina JWT_SECRET ou execute scripts/iniciar-banco.ps1 para gerar uma chave local.' }
if (!$env:SESSION_COOKIE_SECURE) { $env:SESSION_COOKIE_SECURE = 'false' }
Push-Location (Join-Path $raizProjeto 'backend')
try {
    & ./mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'O backend não iniciou. Consulte a mensagem do Maven acima.' }
} finally { Pop-Location }
