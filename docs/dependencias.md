# Dependências e verificação

A raiz usa npm e um único `package-lock.json`. A configuração `.npmrc` bloqueia scripts de ciclo de instalação. Use `npm ci` para reproduzir as versões e verifique build e testes após mudanças no lockfile.

## Auditoria e correções em 28/10/2026

`npm audit` encontrou cinco avisos conhecidos (quatro altos e um baixo) em ferramentas de desenvolvimento: Angular CLI/Build, Vite, `body-parser` e dependências relacionadas. `npm audit fix --ignore-scripts` aplicou apenas atualizações compatíveis com as faixas já declaradas; não houve uso de `--force` nem alteração de `package.json`. As versões corrigidas incluem Angular CLI/Build `20.3.41`, Vite `7.3.7`, `body-parser` `1.20.9`/`2.4.0` e MCP SDK `1.32.1`.

Depois, `npm ci` instalou o lockfile atualizado com scripts bloqueados. `npm audit` e `npm audit --omit=dev` encontraram zero vulnerabilidades. `npm audit signatures` verificou 542 assinaturas e 120 atestações. `npm run typecheck`, `npm run build` e `npm run test:ci` passaram; a suíte executou 98 testes no Chrome Headless 149.

O alerta registrado anteriormente para `braces <=3.0.3` foi retirado do banco de advisories do GitHub em 23/10/2026 por não ser considerado praticamente explorável. A versão transitiva continua em 3.0.3 e o lockfile não aplica override; a auditoria atual não reporta vulnerabilidade para ela. Consulte a [advisory retirada GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm).

Repetir as auditorias antes de cada promoção. Atualizações futuras devem ser revistas pelo diff do lockfile e testadas sem forçar downgrades ou cruzar versões principais.
