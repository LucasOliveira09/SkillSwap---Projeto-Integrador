# Política de visibilidade e privacidade dos perfis

O SkillSwap apresenta profissionais publicamente para que clientes possam comparar ofertas, portfólio e avaliações. A API pública de perfil não exige login. Estes são os campos e controles da entrega atual.

O perfil começa visível publicamente por compatibilidade com contas existentes. O titular pode pausar a exposição pela tela de edição e reativá-la depois, sem apagar conta, publicações, avaliações ou negociações.

## Visíveis sem login

- ID do perfil e nome de exibição informado pela própria pessoa. O nome não comprova identidade.
- Título profissional, data de criação da conta, biografia, cidade, foto, capa, especialidades e links externos opcionais quando preenchidos, somente enquanto o perfil estiver visível.
- Resumo das avaliações recebidas e avaliações não ocultadas, incluindo resposta pública do profissional, somente enquanto o perfil estiver visível.
- Anúncios ativos e não demonstrativos, publicações de portfólio publicadas e imagens ligadas a esses conteúdos, enquanto o perfil estiver visível.

O perfil não publica e-mail, senha/hash de senha, JWT, mensagens, propostas, acordos, notificações nem histórico de moderação. Não há campo de telefone ou endereço preciso no perfil público.

## Controles disponíveis

- A pessoa pode apagar o título, a biografia, a cidade e as especialidades, remover a foto e a capa, pausar seus anúncios e excluir publicações próprias.
- A pessoa pode pausar ou reativar a exibição pública do perfil; isso retira anúncios, portfólio, avaliações, fotos e capa das páginas públicas sem alterar negociações existentes.
- Denúncias só podem apontar para conteúdo atualmente público. Se o perfil do autor estiver pausado, a API retorna 404 e não revela trecho de publicações ou avaliações a quem tentar denunciá-las pelo ID.
- Nome e avaliações públicas não podem ser ocultados individualmente por preferência; uma avaliação sai da vitrine pelas regras de moderação ou quando o perfil é pausado.
- Não existe fluxo de exclusão/anônimização de conta. Esse recurso continua pendente e não deve ser prometido pela interface.

## URLs e remoção de imagens

URLs em `/api/uploads/{hash}.{extensão}` são acessíveis sem login enquanto a imagem estiver ligada a uma foto ou capa de perfil visível, a um anúncio ativo não demonstrativo de perfil visível ou a uma publicação de portfólio visível de perfil visível. Quando o perfil é pausado ou a última referência pública é removida, a URL passa a retornar não encontrado para visitantes. Até a limpeza dos arquivos sem referência (30 dias por padrão), a conta que enviou a imagem ainda pode acessá-la autenticada para prévia ou reutilização. As respostas usam `Cache-Control: no-store` para evitar cache HTTP controlado pelo navegador/proxy. Isso não apaga cópias que visitantes já baixaram ou capturaram.

O SkillSwap não verifica documentos nem garante que uma pessoa, especialidade ou avaliação represente uma qualificação profissional. As notas descritas na [política de avaliações](politica-avaliacoes.md) vêm de acordos concluídos e seguem as regras de moderação.
