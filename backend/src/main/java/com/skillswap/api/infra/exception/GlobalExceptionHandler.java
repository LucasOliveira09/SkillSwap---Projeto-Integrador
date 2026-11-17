package com.skillswap.api.infra.exception;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;

/**
 * Tratamento centralizado de erros da API. Todas as respostas usam o formato
 * {@code {codigo, mensagem, campos}}:
 * <ul>
 *   <li>400 DADOS_INVALIDOS: Bean Validation, JSON malformado, parâmetros inválidos ou regra de entrada;</li>
 *   <li>401 NAO_AUTENTICADO / 403 ACESSO_NEGADO: segurança;</li>
 *   <li>404 NAO_ENCONTRADO: recurso inexistente ({@link NaoEncontradoException});</li>
 *   <li>409 CONFLITO: violação de regra de negócio ou estado atual ({@link ConflitoException}).</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    public record Erro(String codigo, String mensagem, Map<String,String> campos) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> campos(MethodArgumentNotValidException ex) {
        var campos = new TreeMap<String,String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.merge(e.getField(),
                e.getDefaultMessage() == null ? "Valor inválido." : e.getDefaultMessage(),
                (a, b) -> a + " " + b));
        return ResponseEntity.badRequest().body(new Erro("DADOS_INVALIDOS","Revise os campos informados.",campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Erro> corpoInvalido(Exception ex) {
        return resposta(400, "DADOS_INVALIDOS", "Corpo da requisição ausente, malformado ou com campos desconhecidos.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Erro> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(400, "DADOS_INVALIDOS", "Valor inválido para o parâmetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler({HandlerMethodValidationException.class, jakarta.validation.ConstraintViolationException.class})
    ResponseEntity<Erro> parametroInvalido(Exception ex) {
        return resposta(400, "DADOS_INVALIDOS", "Parâmetros fora dos limites permitidos.");
    }

    /** Regras de entrada validadas nos services, cujas mensagens são escritas para o usuário. */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Erro> invalido(IllegalArgumentException ex) {
        String mensagem = mensagemOu(ex, "Revise os dados informados.");
        // Mensagens técnicas de bibliotecas (ex.: Enum.valueOf) não são expostas.
        if (mensagem.contains("com.") || mensagem.contains("No enum constant")) mensagem = "Revise os dados informados.";
        return resposta(400, "DADOS_INVALIDOS", mensagem);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Erro> autenticacao(Exception ex) { return resposta(401,"NAO_AUTENTICADO","Credenciais inválidas ou sessão expirada."); }

    @ExceptionHandler({NaoAutorizadoException.class,org.springframework.security.access.AccessDeniedException.class})
    ResponseEntity<Erro> proibido(Exception ex) {
        String mensagem = ex instanceof NaoAutorizadoException ? mensagemOu(ex, null) : null;
        return resposta(403,"ACESSO_NEGADO", mensagem == null ? "Você não tem permissão para esta operação." : mensagem);
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<Erro> conflito(ConflitoException ex) { return resposta(409,"CONFLITO",ex.getMessage()); }

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<Erro> ausente(Exception ex) {
        return resposta(404,"NAO_ENCONTRADO", mensagemOu(ex, "Recurso não encontrado."));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Erro> banco(Exception ex) { return resposta(503,"INDISPONIVEL","Não foi possível acessar os dados."); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> inesperado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse http) {
            boolean naoEncontrado = http.getStatusCode().value() == 404;
            return ResponseEntity.status(http.getStatusCode()).headers(http.getHeaders())
                .body(new Erro(naoEncontrado ? "NAO_ENCONTRADO" : "HTTP_INVALIDO",
                    naoEncontrado ? "Endereço da API não encontrado." : "Requisição não atendida.", Map.of()));
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha interna: {}",ex.getClass().getSimpleName());
        return resposta(500,"ERRO_INTERNO","Não foi possível concluir a operação.");
    }

    private static String mensagemOu(Exception ex, String padrao) {
        String mensagem = ex.getMessage();
        return mensagem == null || mensagem.isBlank() ? padrao : mensagem;
    }

    private ResponseEntity<Erro> resposta(int status,String codigo,String mensagem) {
        return ResponseEntity.status(status).body(new Erro(codigo,mensagem,Map.of()));
    }
}
