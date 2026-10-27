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

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> upload(
            @RequestParam("file") MultipartFile file) {
        var resposta = uploadService.salvar(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/{nomeArquivo:.+}")
    public ResponseEntity<Resource> obterArquivo(@PathVariable String nomeArquivo) {
        Resource recurso = uploadService.carregarComoRecurso(nomeArquivo);
        MediaType mediaType = MediaTypeFactory.getMediaType(recurso)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .body(recurso);
    }
}
