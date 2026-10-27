package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.portfolio.PublicacaoPortfolioRepository;
import com.skillswap.api.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimpezaUploadsAgendadaTest {
    @TempDir
    Path pasta;

    private UsuarioRepository usuarios;
    private AnuncioRepository anuncios;
    private PublicacaoPortfolioRepository publicacoes;
    private LimpezaUploadsAgendada limpeza;

    @BeforeEach
    void configurar() {
        usuarios = mock(UsuarioRepository.class);
        anuncios = mock(AnuncioRepository.class);
        publicacoes = mock(PublicacaoPortfolioRepository.class);
        when(usuarios.listarFotosDePerfil()).thenReturn(List.of("/api/uploads/" + "a".repeat(64) + ".jpg"));
        when(anuncios.listarImagens()).thenReturn(List.of("/api/uploads/" + "b".repeat(64) + ".png"));
        when(publicacoes.listarFotosReferenciadas()).thenReturn(List.of("/api/uploads/" + "c".repeat(64) + ".gif"));
        limpeza = new LimpezaUploadsAgendada(usuarios, anuncios, publicacoes);
        ReflectionTestUtils.setField(limpeza, "diretorioConfigurado", pasta.toString());
        ReflectionTestUtils.setField(limpeza, "diasRetencao", 30);
    }

    @Test
    void apagaSomenteUploadOrfaoComRetencaoVencida() throws Exception {
        Path referenciado = criarArquivo("a".repeat(64) + ".jpg", 60);
        Path orfaoAntigo = criarArquivo("d".repeat(64) + ".png", 60);
        Path orfaoRecente = criarArquivo("e".repeat(64) + ".gif", 2);
        Path arquivoEstranho = criarArquivo("foto-antiga.png", 60);

        int removidos = limpeza.limparOrfaos(Instant.now());

        assertEquals(1, removidos);
        assertTrue(Files.exists(referenciado));
        assertFalse(Files.exists(orfaoAntigo));
        assertTrue(Files.exists(orfaoRecente));
        assertTrue(Files.exists(arquivoEstranho));
    }

    private Path criarArquivo(String nome, int dias) throws Exception {
        Path arquivo = Files.writeString(pasta.resolve(nome), "imagem");
        Files.setLastModifiedTime(arquivo, FileTime.from(Instant.now().minus(dias, ChronoUnit.DAYS)));
        return arquivo;
    }
}
