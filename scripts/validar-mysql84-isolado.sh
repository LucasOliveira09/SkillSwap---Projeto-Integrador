#!/bin/sh
set -eu
umask 077

if [ "$#" -ne 1 ]; then
  echo "Uso: $0 <arquivo-env-da-prévia>" >&2
  exit 2
fi

arquivo_env=$1
if [ ! -r "$arquivo_env" ]; then
  echo "Arquivo .env não encontrado ou sem permissão de leitura." >&2
  exit 1
fi

# O .env é gerado localmente para a prévia e contém apenas atribuições KEY=value.
. "$arquivo_env"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD ausente}"
: "${DB_PASSWORD:?DB_PASSWORD ausente}"
: "${JWT_SECRET:?JWT_SECRET ausente}"

volume=skillswap-mysql84-validation
banco=skillswap-mysql84-validation
api=skillswap-api84-validation
rede=skillswap-preview-20261027_default
imagem=mysql:8.4@sha256:0f51e63e537a1f176423fa03ef559aabe6fa4372a53160a44bc7e75da7d15364

for ferramenta in docker grep sleep; do
  if ! command -v "$ferramenta" >/dev/null 2>&1; then
    echo "Ferramenta obrigatória não encontrada: $ferramenta" >&2
    exit 1
  fi
done

for nome in "$banco" "$api"; do
  if docker ps -a --format '{{.Names}}' | grep -Fxq "$nome"; then
    echo "Container de validação já existe: $nome" >&2
    exit 1
  fi
done
if docker volume inspect "$volume" >/dev/null 2>&1; then
  echo "Volume temporário já existe: $volume" >&2
  exit 1
fi

limpar() {
  docker rm -f "$api" "$banco" >/dev/null 2>&1 || true
  docker volume rm "$volume" >/dev/null 2>&1 || true
}
trap limpar EXIT
trap 'exit 1' HUP INT TERM

esperar_saude() {
  nome=$1
  tentativa=0
  while [ "$tentativa" -lt 60 ]; do
    estado=$(docker inspect --format '{{.State.Status}}' "$nome" 2>/dev/null || true)
    saude=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$nome" 2>/dev/null || true)
    if [ "$estado" = exited ] || [ "$estado" = dead ]; then
      docker logs --tail 80 "$nome" >&2 || true
      echo "Container não iniciou: $nome" >&2
      return 1
    fi
    if [ "$saude" = healthy ]; then
      return 0
    fi
    tentativa=$((tentativa + 1))
    sleep 5
  done
  docker logs --tail 80 "$nome" >&2 || true
  echo "Tempo excedido aguardando healthcheck: $nome" >&2
  return 1
}

docker volume create "$volume" >/dev/null
docker run -d \
  --name "$banco" \
  --network "$rede" \
  --network-alias mysql84 \
  --memory 768m \
  --mount "type=volume,src=$volume,dst=/var/lib/mysql" \
  --health-cmd='mysqladmin ping -h 127.0.0.1 --silent' \
  --health-interval=5s \
  --health-timeout=3s \
  --health-retries=30 \
  --health-start-period=20s \
  -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  -e MYSQL_DATABASE=skillswap \
  -e MYSQL_USER=skillswap \
  -e MYSQL_PASSWORD="$DB_PASSWORD" \
  "$imagem" >/dev/null
esperar_saude "$banco"

docker run -d \
  --name "$api" \
  --network "$rede" \
  --memory 768m \
  --health-cmd='wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1' \
  --health-interval=5s \
  --health-timeout=5s \
  --health-retries=18 \
  --health-start-period=40s \
  -e DB_URL=jdbc:mysql://mysql84:3306/skillswap \
  -e DB_USER=skillswap \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e SERVER_ADDRESS=0.0.0.0 \
  -e SEED_ENABLED=false \
  -e UPLOAD_DIR=/tmp/skillswap-mysql84-validation \
  skillswap-preview-20261027-api:latest >/dev/null
esperar_saude "$api"

docker exec "$api" sh -ec 'wget -q -O /dev/null http://127.0.0.1:8080/api/anuncios'
versao_mysql=$(docker exec "$banco" sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT VERSION()"')
versao_flyway=$(docker exec "$banco" sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT version FROM skillswap.flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"')
if [ "$versao_flyway" != 14 ]; then
  echo "Flyway terminou em versão inesperada: $versao_flyway" >&2
  docker logs --tail 80 "$api" >&2 || true
  exit 1
fi

echo "MySQL $versao_mysql; Flyway V$versao_flyway; Hibernate validate e API GET /api/anuncios aprovados."
