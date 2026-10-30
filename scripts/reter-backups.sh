#!/usr/bin/env bash
set -euo pipefail
umask 077

uso() {
  cat >&2 <<'EOF'
Uso:
  scripts/reter-backups.sh <diretorio> [--aplicar]
  scripts/reter-backups.sh <diretorio> --confirmar-restauracao <arquivo>

Sem --aplicar, mostra o que seria removido. Confirme a restauração do arquivo
mais recente de cada tipo somente depois de restaurá-lo e verificá-lo em um
ambiente isolado.
EOF
}

if [[ $# -lt 1 || $# -gt 3 ]]; then
  uso
  exit 2
fi

diretorio_arg=$1
modo=${2:---simular}
arquivo_confirmado=${3:-}

if [[ ! -d "$diretorio_arg" || -L "$diretorio_arg" ]]; then
  echo "O diretório deve existir e não pode ser um link simbólico." >&2
  exit 2
fi
diretorio=$(cd -- "$diretorio_arg" && pwd -P)

confirmar_integridade() {
  local arquivo=$1 nome checksum esperado atual linha
  nome=${arquivo##*/}
  checksum="$arquivo.sha256"

  if [[ ! -f "$arquivo" || -L "$arquivo" || ! -f "$checksum" || -L "$checksum" ]]; then
    echo "Arquivo ou checksum ausente, não regular ou simbólico: $nome" >&2
    return 1
  fi
  read -r esperado linha < "$checksum" || true
  if [[ ! "$esperado" =~ ^[[:xdigit:]]{64}$ || ( "$linha" != "$nome" && "$linha" != "*$nome" ) ]]; then
    echo "Formato de checksum inválido para $nome." >&2
    return 1
  fi

  atual=$(sha256sum -- "$arquivo")
  atual=${atual%% *}
  if [[ "${atual,,}" != "${esperado,,}" ]]; then
    echo "Checksum não confere: $nome" >&2
    return 1
  fi

  case "$nome" in
    skillswap-*.sql.gz) gzip -t -- "$arquivo" ;;
    skillswap-uploads-*.tar.gz) tar -tzf "$arquivo" >/dev/null ;;
    *) echo "Formato de backup não reconhecido: $nome" >&2; return 1 ;;
  esac
}

if [[ "$modo" == "--confirmar-restauracao" ]]; then
  if [[ $# -ne 3 || "$arquivo_confirmado" != "${arquivo_confirmado##*/}" || "$arquivo_confirmado" == -* ]]; then
    uso
    exit 2
  fi
  arquivo="$diretorio/$arquivo_confirmado"
  confirmar_integridade "$arquivo"
  digest=$(sha256sum -- "$arquivo")
  digest=${digest%% *}
  marcador="$arquivo.restore-verified"
  temporario="$marcador.partial.$$"
  printf '%s\n' "$digest" > "$temporario"
  chmod 600 "$temporario"
  mv -- "$temporario" "$marcador"
  echo "Restauração confirmada para $arquivo_confirmado."
  exit 0
fi

if [[ "$modo" != "--simular" && "$modo" != "--aplicar" ]] || [[ $# -eq 3 ]]; then
  uso
  exit 2
fi

shopt -s nullglob
arquivos=("$diretorio"/skillswap-*.sql.gz "$diretorio"/skillswap-uploads-*.tar.gz)
if [[ ${#arquivos[@]} -eq 0 ]]; then
  echo "Nenhum backup compatível encontrado em $diretorio." >&2
  exit 1
fi

declare -A dia_por_arquivo mes_por_arquivo tipo_por_arquivo
declare -A ultimo_por_dia ultimo_por_mes preservar
declare -a nomes=()

for arquivo in "${arquivos[@]}"; do
  nome=${arquivo##*/}
  if [[ "$nome" =~ ^skillswap-uploads-([0-9]{8}T[0-9]{6}Z)\.tar\.gz$ ]]; then
    carimbo=${BASH_REMATCH[1]}
    tipo=uploads
  elif [[ "$nome" =~ ^skillswap-[A-Za-z0-9_]+-([0-9]{8}T[0-9]{6}Z)\.sql\.gz$ ]]; then
    carimbo=${BASH_REMATCH[1]}
    tipo=mysql
  else
    echo "Nome de backup não reconhecido: $nome" >&2
    exit 1
  fi

  if ! date -u -d "${carimbo:0:8} ${carimbo:9:2}:${carimbo:11:2}:${carimbo:13:2}" +%s >/dev/null 2>&1; then
    echo "Data inválida no nome do backup: $nome" >&2
    exit 1
  fi
  confirmar_integridade "$arquivo"
  nomes+=("$nome")
  dia_por_arquivo["$nome"]=${carimbo:0:8}
  mes_por_arquivo["$nome"]=${carimbo:0:6}
  tipo_por_arquivo["$nome"]=$tipo
done

IFS=$'\n' nomes=($(printf '%s\n' "${nomes[@]}" | sort))
unset IFS
limite_dia=$(date -u -d '29 days ago' +%Y%m%d)

for tipo in mysql uploads; do
  mais_recente=
  for nome in "${nomes[@]}"; do
    [[ "${tipo_por_arquivo[$nome]}" == "$tipo" ]] || continue
    mais_recente=$nome
    dia=${dia_por_arquivo[$nome]}
    if [[ "$dia" > "$limite_dia" || "$dia" == "$limite_dia" ]]; then
      ultimo_por_dia["$tipo:$dia"]=$nome
    else
      ultimo_por_mes["$tipo:${mes_por_arquivo[$nome]}"]=$nome
    fi
  done

  [[ -n "$mais_recente" ]] || continue
  marcador="$diretorio/$mais_recente.restore-verified"
  atual=$(sha256sum -- "$diretorio/$mais_recente")
  atual=${atual%% *}
  if [[ "$modo" == "--aplicar" && ( ! -f "$marcador" || -L "$marcador" || "$(<"$marcador")" != "$atual" ) ]]; then
    echo "Antes da retenção, restaure o backup mais recente de $tipo em ambiente isolado e confirme-o com --confirmar-restauracao: $mais_recente" >&2
    exit 1
  fi
done

for chave in "${!ultimo_por_dia[@]}"; do
  preservar["${ultimo_por_dia[$chave]}"]=1
done

for tipo in mysql uploads; do
  meses=()
  for chave in "${!ultimo_por_mes[@]}"; do
    [[ "$chave" == "$tipo:"* ]] && meses+=("${chave#*:}")
  done
  if [[ ${#meses[@]} -gt 0 ]]; then
    IFS=$'\n' meses=($(printf '%s\n' "${meses[@]}" | sort -r))
    unset IFS
    for mes in "${meses[@]:0:12}"; do
      preservar["${ultimo_por_mes[$tipo:$mes]}"]=1
    done
  fi
done

remover=()
for nome in "${nomes[@]}"; do
  [[ -n "${preservar[$nome]:-}" ]] || remover+=("$nome")
done

if [[ ${#remover[@]} -eq 0 ]]; then
  echo "Retenção concluída: nenhum backup excedente; todos os arquivos passaram na validação."
  exit 0
fi

if [[ "$modo" == "--simular" ]]; then
  printf 'Seriam removidos (%s):\n' "${#remover[@]}"
  printf '  %s\n' "${remover[@]}"
  echo "Simulação: nenhum arquivo foi removido. A poda real também exige confirmação da restauração dos backups mais recentes."
  exit 0
fi

for nome in "${remover[@]}"; do
  arquivo="$diretorio/$nome"
  confirmar_integridade "$arquivo"
  rm -f -- "$arquivo" "$arquivo.sha256" "$arquivo.restore-verified"
  echo "Removido: $nome e arquivos auxiliares."
done
