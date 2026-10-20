package br.edu.unifio.skillswap.controllers;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErroControllerAdvice {
    public record Erro(String codigo, String mensagem, Map<String, String> campos) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> campos(MethodArgumentNotValidException ex) {
        var campos = new TreeMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(erro -> campos.put(erro.getField(), "Valor inválido."));
        return ResponseEntity.badRequest().body(new Erro("DADOS_INVALIDOS", "Revise os campos informados.", campos));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Erro> invalido(Exception ex) {
        return ResponseEntity.badRequest().body(new Erro("DADOS_INVALIDOS", "Revise os dados informados.", Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Erro> naoEncontrado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new Erro("NAO_ENCONTRADO", "Anúncio não encontrado.", Map.of()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Erro> banco(DataAccessException ex) {
        return ResponseEntity.status(503).body(new Erro("INDISPONIVEL", "Não foi possível acessar os dados. Tente novamente.", Map.of()));
    }
}
