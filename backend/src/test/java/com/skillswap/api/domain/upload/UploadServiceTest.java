package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioRepository;
import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.portfolio.PublicacaoPortfolioRepository;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadServiceTest {
    private static final long LIMITE_BYTES = 5 * 1024 * 1024;

    @TempDir Path pasta;

    private UsuarioRepository usuarios;
    private UploadUsuarioRepository uploads;
    private AnuncioRepository anuncios;
    private PublicacaoPortfolioRepository portfolio;
    private UploadService service;

    @BeforeEach
    void configurar() {
        usuarios = mock(UsuarioRepository.class);
        uploads = mock(UploadUsuarioRepository.class);
        anuncios = mock(AnuncioRepository.class);
        portfolio = mock(PublicacaoPortfolioRepository.class);
        var usuario = Usuario.builder().id(42L).email("ana@skillswap.invalid").build();
        when(usuarios.findByEmailForUpdate("ana@skillswap.invalid")).thenReturn(Optional.of(usuario));
        when(uploads.findByUsuarioIdAndNomeArquivo(any(), anyString())).thenReturn(Optional.empty());
        service = new UploadService(usuarios, uploads, anuncios, portfolio);
        ReflectionTestUtils.setField(service, "diretorioConfigurado", pasta.toString());
        ReflectionTestUtils.setField(service, "limitePorUsuarioBytes", LIMITE_BYTES);
        service.inicializar();
    }

    @Test
    void bloqueiaUmNovoArquivoQuandoAQuotaDaContaEsgotaria() throws Exception {
        byte[] primeiraImagem = imagemComRuido(1);
        byte[] segundaImagem = imagemComRuido(2);
        assertTrue(primeiraImagem.length < LIMITE_BYTES);
        assertTrue(segundaImagem.length < LIMITE_BYTES);
        assertTrue(primeiraImagem.length + segundaImagem.length > LIMITE_BYTES);
        when(uploads.somarTamanhoPorUsuario(42L)).thenReturn(0L, (long) primeiraImagem.length);

        service.salvar(arquivo(primeiraImagem), "ana@skillswap.invalid");
        var erro = assertThrows(IllegalArgumentException.class,
                () -> service.salvar(arquivo(segundaImagem), "ana@skillswap.invalid"));

        assertTrue(erro.getMessage().contains("Limite de armazenamento"));
        verify(uploads).save(any(UploadUsuario.class));
    }

    @Test
    void uploadOrfaoSoPodeSerLidoPeloProprietarioEImagemPublicaTemAcessoAnonimo() throws Exception {
        var resposta = service.salvar(arquivo(imagemComRuido(3)), "ana@skillswap.invalid");
        String nomeArquivo = resposta.nomeArquivo();
        String url = resposta.url();
        when(uploads.pertenceAoUsuario("ana@skillswap.invalid", nomeArquivo)).thenReturn(true);

        assertThrows(NaoEncontradoException.class, () -> service.carregarComoRecurso(nomeArquivo, null));
        assertTrue(service.carregarComoRecurso(nomeArquivo, "ana@skillswap.invalid").exists());

        when(usuarios.existsByFotoUrl(url)).thenReturn(true);
        assertTrue(service.carregarComoRecurso(nomeArquivo, null).exists());

        when(usuarios.existsByFotoUrl(url)).thenReturn(false);
        assertThrows(NaoEncontradoException.class, () -> service.carregarComoRecurso(nomeArquivo, null));
    }

    private MockMultipartFile arquivo(byte[] bytes) {
        return new MockMultipartFile("file", "portfolio.png", "image/png", bytes);
    }

    private byte[] imagemComRuido(long semente) throws Exception {
        var imagem = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        var aleatorio = new Random(semente);
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                imagem.setRGB(x, y, aleatorio.nextInt(1 << 24));
            }
        }
        var saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }
}
