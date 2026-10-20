# ADR-002: Java com Spring Boot e MySQL

Status: aceita por definição do usuário em 19/10/2026.

Atualização de 20/10/2026: versões e implementação inicial concretizadas na [ADR-003](003-backend-padroes-eventos.md). As consequências abaixo registram o momento desta decisão.

## Decisão

- Frontend: Angular 20, mantendo a base standalone e Angular Material existentes.
- Backend: Java com Spring Boot.
- Banco de dados: MySQL.

O usuário confirmou explicitamente essa stack. Os exemplos de tecnologias nos PDFs e a implementação Node/SQLite do projeto antigo são referências, não escolhas para o backend deste repositório.

## Consequências e próximo passo

O frontend atual continua como demonstração em memória. O backend Java e o schema MySQL ainda não foram criados. Na próxima etapa, definir versões compatíveis de Java, Spring Boot e MySQL, ferramenta de build, contrato da API, modelo de contas/perfis/anúncios e configuração local de conexão. Credenciais deverão ficar fora do Git.

Depois da base do backend, substituir o serviço demonstrativo por chamadas à API e confirmar cadastros somente após persistência bem-sucedida. A validação e a autorização também deverão ocorrer no servidor. A instalação da skill `security-audit` não substitui esses controles nem indica que uma auditoria já foi executada.
