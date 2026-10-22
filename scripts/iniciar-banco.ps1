$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
Push-Location $raizProjeto
try {
    if (!(Test-Path -LiteralPath '.env')) {
        $senhaBanco = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
        $senhaRoot = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
        [IO.File]::WriteAllText((Join-Path $raizProjeto '.env'), "DB_PASSWORD=$senhaBanco`nMYSQL_ROOT_PASSWORD=$senhaRoot`n")
        Write-Host 'Credenciais locais geradas em .env (ignorado pelo Git).'
    }
    if (!(Select-String -LiteralPath '.env' -Pattern '^JWT_SECRET=.+' -Quiet)) {
        $chaveJwtSkillSwap = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
        [IO.File]::AppendAllText((Join-Path $raizProjeto '.env'), "`nJWT_SECRET=$chaveJwtSkillSwap`n")
    }
    docker compose up -d --wait mysql
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o MySQL. Verifique se o Docker Desktop está ativo.' }
} finally { Pop-Location }
