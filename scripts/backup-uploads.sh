#!/bin/sh
set -eu
umask 077

if [ "$#" -ne 2 ]; then
  echo "Uso: $0 <nome-do-volume-docker> <diretorio-de-backup>" >&2
  exit 2
fi

volume=$1
diretorio=$2

case "$volume" in
  ""|*[!A-Za-z0-9_.-]*)
    echo "Nome de volume Docker inválido." >&2
    exit 2
    ;;
esac

for ferramenta in docker tar sha256sum; do
  if ! command -v "$ferramenta" >/dev/null 2>&1; then
    echo "Ferramenta obrigatória não encontrada: $ferramenta" >&2
    exit 1
  fi
done

if ! docker volume inspect "$volume" >/dev/null 2>&1; then
  echo "Volume Docker não encontrado: $volume" >&2
  exit 1
fi

mkdir -p -- "$diretorio"
diretorio=$(cd -- "$diretorio" && pwd -P)
instante=$(date -u +%Y%m%dT%H%M%SZ)
arquivo="skillswap-uploads-$instante.tar.gz"
temporario="$arquivo.partial-$$"
checksum_temporario="$arquivo.sha256.partial-$$"

if [ -e "$diretorio/$arquivo" ] || [ -e "$diretorio/$temporario" ] || [ -e "$diretorio/$checksum_temporario" ]; then
  echo "Já existe um backup com este nome. Tente novamente em alguns segundos." >&2
  exit 1
fi

limpar() {
  rm -f -- "$diretorio/$temporario"
  rm -f -- "$diretorio/$checksum_temporario"
}
trap limpar EXIT
trap 'exit 1' HUP INT TERM

docker run --rm --pull=missing --network none \
  --mount "type=volume,src=$volume,dst=/source,readonly" \
  --mount "type=bind,src=$diretorio,dst=/backup" \
  --env "BACKUP_FILE=$temporario" \
  busybox:1.37.0 sh -ec 'tar -czf "/backup/$BACKUP_FILE" -C /source .'

if [ ! -s "$diretorio/$temporario" ]; then
  echo "O arquivo de backup ficou vazio." >&2
  exit 1
fi

tar -tzf "$diretorio/$temporario" >/dev/null
mv -- "$diretorio/$temporario" "$diretorio/$arquivo"
sha256sum "$diretorio/$arquivo" > "$diretorio/$checksum_temporario"
mv -- "$diretorio/$checksum_temporario" "$diretorio/$arquivo.sha256"
chmod 600 "$diretorio/$arquivo" "$diretorio/$arquivo.sha256"
trap - EXIT HUP INT TERM

echo "Backup validado: $diretorio/$arquivo"
echo "Checksum: $diretorio/$arquivo.sha256"
