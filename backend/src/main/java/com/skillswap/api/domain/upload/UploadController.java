package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.upload.dto.UploadResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import com.skillswap.api.domain.upload.dto.UsoArmazenamentoDTO;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> upload(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        var resposta = uploadService.salvar(file, (String) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/uso")
    public ResponseEntity<UsoArmazenamentoDTO> uso(Authentication authentication) {
        return ResponseEntity.ok(uploadService.uso((String) authentication.getPrincipal()));
    }

    @GetMapping("/{nomeArquivo:.+}")
    public ResponseEntity<Resource> obterArquivo(@PathVariable String nomeArquivo, Authentication authentication) {
        String emailUsuario = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                ? authentication.getName() : null;
        Resource recurso = uploadService.carregarComoRecurso(nomeArquivo, emailUsuario);
        MediaType mediaType = MediaTypeFactory.getMediaType(recurso)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .body(recurso);
    }
}
