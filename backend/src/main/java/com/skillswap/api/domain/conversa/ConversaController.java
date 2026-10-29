package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.mensagem.dto.MensagemRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController @RequiredArgsConstructor @Validated
@RequestMapping("/api")
public class ConversaController {
    private final ConversaService service;
    public record PrimeiroContato(@NotBlank @Size(max=500) String conteudo) {}
    public record Leitura(@NotNull @Min(1) Long ultimaMensagemId) {}
    public record Resumo(long naoLidas) {}

    @PostMapping("/anuncios/{id}/conversas")
    public ResponseEntity<ConversaDTO> abrir(@PathVariable Long id, @RequestBody @Valid PrimeiroContato dto, Authentication auth) {
        var abertura = service.abrir(id, dto.conteudo(), (String) auth.getPrincipal());
        var uri = URI.create("/api/conversas/" + abertura.conversa().id());
        return abertura.criada() ? ResponseEntity.created(uri).body(abertura.conversa()) : ResponseEntity.ok(abertura.conversa());
    }
    @GetMapping("/conversas")
    public ResponseEntity<PaginaDTO<ConversaDTO>> listar(@RequestParam(defaultValue="0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue="20") @Min(1) @Max(50) int tamanho, Authentication auth) {
        return ResponseEntity.ok(PaginaDTO.de(service.listar((String) auth.getPrincipal(), PageRequest.of(pagina, tamanho))));
    }
    @GetMapping("/conversas/resumo")
    public Resumo resumo(Authentication auth) { return new Resumo(service.naoLidas((String) auth.getPrincipal())); }
    @GetMapping("/conversas/{id}")
    public ConversaDTO buscar(@PathVariable Long id, Authentication auth) { return service.buscar(id, (String) auth.getPrincipal()); }
    @GetMapping("/conversas/{id}/mensagens")
    public PaginaDTO<MensagemConversaDTO> mensagens(@PathVariable Long id,
            @RequestParam(defaultValue="0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int tamanho, Authentication auth) {
        return PaginaDTO.de(service.listarMensagens(id, (String) auth.getPrincipal(),
                PageRequest.of(pagina, tamanho, Sort.by("enviadoEm", "id"))));
    }
    @PostMapping("/conversas/{id}/mensagens")
    public ResponseEntity<MensagemConversaDTO> enviar(@PathVariable Long id, @RequestBody @Valid MensagemRequestDTO dto, Authentication auth) {
        var mensagem = service.enviar(id, dto.conteudo(), (String) auth.getPrincipal());
        return ResponseEntity.created(URI.create("/api/conversas/"+id+"/mensagens/"+mensagem.id())).body(mensagem);
    }
    @GetMapping("/conversas/{id}/mensagens/{mensagemId}")
    public MensagemConversaDTO mensagem(@PathVariable Long id, @PathVariable Long mensagemId, Authentication auth) {
        return service.buscarMensagem(id, mensagemId, (String) auth.getPrincipal());
    }
    @PatchMapping("/conversas/{id}/leitura")
    public ResponseEntity<Void> ler(@PathVariable Long id, @RequestBody @Valid Leitura dto, Authentication auth) {
        service.ler(id, dto.ultimaMensagemId(), (String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
