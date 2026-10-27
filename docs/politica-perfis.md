# Política de visibilidade e privacidade dos perfis

O SkillSwap apresenta profissionais publicamente para que clientes possam comparar ofertas, portfólio e avaliações. A API pública de perfil não exige login. Estes são os campos e controles da entrega atual.

## Visíveis sem login

- ID do perfil e nome de exibição informado pela própria pessoa. O nome não comprova identidade.
- Data de criação da conta, biografia, cidade, foto e especialidades quando preenchidas.
- Resumo das avaliações recebidas e avaliações não ocultadas, incluindo resposta pública do profissional.
- Anúncios ativos e não demonstrativos, publicações de portfólio publicadas e imagens ligadas a esses conteúdos.

O perfil não publica e-mail, senha/hash de senha, JWT, mensagens, propostas, acordos, notificações nem histórico de moderação. Não há campo de telefone ou endereço preciso no perfil público.

## Controles disponíveis

- A pessoa pode apagar a biografia, a cidade e as especialidades, remover a foto, pausar seus anúncios e excluir publicações próprias.
- Nome e avaliações públicas não podem ser ocultados por preferência nesta fase; uma avaliação sai da vitrine apenas pelas regras de moderação.
- Não existe botão para tornar o perfil inteiro privado nem fluxo de exclusão/anônimização de conta. Esses recursos continuam pendentes e não devem ser prometidos pela interface.

## Limite atual das URLs de imagem

URLs em `/api/uploads/{hash}.{extensão}` são públicas e endereçadas pelo hash do conteúdo. Remover uma referência tira a imagem do perfil, anúncio ou portfólio, mas não invalida imediatamente uma URL já conhecida: arquivos sem referência são apagados pela rotina de limpeza após a retenção configurada, de 30 dias por padrão. O cache público pode conservar a representação por até 24 horas. Portanto, não enviar imagens sensíveis ou que precisem de revogação imediata. Revogar a URL assim que a última referência pública for removida segue como item de endurecimento.

O SkillSwap não verifica documentos nem garante que uma pessoa, especialidade ou avaliação represente uma qualificação profissional. As notas descritas na [política de avaliações](politica-avaliacoes.md) vêm de acordos concluídos e seguem as regras de moderação.
