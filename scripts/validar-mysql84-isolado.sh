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

# Exercita a notificação criada por uma proposta e sua leitura persistida no MySQL descartável.
docker exec "$api" sh -ec '
  set -eu
  base=http://127.0.0.1:8080
  sufixo="$(date +%s)-$$"
  senha=validacao-temporaria-123

  registrar() {
    nome=$1
    email=$2
    resposta=$(wget -qO- --header="Content-Type: application/json" \
      --post-data="{\"nome\":\"$nome\",\"email\":\"$email\",\"senha\":\"$senha\"}" \
      "$base/api/usuarios")
    token=$(printf "%s" "$resposta" | sed -n "s/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p")
    if [ -z "$token" ]; then
      echo "A API não devolveu o token da conta temporária." >&2
      exit 1
    fi
    printf "%s" "$token"
  }

  token_profissional=$(registrar "Profissional de validação" "profissional-$sufixo@skillswap.invalid")
  token_cliente=$(registrar "Cliente de validação" "cliente-$sufixo@skillswap.invalid")
  token_externo=$(registrar "Conta externa de validação" "externo-$sufixo@skillswap.invalid")

  anuncio=$(wget -qO- --header="Content-Type: application/json" \
    --header="Authorization: Bearer $token_profissional" \
    --post-data="{\"titulo\":\"Aula de validação MySQL\",\"descricao\":\"Oferta descartável para validar notificações.\",\"categoria\":\"AULAS\",\"cidade\":\"Florianópolis\",\"modalidade\":\"VENDA\",\"preco\":10.00,\"disponibilidades\":[]}" \
    "$base/api/anuncios")
  anuncio_id=$(printf "%s" "$anuncio" | sed -n "s/.*\"id\":\\([0-9][0-9]*\\),\"titulo\".*/\\1/p")
  if [ -z "$anuncio_id" ]; then
    echo "A API não devolveu o anúncio temporário." >&2
    exit 1
  fi

  wget -qO- --header="Content-Type: application/json" \
    --header="Authorization: Bearer $token_cliente" \
    --post-data="{\"mensagemInicial\":\"Tenho interesse nesta oferta de validação.\"}" \
    "$base/api/anuncios/$anuncio_id/propostas" >/dev/null

  notificacoes=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes" | grep -q "PROPOSTA_RECEBIDA"
  printf "%s" "$notificacoes" | grep -q "\"lida\":false"
  notificacao_id=$(printf "%s" "$notificacoes" | sed -n "s/.*\"itens\":\\[{\"id\":\\([0-9][0-9]*\\),\"tipo\":\"PROPOSTA_RECEBIDA\".*/\\1/p")
  if [ -z "$notificacao_id" ]; then
    echo "A notificação da proposta não foi encontrada na página do destinatário." >&2
    exit 1
  fi

  resposta_nao_autorizada=$(printf "PATCH /api/notificacoes/%s HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nAuthorization: Bearer %s\r\nContent-Type: application/json\r\nContent-Length: 13\r\nConnection: close\r\n\r\n{\"lida\":true}" \
    "$notificacao_id" "$token_externo" | busybox nc 127.0.0.1 8080)
  printf "%s" "$resposta_nao_autorizada" | grep -q "HTTP/1.1 404"
  notificacoes_inalteradas=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes_inalteradas" | grep -q "\"lida\":false"

  resposta_patch=$(printf "PATCH /api/notificacoes/%s HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nAuthorization: Bearer %s\r\nContent-Type: application/json\r\nContent-Length: 13\r\nConnection: close\r\n\r\n{\"lida\":true}" \
    "$notificacao_id" "$token_profissional" | busybox nc 127.0.0.1 8080)
  printf "%s" "$resposta_patch" | grep -q "HTTP/1.1 200"
  printf "%s" "$resposta_patch" | grep -q "\"lida\":true"
  notificacoes_atualizadas=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes_atualizadas" | grep -q "\"lida\":true"
  resposta_reabertura=$(printf "PATCH /api/notificacoes/%s HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nAuthorization: Bearer %s\r\nContent-Type: application/json\r\nContent-Length: 14\r\nConnection: close\r\n\r\n{\"lida\":false}" \
    "$notificacao_id" "$token_profissional" | busybox nc 127.0.0.1 8080)
  printf "%s" "$resposta_reabertura" | grep -q "HTTP/1.1 200"
  printf "%s" "$resposta_reabertura" | grep -q "\"lida\":false"
  notificacoes_reabertas=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes_reabertas" | grep -q "\"lida\":false"
  echo "Notificação criada, listada, protegida contra outra conta, marcada como lida e reaberta no MySQL descartável."
'

versao_mysql=$(docker exec "$banco" sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT VERSION()"')
versao_flyway=$(docker exec "$banco" sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT version FROM skillswap.flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"')
if [ "$versao_flyway" != 14 ]; then
  echo "Flyway terminou em versão inesperada: $versao_flyway" >&2
  docker logs --tail 80 "$api" >&2 || true
  exit 1
fi

echo "MySQL $versao_mysql; Flyway V$versao_flyway; Hibernate validate e API GET /api/anuncios aprovados."
