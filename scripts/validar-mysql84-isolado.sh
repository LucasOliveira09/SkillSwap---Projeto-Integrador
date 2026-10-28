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

# Exercita descoberta, negociação, conversa e reputação no MySQL descartável.
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

  patch_json() {
    caminho=$1
    bearer=$2
    corpo=$3
    tamanho=$(printf "%s" "$corpo" | wc -c | tr -d "[:space:]")
    printf "PATCH %s HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nAuthorization: Bearer %s\r\nContent-Type: application/json\r\nContent-Length: %s\r\nConnection: close\r\n\r\n%s" \
      "$caminho" "$bearer" "$tamanho" "$corpo" | busybox nc 127.0.0.1 8080
  }

  token_profissional=$(registrar "Profissional de validação" "profissional-$sufixo@skillswap.invalid")
  token_cliente=$(registrar "Cliente de validação" "cliente-$sufixo@skillswap.invalid")
  token_externo=$(registrar "Conta externa de validação" "externo-$sufixo@skillswap.invalid")

  perfil_json="{\"tituloProfissional\":\"Marceneiro profissional\",\"bio\":\"Móveis sob medida.\",\"cidade\":\"Joinville\",\"especialidades\":\"marcenaria, móveis planejados\",\"perfilPublico\":true}"
  resposta_perfil=$(patch_json /api/perfil "$token_profissional" "$perfil_json")
  printf "%s" "$resposta_perfil" | grep -q "HTTP/1.1 200"
  profissional_id=$(printf "%s" "$resposta_perfil" | sed -n "s/.*\\\"id\\\":\\([0-9][0-9]*\\),\\\"nome\\\".*/\\1/p")
  if [ -z "$profissional_id" ]; then
    echo "A API não devolveu o identificador do perfil atualizado." >&2
    exit 1
  fi
  profissionais=$(wget -qO- "$base/api/profissionais?busca=marceneiro&cidade=Joinville")
  printf "%s" "$profissionais" | grep -q "\"totalItens\":1"
  printf "%s" "$profissionais" | grep -q "\"tituloProfissional\":\"Marceneiro profissional\""
  if printf "%s" "$profissionais" | grep -q "\"email\""; then
    echo "A busca pública de profissionais expôs um endereço de e-mail." >&2
    exit 1
  fi
  busca_com_poucas_avaliacoes=$(wget -qO- "$base/api/profissionais?busca=marceneiro&cidade=Joinville&notaMinima=4.5")
  printf "%s" "$busca_com_poucas_avaliacoes" | grep -q "\"totalItens\":0"

  anuncio=$(wget -qO- --header="Content-Type: application/json" \
    --header="Authorization: Bearer $token_profissional" \
    --post-data="{\"titulo\":\"Aula de validação MySQL\",\"descricao\":\"Oferta descartável para validar notificações.\",\"categoria\":\"AULAS\",\"cidade\":\"Florianópolis\",\"modalidade\":\"VENDA\",\"preco\":10.00,\"disponibilidades\":[]}" \
    "$base/api/anuncios")
  anuncio_id=$(printf "%s" "$anuncio" | sed -n "s/.*\"id\":\\([0-9][0-9]*\\),\"titulo\".*/\\1/p")
  if [ -z "$anuncio_id" ]; then
    echo "A API não devolveu o anúncio temporário." >&2
    exit 1
  fi

  proposta=$(wget -qO- --header="Content-Type: application/json" \
    --header="Authorization: Bearer $token_cliente" \
    --post-data="{\"mensagemInicial\":\"Tenho interesse nesta oferta de validação.\"}" \
    "$base/api/anuncios/$anuncio_id/propostas")
  proposta_id=$(printf "%s" "$proposta" | sed -n "s/.*\\\"id\\\":\\([0-9][0-9]*\\),\\\"anuncioId\\\".*/\\1/p")
  if [ -z "$proposta_id" ]; then
    echo "A API não devolveu o identificador da proposta temporária." >&2
    exit 1
  fi

  notificacoes=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes" | grep -q "PROPOSTA_RECEBIDA"
  printf "%s" "$notificacoes" | grep -q "\"lida\":false"
  notificacao_id=$(printf "%s" "$notificacoes" | sed -n "s/.*\"itens\":\\[{\"id\":\\([0-9][0-9]*\\),\"tipo\":\"PROPOSTA_RECEBIDA\".*/\\1/p")
  if [ -z "$notificacao_id" ]; then
    echo "A notificação da proposta não foi encontrada na página do destinatário." >&2
    exit 1
  fi

  resposta_nao_autorizada=$(patch_json "/api/notificacoes/$notificacao_id" "$token_externo" "{\"lida\":true}")
  printf "%s" "$resposta_nao_autorizada" | grep -q "HTTP/1.1 404"
  notificacoes_inalteradas=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes_inalteradas" | grep -q "\"lida\":false"

  resposta_patch=$(patch_json "/api/notificacoes/$notificacao_id" "$token_profissional" "{\"lida\":true}")
  printf "%s" "$resposta_patch" | grep -q "HTTP/1.1 200"
  printf "%s" "$resposta_patch" | grep -q "\"lida\":true"
  notificacoes_atualizadas=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes_atualizadas" | grep -q "\"lida\":true"
  resposta_reabertura=$(patch_json "/api/notificacoes/$notificacao_id" "$token_profissional" "{\"lida\":false}")
  printf "%s" "$resposta_reabertura" | grep -q "HTTP/1.1 200"
  printf "%s" "$resposta_reabertura" | grep -q "\"lida\":false"
  notificacoes_reabertas=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/notificacoes?pagina=0&tamanho=20")
  printf "%s" "$notificacoes_reabertas" | grep -q "\"lida\":false"

  proposta_aceita=$(patch_json "/api/propostas/$proposta_id" "$token_profissional" "{\"status\":\"ACEITA\"}")
  printf "%s" "$proposta_aceita" | grep -q "\"status\":\"ACEITA\""
  acordos=$(wget -qO- --header="Authorization: Bearer $token_cliente" "$base/api/acordos?pagina=0&tamanho=20")
  acordo_id=$(printf "%s" "$acordos" | sed -n "s/.*\\\"itens\\\":\\[{\\\"id\\\":\\([0-9][0-9]*\\),\\\"propostaId\\\".*/\\1/p")
  if [ -z "$acordo_id" ]; then
    echo "A proposta aceita não criou um acordo para as partes." >&2
    exit 1
  fi
  wget -qO- --header="Content-Type: application/json" \
    --header="Authorization: Bearer $token_cliente" \
    --post-data="{\"conteudo\":\"Vamos agendar para sexta-feira.\"}" \
    "$base/api/acordos/$acordo_id/mensagens" >/dev/null
  mensagens=$(wget -qO- --header="Authorization: Bearer $token_profissional" \
    "$base/api/acordos/$acordo_id/mensagens?pagina=0&tamanho=20")
  printf "%s" "$mensagens" | grep -q "Vamos agendar para sexta-feira."

  confirmacao_cliente=$(patch_json "/api/acordos/$acordo_id" "$token_cliente" "{\"status\":\"CONCLUIDO\"}")
  printf "%s" "$confirmacao_cliente" | grep -q "\"status\":\"EM_ANDAMENTO\""
  confirmacao_profissional=$(patch_json "/api/acordos/$acordo_id" "$token_profissional" "{\"status\":\"CONCLUIDO\"}")
  printf "%s" "$confirmacao_profissional" | grep -q "\"status\":\"CONCLUIDO\""
  wget -qO- --header="Content-Type: application/json" \
    --header="Authorization: Bearer $token_cliente" \
    --post-data="{\"nota\":5,\"notaAnuncio\":4,\"depoimento\":\"Trabalho cuidadoso e pontual.\"}" \
    "$base/api/acordos/$acordo_id/avaliacoes" >/dev/null
  perfil_publico=$(wget -qO- "$base/api/perfis/$profissional_id")
  printf "%s" "$perfil_publico" | grep -q "\"mediaNotas\":5.0"
  printf "%s" "$perfil_publico" | grep -q "\"quantidadeAvaliacoes\":1"
  anuncio_publico=$(wget -qO- "$base/api/anuncios/$anuncio_id")
  printf "%s" "$anuncio_publico" | grep -q "\"mediaAvaliacoesAnuncio\":4.0"
  printf "%s" "$anuncio_publico" | grep -q "\"quantidadeAvaliacoesAnuncio\":1"
  busca_com_reputacao=$(wget -qO- "$base/api/profissionais?busca=marceneiro&cidade=Joinville")
  printf "%s" "$busca_com_reputacao" | grep -q "\"mediaNotas\":5.0"
  if printf "%s" "$busca_com_reputacao" | grep -q "\"email\""; then
    echo "A busca pública expôs e-mail após a avaliação." >&2
    exit 1
  fi
  printf "%s %s\n" "$profissional_id" "$anuncio_id" > /tmp/skillswap-validation-ids
  echo "Perfil, descoberta, reputação mínima, proposta, notificação, acordo, conversa, conclusão mútua e avaliação passaram em MySQL descartável."
'

docker restart "$api" >/dev/null
esperar_saude "$api"
docker exec "$api" sh -ec '
  set -- $(cat /tmp/skillswap-validation-ids)
  perfil=$(wget -qO- "http://127.0.0.1:8080/api/perfis/$1")
  printf "%s" "$perfil" | grep -q "\"mediaNotas\":5.0"
  printf "%s" "$perfil" | grep -q "\"quantidadeAvaliacoes\":1"
  anuncio=$(wget -qO- "http://127.0.0.1:8080/api/anuncios/$2")
  printf "%s" "$anuncio" | grep -q "\"mediaAvaliacoesAnuncio\":4.0"
  printf "%s" "$anuncio" | grep -q "\"quantidadeAvaliacoesAnuncio\":1"
  busca=$(wget -qO- "http://127.0.0.1:8080/api/profissionais?busca=marceneiro&cidade=Joinville")
  printf "%s" "$busca" | grep -q "\"mediaNotas\":5.0"
  echo "Avaliação, médias públicas e busca persistiram após reinício da API."
'

versao_mysql=$(docker exec "$banco" sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT VERSION()"')
versao_flyway=$(docker exec "$banco" sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -e "SELECT version FROM skillswap.flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1"')
if [ "$versao_flyway" != 14 ]; then
  echo "Flyway terminou em versão inesperada: $versao_flyway" >&2
  docker logs --tail 80 "$api" >&2 || true
  exit 1
fi

echo "MySQL $versao_mysql; Flyway V$versao_flyway; Hibernate validate, descoberta e fluxo REST de venda/reputação aprovados."
