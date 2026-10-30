package com.skillswap.api.domain.upload.dto;

public record UploadResponseDTO(
        String url,
        String nomeArquivo,
        long tamanhoBytes,
        long usoBytes,
        long limiteBytes
) {}
