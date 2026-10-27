#!/bin/sh
set -eu
umask 077

if [ "$#" -ne 3 ]; then
  echo "Uso: $0 <container-mysql> <banco> <diretorio-de-backup>" >&2
  exit 2
fi

container=$1
banco=$2
diretorio=$3

case "$container" in
  ""|*[!A-Za-z0-9_.-]*)
    echo "Nome de container inválido." >&2
    exit 2
    ;;
esac

case "$banco" in
  ""|*[!A-Za-z0-9_]*)
    echo "Nome de banco inválido." >&2
    exit 2
    ;;
esac

for ferramenta in docker gzip sha256sum date; do
  if ! command -v "$ferramenta" >/dev/null 2>&1; then
    echo "Ferramenta obrigatória não encontrada: $ferramenta" >&2
    exit 1
  fi
done

mkdir -p -- "$diretorio"
diretorio=$(cd -- "$diretorio" && pwd -P)
chmod 700 "$diretorio"

instante=$(date -u +%Y%m%dT%H%M%SZ)
base="skillswap-$banco-$instante.sql.gz"
arquivo="$diretorio/$base"
dump_temporario="$diretorio/$base.sql.partial-$$"
temporario="$dump_temporario.gz"
checksum="$arquivo.sha256"
checksum_temporario="$checksum.partial-$$"
sucesso=0

if [ -e "$arquivo" ] || [ -e "$temporario" ] || [ -e "$dump_temporario" ] || [ -e "$checksum" ] || [ -e "$checksum_temporario" ]; then
  echo "Já existe um backup com este nome. Tente novamente em alguns segundos." >&2
  exit 1
fi

limpar() {
  rm -f -- "$temporario" "$dump_temporario" "$checksum_temporario"
  if [ "$sucesso" -ne 1 ]; then
    rm -f -- "$arquivo" "$checksum"
  fi
}
trap limpar EXIT
trap 'exit 1' HUP INT TERM

docker exec "$container" sh -ec '
  : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD ausente no container}"
  export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
  exec mysqldump --single-transaction --routines --triggers --events --hex-blob \
    --set-gtid-purged=OFF "$1"
' sh "$banco" > "$dump_temporario"

if [ ! -s "$dump_temporario" ]; then
  echo "O dump do MySQL ficou vazio." >&2
  exit 1
fi

gzip -n "$dump_temporario"
mv -- "$temporario" "$arquivo"
gzip -t "$arquivo"

(cd -- "$diretorio" && sha256sum "$base" > "$(basename "$checksum_temporario")")
mv -- "$checksum_temporario" "$checksum"
(cd -- "$diretorio" && sha256sum -c "$(basename "$checksum")")
chmod 600 "$arquivo" "$checksum"

sucesso=1
trap - EXIT HUP INT TERM
echo "Backup MySQL validado: $arquivo"
echo "Checksum: $checksum"
