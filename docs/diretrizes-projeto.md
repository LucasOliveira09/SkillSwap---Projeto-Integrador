# Diretrizes do SkillSwap

Referências incorporadas em 19/10/2026 a pedido do usuário. Consulte o [índice dos PDFs](referencias/README.md) para acessar os originais.

## Como aplicar os materiais

Este documento consolida orientações de aula para a continuidade do projeto. Os comandos dos PDFs são exemplos para uma etapa de implementação, não ações executadas nesta adoção. As entidades biblioteca, livros, atletas, treinos e projetos também são exemplos; não definem o domínio do SkillSwap.

O guia textual de Angular Material é o roteiro operacional mais detalhado. Os slides complementam os conceitos. Em eventuais diferenças de sintaxe ou configuração, conferir a documentação oficial da versão escolhida antes de implementar. Não reproduzir automaticamente capturas de tela ou afirmações gerais dos slides.

## Proposta e apresentação

Fonte: **Proposta de Apresentação**, páginas 2 a 12.

A proposta deve relacionar um problema real, o valor da solução e sua viabilidade. Preparar de 8 a 12 slides, agrupando assuntos quando necessário, nesta ordem sugerida:

1. Capa: nome do projeto, integrantes, curso, disciplina e data reais.
2. Contexto e ambiente em que o problema ocorre.
3. Problema e pessoas afetadas.
4. Justificativa e impactos do problema.
5. Objetivo central.
6. Público-alvo.
7. Solução proposta.
8. De quatro a seis funcionalidades principais, viáveis no prazo disponível.
9. Diferencial ligado ao problema e ao público.
10. Tecnologias e responsabilidades das camadas de interface, lógica e dados.
11. Protótipos ou wireframes compreensíveis dos fluxos principais.
12. Conclusão com impacto esperado, benefícios e viabilidade de execução.

Para esta etapa, priorizar a compreensão do fluxo no protótipo. Não apresentar funcionalidades planejadas como já implementadas, nem adotar automaticamente as tecnologias ilustradas no exemplo dos slides.

## Arquitetura Angular

Fonte: **Angular Arquitetura**, páginas 3 e 5 a 18; adaptação standalone no **Guia de Angular Material**, página 6.

- Usar os conceitos de SPA, componentes e roteamento interno como base da interface Angular.
- Organizar telas e serviços por domínio funcional, com responsabilidades claras.
- Separar template HTML, estilos e comportamento TypeScript dos componentes.
- Planejar rotas para as telas e carregamento sob demanda nas funcionalidades em que fizer sentido.
- O modelo didático principal usa NgModules, módulo raiz, módulos de funcionalidade e rotas internas. A escolha entre NgModules e standalone deve ser registrada antes da criação da aplicação.
- Em NgModules, declarar cada componente não standalone em um único módulo. Importar e exportar os recursos necessários no escopo consumidor.
- Em standalone, importar as dependências do template no próprio componente e preservar esse modelo; não colocar o componente em `declarations`.
- Na implementação da navegação interna, verificar os mecanismos do Angular Router na documentação da versão adotada; os esquemas visuais dos slides não substituem a configuração real.

## Angular Material e cadastro

Fontes: **Guia de Angular Material**, páginas 1 a 7; **Integração Angular Material**, páginas 2 a 17.

### Preparação e instalação futura

1. Ter uma aplicação Angular funcionando e um commit de referência antes da integração.
2. Executar a instalação na pasta que contém `angular.json` e `package.json`.
3. Verificar versões de Angular, Material e CDK; escolher versões compatíveis. Angular 20 é apenas o exemplo dos materiais.
4. Usar o instalador `ng add` com a versão adequada, sem forçar conflitos de dependências.
5. Escolher um tema pronto para a atividade e revisar as configurações de tipografia e animações produzidas pelo instalador. Não duplicar temas, módulos ou providers.

### Composição e comportamento

- Escolher uma tela de cadastro do domínio real do SkillSwap, com campos e regras definidos pela equipe.
- Usar cartão, campos com rótulos e botões Material: `MatCardModule`, `MatFormFieldModule`, `MatInputModule` e `MatButtonModule` são os recursos do exemplo.
- Importar os recursos no módulo responsável pela tela ou no componente standalone, conforme a arquitetura adotada.
- O exemplo usa `FormsModule` e `[(ngModel)]`, interpolação, `(click)` e `[disabled]`. Preservar os bindings e métodos do projeto ao adaptar a interface.
- Em formulário com `ngModel`, atribuir um `name` próprio a cada controle. Usar `matInput` em inputs e textareas dentro de `mat-form-field`.
- Validar os campos obrigatórios, incluindo valores compostos apenas de espaços quando aplicável, e impedir envio inválido.
- Manter a chamada ao serviço de cadastro. Exibir confirmação somente após sucesso da gravação; sem persistência, indicar que é simulação.
- A ação Limpar deve limpar os campos e a mensagem da tela. Exibir feedback de estado acessível, como o `role="status"` do exemplo.
- Usar o controle de fluxo compatível com o projeto: o guia mostra `*ngIf` e a alternativa `@if`.
- Usar CSS local para largura, espaçamento e organização do layout, com campos e ações utilizáveis em telas menores.
- Corrigir imports e configuração quando ocorrerem erros; não esconder componentes desconhecidos com `CUSTOM_ELEMENTS_SCHEMA`.

### Critérios de aceite da primeira tela

- [ ] Aplicação compila e abre a rota de cadastro sem erros.
- [ ] Tema aplicado; cartão, campos com rótulos e botões visíveis e legíveis.
- [ ] Valores dos campos e eventos refletem o comportamento esperado.
- [ ] Campos obrigatórios inválidos ou apenas com espaços impedem o envio, conforme as regras do domínio.
- [ ] Cadastro válido aciona o serviço e confirma apenas após persistência, ou informa claramente a simulação.
- [ ] Limpar reinicia os campos e o feedback.
- [ ] Tela utilizável em janela estreita e navegação existente preservada.
- [ ] Equipe consegue explicar imports, bindings e fluxo do cadastro.
- [ ] Verificações realizadas e alterações registradas em commits.

## Sequência de continuidade

| Etapa | Entrega esperada | Situação |
| --- | --- | --- |
| 1. Referências | PDFs preservados e orientações consolidadas | Concluída nesta adoção |
| 2. Definição do SkillSwap | Problema, público, objetivo, diferencial e 4 a 6 funcionalidades | Pendente |
| 3. Proposta e protótipos | Roteiro de 8 a 12 slides e fluxos compreensíveis | Pendente |
| 4. Base técnica | Versões, decisão NgModules/standalone, aplicação inicial e rotas | Pendente |
| 5. Cadastro com Material | Tela do domínio atendendo aos critérios acima | Pendente |
| 6. Persistência e evolução | Serviços, armazenamento e demais funcionalidades definidas | Pendente |

Ainda não há código de aplicação nem configuração de build ou testes neste repositório. Backend, banco de dados, autenticação, entidades e regras de troca permanecem em aberto; os PDFs não estabelecem essas decisões para o SkillSwap.
