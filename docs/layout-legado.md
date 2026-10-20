# Layout do projeto antigo

Pedido do usuário: manter o layout do [Skillswap antigo](https://github.com/LucasOliveira09/Skillswap) na aplicação Angular standalone. Referência inspecionada no commit `0714cb747cdf5dc546f3137a47b985872da5a1bd`.

## Referência e escopo

O catálogo de referência é `public/pagina-servicos.html`, com os estilos `servicos.css`, `menu.css` e `pro-overrides.css`. O cadastro de referência é `public/pagina-anuncio.html` e `anuncio.css`.

Reproduzir logo, tipografia Poppins/Angkor, azul `#255188`, verde `#8bc63f`, cabeçalho com busca e menu, banner recortado com círculos, cards com fotografias, categorias circulares, faixas de destaques e rodapé azul. Aplicar essa identidade ao cadastro existente e mostrar a prévia ao lado, preservando campos, validação e serviço demonstrativo.

Os exemplos de ofertas e perfis da vitrine são ilustrativos, separados dos anúncios da sessão. Busca e categorias filtram a vitrine no navegador. Não importar scripts antigos, autenticação, chamadas de API, armazenamento de sessão ou regras de negócio do legado. Não apresentar perfis, notas, promoções, aplicativos ou dados de contato do protótipo como serviços reais disponíveis.

## Implementação e aceite

1. Incorporar somente imagens usadas, fontes e suas licenças; criar componentes de apresentação standalone para banner, cards e coleções.
2. Adaptar cabeçalho, menu e rodapé. Busca deve funcionar por URL; funções ainda planejadas devem informar sua disponibilidade de forma explícita.
3. Substituir a apresentação do catálogo pela composição do legado, com exemplos identificados e anúncios da sessão preservados. Setas/carrosséis devem funcionar e não iniciar rotação automática.
4. Aplicar identidade ao cadastro e incluir prévia dos dados, mantendo salvar e limpar.
5. Validar build, tipos, testes, console, teclado e larguras de 320 a 1440 px. Comparar capturas com a referência e criar commits por objetivo.

Imagens derivadas do repositório indicado pelo usuário em `public/images/legado/`, com dimensões adequadas à web e conteúdo visual preservado. As imagens passaram de 18.431.610 para 1.974.809 bytes (redução de 89%); o GIF mantém a animação original. Fontes obtidas do [Google Fonts](https://github.com/google/fonts), com licenças OFL em `public/fonts/`; carregamento local, sem CDNs.

Padrões Angular: [inputs de componentes](https://angular.dev/guide/components/inputs), [signals e computed](https://v20.angular.dev/guide/signals), [parâmetros de rotas](https://v20.angular.dev/guide/routing/read-route-state) e componentes Material da versão instalada. Os estilos foram organizados por componente, sem copiar a cascata de sobrescritas do legado.

## Validação realizada

- Testes de busca, categorias e separação entre exemplos e anúncios da sessão executados em falha antes da implementação. A suíte passou com 22 testes, incluindo as verificações anteriores de cadastro, serviço e rotas.
- Build de produção, tipos da aplicação/testes e budgets verificados. Diálogos de conta, carrinho e ajuda são carregados sob demanda; o bundle inicial fica abaixo de 500 kB. Nenhuma dependência da aplicação foi acrescentada.
- Chrome real com perfil isolado: busca, resultados vazios, filtros, banner manual, paginação, cadastro de venda e troca, prévia, limpar, perda dos dados da sessão na recarga, menu com Escape e diálogos.
- Catálogo e cadastro conferidos em 320, 768, 1024 e 1440 px, sem rolagem horizontal da página. Coleções horizontais preservam sua própria rolagem e controles.
- Capturas do catálogo e cadastro comparadas com o legado. A composição foi mantida, com ajustes para contraste, responsividade e indicação de demonstração. O cadastro conserva os campos atuais, em uma única tela com prévia; não simula os passos de upload/publicação do antigo.
- axe-core: nenhuma violação nas regras automatizadas WCAG 2 A/AA e 2.1 A/AA das duas páginas. Console sem erros ou avisos nos fluxos conferidos. Essa verificação não equivale a uma avaliação manual completa de acessibilidade.

Inspeção e preparação de assets usaram ferramentas temporárias fora do repositório (Playwright, axe-core e Sharp). A skill `security-audit` foi instalada separadamente a pedido do usuário; sua instalação não significa execução de auditoria nesta entrega.
