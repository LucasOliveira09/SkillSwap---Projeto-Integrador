# Modelo de dados — diagrama de classes

Diagrama de classes das entidades JPA do SkillSwap, persistidas em MySQL pelas migrações Flyway de `backend/src/main/resources/db/migration`.

![Diagrama de classes do SkillSwap](modelo-dados.png)

- Imagem: [modelo-dados.png](modelo-dados.png)
- Fonte editável (Mermaid): [modelo-dados.mmd](modelo-dados.mmd). O GitHub também desenha o diagrama diretamente a partir do bloco abaixo.

Legenda: seta (`-->`) indica associação `@ManyToOne`/`@OneToOne` com a multiplicidade de cada lado; losango preenchido (`*--`) indica composição, isto é, a parte não existe sem o todo (ex.: disponibilidades de um anúncio, eventos de um acordo).

## Como as entidades se relacionam

| Grupo | Entidades | Relações principais |
| --- | --- | --- |
| Conta e perfil profissional | `Usuario`, `ExperienciaProfissional`, `CertificacaoProfissional`, `Seguidor` | Um usuário tem várias experiências e certificações; `Seguidor` liga dois usuários (chave composta `SeguidorId`) |
| Oferta | `Anuncio`, `DisponibilidadeAnuncio`, `ComentarioAnuncio` | O anúncio pertence a um usuário, tem dias/períodos disponíveis e comentários com uma resposta por comentário |
| Negociação (funcionalidade do PI) | `Conversa`, `Mensagem`, `Proposta`, `Acordo`, `EventoAcordo` | A conversa é única por par de usuários; a proposta é feita sobre um anúncio dentro da conversa; o aceite gera um acordo, que registra eventos até a conclusão |
| Reputação | `Avaliacao`, `AvaliacaoHistorico`, `ContestacaoAvaliacao` | Uma avaliação por acordo concluído (cliente → profissional), com a versão anterior guardada se editada e no máximo uma contestação |
| Portfólio | `PublicacaoPortfolio`, `ComentarioPublicacao`, `CurtidaPublicacao` | Publicações do usuário, opcionalmente ligadas a um anúncio, com comentários e curtidas |
| Moderação e avisos | `Denuncia`, `Notificacao` | A denúncia aponta para o conteúdo por tipo + id (publicação, avaliação ou comentário); a notificação tem um destinatário |

## Enumerações usadas

| Enum | Valores |
| --- | --- |
| `ModalidadeAnuncio` | `VENDA`, `TROCA` |
| `StatusAnuncio` | `ATIVO`, `INATIVO` |
| `FormaPagamentoProposta` | `A_VISTA`, `PARCELADO`, `POR_HORA`, `POR_DIA`, `ASSINATURA`, `TROCA` |
| `StatusProposta` | `PENDENTE`, `ACEITA`, `RECUSADA`, `CANCELADA` |
| `StatusAcordo` | `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO` |
| `StatusDenuncia` | `ABERTA`, `OCULTADA`, `DESCARTADA`, `REEXIBIDA` |
| `StatusContestacaoAvaliacao` | `ABERTA`, `MANTIDA`, `OCULTADA` |

Entidades de infraestrutura que não fazem parte do domínio e ficaram fora do diagrama: `SessaoPersistente` (sessões de login), `UploadUsuario` (cota de arquivos enviados) e `ItemVitrine` (conteúdo da página inicial).

```mermaid
classDiagram
    direction TB

    class Usuario {
        Long id
        String nome
        String email
        String senha
        String tituloProfissional
        String cidade
        String especialidades
        boolean perfilPublico
        boolean disponivelTrabalhos
        LocalDateTime criadoEm
    }
    class Anuncio {
        Long id
        String titulo
        String descricao
        CategoriaAnuncio categoria
        String cidade
        ModalidadeAnuncio modalidade
        BigDecimal preco
        String habilidadeDesejada
        StatusAnuncio status
        LocalDateTime criadoEm
    }
    class DisponibilidadeAnuncio {
        Long id
        DiaSemana diaSemana
        PeriodoDia periodo
    }
    class Conversa {
        Long id
        LocalDateTime criadoEm
        LocalDateTime ultimaAtividadeEm
    }
    class Mensagem {
        Long id
        String conteudo
        LocalDateTime enviadoEm
    }
    class Proposta {
        Long id
        String servico
        String escopo
        FormaPagamentoProposta formaPagamento
        BigDecimal valor
        Integer parcelas
        String duracao
        LocalDate validadeEm
        StatusProposta status
        LocalDateTime criadoEm
    }
    class Acordo {
        Long id
        StatusAcordo status
        Boolean conclusaoConfirmadaAnunciante
        Boolean conclusaoConfirmadaProponente
        LocalDateTime criadoEm
        LocalDateTime concluidoEm
    }
    class EventoAcordo {
        Long id
        TipoEventoAcordo tipo
        LocalDateTime criadoEm
    }
    class Avaliacao {
        Long id
        int nota
        Integer notaAnuncio
        String depoimento
        String respostaProfissional
        boolean moderadaOculta
        boolean edicaoRealizada
        LocalDateTime criadoEm
    }
    class AvaliacaoHistorico {
        Long id
        int notaAnterior
        Integer notaAnuncioAnterior
        String depoimentoAnterior
        LocalDateTime registradaEm
    }
    class ContestacaoAvaliacao {
        Long id
        String justificativa
        StatusContestacaoAvaliacao status
        String decisao
        LocalDateTime criadaEm
    }
    class ComentarioAnuncio {
        Long id
        String conteudo
        boolean moderadaOculta
        LocalDateTime criadoEm
    }
    class PublicacaoPortfolio {
        Long id
        String legenda
        List~String~ fotos
        boolean moderadaOculta
        LocalDateTime criadoEm
    }
    class ComentarioPublicacao {
        Long id
        String conteudo
        boolean moderadaOculta
        LocalDateTime criadoEm
    }
    class CurtidaPublicacao {
        Long id
        LocalDateTime criadoEm
    }
    class ExperienciaProfissional {
        Long id
        String cargo
        String organizacao
        LocalDate dataInicio
        LocalDate dataFim
    }
    class CertificacaoProfissional {
        Long id
        String nome
        String emissor
        LocalDate dataEmissao
        LocalDate dataValidade
    }
    class Seguidor {
        SeguidorId id
        LocalDateTime criadoEm
    }
    class Denuncia {
        Long id
        TipoConteudoDenunciado tipoConteudo
        Long recursoId
        MotivoDenuncia motivo
        String descricao
        StatusDenuncia status
        LocalDateTime criadaEm
    }
    class Notificacao {
        Long id
        TipoNotificacao tipo
        String titulo
        String mensagem
        LocalDateTime lidaEm
        LocalDateTime criadaEm
    }

    Usuario "1" --> "0..*" Anuncio : publica
    Anuncio "1" *-- "0..*" DisponibilidadeAnuncio : disponibilidades
    Usuario "2" <-- "0..*" Conversa : participantes
    Anuncio "0..1" <-- "0..*" Conversa : contexto
    Conversa "1" *-- "0..*" Mensagem : mensagens
    Usuario "1" <-- "0..*" Mensagem : remetente
    Conversa "0..1" <-- "0..*" Proposta : negociada em
    Anuncio "1" <-- "0..*" Proposta : sobre
    Usuario "1" <-- "0..*" Proposta : proponente
    Proposta "0..1" <-- "0..*" Mensagem : cartão da proposta
    Proposta "1" <-- "0..1" Acordo : aceita gera
    Acordo "0..1" <-- "0..*" Mensagem : mensagens do acordo
    Acordo "1" *-- "0..*" EventoAcordo : linha do tempo
    Acordo "1" <-- "0..1" Avaliacao : avaliado em
    Usuario "1" <-- "0..*" Avaliacao : avaliador / avaliado
    Avaliacao "1" *-- "0..1" AvaliacaoHistorico : edição
    Avaliacao "1" <-- "0..1" ContestacaoAvaliacao : contestada por
    Anuncio "1" *-- "0..*" ComentarioAnuncio : comentários
    ComentarioAnuncio "0..1" <-- "0..*" ComentarioAnuncio : resposta a
    Usuario "1" --> "0..*" PublicacaoPortfolio : portfólio
    PublicacaoPortfolio "0..*" --> "0..1" Anuncio : vinculada a
    PublicacaoPortfolio "1" *-- "0..*" ComentarioPublicacao : comentários
    PublicacaoPortfolio "1" *-- "0..*" CurtidaPublicacao : curtidas
    Usuario "1" *-- "0..*" ExperienciaProfissional : experiências
    Usuario "1" *-- "0..*" CertificacaoProfissional : certificações
    Usuario "1" <-- "0..*" Seguidor : seguidor / seguido
    Usuario "1" <-- "0..*" Denuncia : autor
    Usuario "1" <-- "0..*" Notificacao : destinatário
```
