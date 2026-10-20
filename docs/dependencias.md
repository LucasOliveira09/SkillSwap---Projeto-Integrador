# Dependências e verificação

A raiz usa npm e um único `package-lock.json`. A configuração `.npmrc` bloqueia scripts de ciclo de instalação para futuras instalações. Usar `npm ci` para reproduzir as versões; verificar build e testes após mudanças no lockfile.

## Achado conhecido em ferramentas de teste

Em 19/10/2026, `npm audit` reportou seis entradas de severidade alta, derivadas de um mesmo problema em `braces <=3.0.3`, trazido pelo watcher do Karma. A [advisory GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm) informa ausência de versão corrigida. As sugestões automáticas implicam downgrades incompatíveis; não aplicar `npm audit fix --force`.

O problema ocorre com padrões de expansão profundamente aninhados controlados por um atacante. Nesta base, os globs de testes são definidos pelo projeto; dados digitados no cadastro não são usados como padrões de arquivos. A dependência pertence às ferramentas de desenvolvimento, sem uso no bundle da aplicação. `npm audit --omit=dev` deve ser verificado separadamente.

Manter testes e servidor de desenvolvimento locais, sem permitir padrões de arquivos vindos de usuários. Não executar testes de repositórios ou configurações não confiáveis. Reavaliar até 26/10/2026, antes de expor ferramentas na rede ou ao mudar a versão do executor de testes. A escolha de Karma acompanha o executor estável gerado pela CLI 20; uma futura migração deverá preservar a cobertura de comportamento.
