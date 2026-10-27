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

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record Erro(String codigo, String mensagem, Map<String,String> campos) {}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> campos(MethodArgumentNotValidException ex) {
        var campos = new TreeMap<String,String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.put(e.getField(), "Valor inválido."));
        return ResponseEntity.badRequest().body(new Erro("DADOS_INVALIDOS","Revise os campos informados.",campos));
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Erro> invalido(Exception ex) { return resposta(400,"DADOS_INVALIDOS","Revise os dados informados."); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Erro> autenticacao(Exception ex) { return resposta(401,"NAO_AUTENTICADO","Credenciais inválidas ou sessão expirada."); }
    @ExceptionHandler({NaoAutorizadoException.class,org.springframework.security.access.AccessDeniedException.class})
    ResponseEntity<Erro> proibido(Exception ex) { return resposta(403,"ACESSO_NEGADO","Você não tem permissão para esta operação."); }
    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<Erro> conflito(ConflitoException ex) { return resposta(409,"CONFLITO",ex.getMessage()); }
    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<Erro> ausente(Exception ex) { return resposta(404,"NAO_ENCONTRADO","Recurso não encontrado."); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Erro> banco(Exception ex) { return resposta(503,"INDISPONIVEL","Não foi possível acessar os dados."); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> inesperado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse http) {
            return ResponseEntity.status(http.getStatusCode()).headers(http.getHeaders())
                .body(new Erro(http.getStatusCode().value()==404 ? "NAO_ENCONTRADO" : "HTTP_INVALIDO",
                    "Requisição não atendida.", Map.of()));
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha interna: {}",ex.getClass().getSimpleName());
        return resposta(500,"ERRO_INTERNO","Não foi possível concluir a operação.");
    }
    private ResponseEntity<Erro> resposta(int status,String codigo,String mensagem) {
        return ResponseEntity.status(status).body(new Erro(codigo,mensagem,Map.of()));
    }
}
