package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.upload.dto.UploadResponseDTO;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Path PASTA_UPLOADS = Paths.get("uploads").toAbsolutePath().normalize();
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );
    private static final long TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024; // 5 MB

    public UploadService() {
        try {
            Files.createDirectories(PASTA_UPLOADS);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível inicializar diretório de uploads", e);
        }
    }

    public UploadResponseDTO salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem é obrigatório");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Arquivo excede o tamanho máximo de 5MB");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato de imagem não suportado. Formatos aceitos: JPG, PNG, WEBP, GIF");
        }

        String extensao = obterExtensao(arquivo.getOriginalFilename(), contentType);
        String nomeSalvo = UUID.randomUUID().toString().replace("-", "") + extensao;

        try {
            Path destino = PASTA_UPLOADS.resolve(nomeSalvo).normalize();
            if (!destino.startsWith(PASTA_UPLOADS)) {
                throw new IllegalArgumentException("Nome de arquivo inválido");
            }
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return new UploadResponseDTO("/api/uploads/" + nomeSalvo, nomeSalvo);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar arquivo no servidor", e);
        }
    }

    public Resource carregarComoRecurso(String nomeArquivo) {
        try {
            Path caminho = PASTA_UPLOADS.resolve(nomeArquivo).normalize();
            if (!caminho.startsWith(PASTA_UPLOADS)) {
                throw new NaoEncontradoException("Arquivo não encontrado");
            }
            Resource recurso = new UrlResource(caminho.toUri());
            if (recurso.exists() && recurso.isReadable()) {
                return recurso;
            }
            throw new NaoEncontradoException("Arquivo não encontrado");
        } catch (MalformedURLException e) {
            throw new NaoEncontradoException("Arquivo não encontrado");
        }
    }

    private String obterExtensao(String nomeOriginal, String contentType) {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            String ext = nomeOriginal.substring(nomeOriginal.lastIndexOf(".")).toLowerCase();
            if (ext.matches("^\\.(jpe?g|png|webp|gif)$")) {
                return ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
