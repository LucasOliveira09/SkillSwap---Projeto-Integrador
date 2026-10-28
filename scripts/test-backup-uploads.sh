#!/usr/bin/env bash
set -euo pipefail

raiz=$(cd "$(dirname "$0")/.." && pwd -P)
temporario=$(mktemp -d)
trap 'rm -rf -- "$temporario"' EXIT

mkdir -p "$temporario/bin" "$temporario/source" "$temporario/backups"
chmod 755 "$temporario/backups"
printf 'foto de teste\n' > "$temporario/source/foto.txt"

cat > "$temporario/bin/date" <<'DATE'
#!/bin/sh
printf '%s\n' 20261028T000000Z
DATE
chmod 700 "$temporario/bin/date"

cat > "$temporario/bin/docker" <<'DOCKER'
#!/bin/sh
set -eu

if [ "$1" = volume ] && [ "$2" = inspect ]; then
  exit 0
fi

if [ "$1" = run ]; then
  shift
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --mount)
        case "$2" in
          type=bind,src=*,dst=/backup)
            montagem=${2#type=bind,src=}
            diretorio=${montagem%,dst=/backup}
            ;;
        esac
        shift 2
        ;;
      --env)
        arquivo=${2#BACKUP_FILE=}
        shift 2
        ;;
      *) shift ;;
    esac
  done
  if [ -n "${BACKUP_ACTIVE_MARKER:-}" ]; then
    : > "$BACKUP_ACTIVE_MARKER"
    sleep 2
  fi
  tar -czf "$diretorio/$arquivo" -C "$UPLOAD_FIXTURE" .
  exit 0
fi

echo "Chamada Docker inesperada." >&2
exit 1
DOCKER
chmod 700 "$temporario/bin/docker"

PATH="$temporario/bin:$PATH" \
UPLOAD_FIXTURE="$temporario/source" \
  "$raiz/scripts/backup-uploads.sh" skillswap-uploads-test "$temporario/backups"

permissao_diretorio=$(stat -c '%a' "$temporario/backups")
if [ "$permissao_diretorio" != 700 ]; then
  echo "Diretório de backup ficou com modo $permissao_diretorio; esperado 700." >&2
  exit 1
fi

arquivo=$(find "$temporario/backups" -maxdepth 1 -type f -name 'skillswap-uploads-*.tar.gz' -print -quit)
[ -n "$arquivo" ] || { echo "Arquivo de backup não foi criado." >&2; exit 1; }
[ "$(stat -c '%a' "$arquivo")" = 600 ] || { echo "Backup não está restrito ao proprietário." >&2; exit 1; }
[ "$(stat -c '%a' "$arquivo.sha256")" = 600 ] || { echo "Checksum não está restrito ao proprietário." >&2; exit 1; }
(cd "$temporario/backups" && sha256sum -c "$(basename "$arquivo").sha256")
tar -xOzf "$arquivo" ./foto.txt | grep -Fx 'foto de teste' >/dev/null

mkdir -p "$temporario/bin-checksum-falha" "$temporario/backups-falha"
cat > "$temporario/bin-checksum-falha/sha256sum" <<'CHECKSUM'
#!/bin/sh
exit 1
CHECKSUM
chmod 700 "$temporario/bin-checksum-falha/sha256sum"

if PATH="$temporario/bin-checksum-falha:$temporario/bin:$PATH" \
  UPLOAD_FIXTURE="$temporario/source" \
  "$raiz/scripts/backup-uploads.sh" skillswap-uploads-test "$temporario/backups-falha"; then
  echo "A falha de checksum deveria interromper o backup." >&2
  exit 1
fi

if find "$temporario/backups-falha" -maxdepth 1 -type f -print -quit | grep -q .; then
  echo "Falha de checksum deixou artefatos incompletos no diretório." >&2
  exit 1
fi

mkdir -p "$temporario/backups-concorrentes"
PATH="$temporario/bin:$PATH" \
UPLOAD_FIXTURE="$temporario/source" \
BACKUP_ACTIVE_MARKER="$temporario/docker-ativo" \
  "$raiz/scripts/backup-uploads.sh" skillswap-uploads-test "$temporario/backups-concorrentes" > "$temporario/primeiro.log" 2>&1 &
primeiro_pid=$!

tentativa=0
while [ ! -e "$temporario/docker-ativo" ] && [ "$tentativa" -lt 100 ]; do
  sleep 0.05
  tentativa=$((tentativa + 1))
done
if [ ! -e "$temporario/docker-ativo" ]; then
  kill "$primeiro_pid" 2>/dev/null || true
  echo "O primeiro backup não iniciou o Docker simulado." >&2
  exit 1
fi

if PATH="$temporario/bin:$PATH" \
  UPLOAD_FIXTURE="$temporario/source" \
  "$raiz/scripts/backup-uploads.sh" skillswap-uploads-test "$temporario/backups-concorrentes" > "$temporario/segundo.log" 2>&1; then
  segundo_status=0
else
  segundo_status=$?
fi

if wait "$primeiro_pid"; then
  primeiro_status=0
else
  primeiro_status=$?
fi
if [ "$primeiro_status" -eq "$segundo_status" ]; then
  echo "Execuções simultâneas não resultaram em exatamente um backup publicado." >&2
  exit 1
fi
[ "$(find "$temporario/backups-concorrentes" -maxdepth 1 -type f -name 'skillswap-uploads-*.tar.gz' | wc -l)" -eq 1 ] || {
  echo "A concorrência não preservou exatamente um arquivo de backup." >&2
  exit 1
}
[ "$(find "$temporario/backups-concorrentes" -maxdepth 1 -type f -name 'skillswap-uploads-*.tar.gz.sha256' | wc -l)" -eq 1 ] || {
  echo "A concorrência não preservou exatamente um checksum." >&2
  exit 1
}
if find "$temporario/backups-concorrentes" -maxdepth 1 -type f -name '*.partial-*' | grep -q .; then
  echo "A concorrência deixou arquivos temporários." >&2
  exit 1
fi

echo "Teste de permissões e integridade do backup de uploads passou."
