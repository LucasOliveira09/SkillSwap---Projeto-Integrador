# Política de visibilidade e privacidade dos perfis

O SkillSwap apresenta profissionais publicamente para que clientes possam comparar ofertas, portfólio e avaliações. A API pública de perfil não exige login. Estes são os campos e controles da entrega atual.

## Visíveis sem login

- ID do perfil e nome de exibição informado pela própria pessoa. O nome não comprova identidade.
- Título profissional, data de criação da conta, biografia, cidade, foto, capa, especialidades e links externos opcionais quando preenchidos.
- Resumo das avaliações recebidas e avaliações não ocultadas, incluindo resposta pública do profissional.
- Anúncios ativos e não demonstrativos, publicações de portfólio publicadas e imagens ligadas a esses conteúdos.

O perfil não publica e-mail, senha/hash de senha, JWT, mensagens, propostas, acordos, notificações nem histórico de moderação. Não há campo de telefone ou endereço preciso no perfil público.

## Controles disponíveis

- A pessoa pode apagar o título, a biografia, a cidade e as especialidades, remover a foto e a capa, pausar seus anúncios e excluir publicações próprias.
- Nome e avaliações públicas não podem ser ocultados por preferência nesta fase; uma avaliação sai da vitrine apenas pelas regras de moderação.
- Não existe botão para tornar o perfil inteiro privado nem fluxo de exclusão/anônimização de conta. Esses recursos continuam pendentes e não devem ser prometidos pela interface.

## URLs e remoção de imagens

URLs em `/api/uploads/{hash}.{extensão}` são acessíveis sem login enquanto a imagem estiver ligada a uma foto ou capa de perfil, a um anúncio ativo não demonstrativo ou a uma publicação de portfólio visível. Quando a última referência pública é removida ou ocultada, a URL passa a retornar não encontrado. Até a limpeza dos arquivos sem referência (30 dias por padrão), a conta que enviou a imagem ainda pode acessá-la autenticada para prévia ou reutilização. As respostas usam `Cache-Control: no-store` para evitar cache HTTP controlado pelo navegador/proxy. Isso não apaga cópias que visitantes já baixaram ou capturaram.

O SkillSwap não verifica documentos nem garante que uma pessoa, especialidade ou avaliação represente uma qualificação profissional. As notas descritas na [política de avaliações](politica-avaliacoes.md) vêm de acordos concluídos e seguem as regras de moderação.
