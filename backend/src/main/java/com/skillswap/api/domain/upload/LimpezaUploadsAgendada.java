package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.portfolio.PublicacaoPortfolioRepository;
import com.skillswap.api.domain.usuario.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class LimpezaUploadsAgendada {
    private static final Pattern NOME_DE_UPLOAD = Pattern.compile("^[a-f0-9]{64}\\.(jpg|png|gif)$");
    private static final Pattern ROTA_DE_UPLOAD = Pattern.compile("/api/uploads/([a-f0-9]{64}\\.(?:jpg|png|gif))$");

    private final UsuarioRepository usuarioRepository;
    private final AnuncioRepository anuncioRepository;
    private final PublicacaoPortfolioRepository publicacaoRepository;

    @Value("${skillswap.upload.directory:uploads}")
    private String diretorioConfigurado;

    @Value("${skillswap.upload.orphan-retention-days:30}")
    private int diasRetencao;

    public LimpezaUploadsAgendada(
            UsuarioRepository usuarioRepository,
            AnuncioRepository anuncioRepository,
            PublicacaoPortfolioRepository publicacaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.anuncioRepository = anuncioRepository;
        this.publicacaoRepository = publicacaoRepository;
    }

    @Scheduled(cron = "${skillswap.upload.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    public void limparOrfaosAgendado() {
        try {
            int removidos = limparOrfaos(Instant.now());
            if (removidos > 0) {
                log.info("Limpeza de uploads removeu {} arquivo(s) sem referência e com retenção vencida", removidos);
            }
        } catch (RuntimeException e) {
            log.error("Falha na limpeza agendada de uploads órfãos", e);
        }
    }

    int limparOrfaos(Instant agora) {
        if (diasRetencao < 1) {
            throw new IllegalStateException("A retenção de uploads órfãos precisa ser de ao menos um dia");
        }

        Path pasta = Path.of(diretorioConfigurado).toAbsolutePath().normalize();
        if (!Files.isDirectory(pasta)) return 0;

        Set<String> referenciados = new HashSet<>();
        usuarioRepository.listarFotosDePerfil().forEach(url -> adicionarNome(url, referenciados));
        anuncioRepository.listarImagens().forEach(url -> adicionarNome(url, referenciados));
        publicacaoRepository.listarFotosReferenciadas().forEach(url -> adicionarNome(url, referenciados));

        Instant limite = agora.minus(diasRetencao, ChronoUnit.DAYS);
        int removidos = 0;
        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.toList()) {
                String nome = arquivo.getFileName().toString();
                if (!NOME_DE_UPLOAD.matcher(nome).matches() || referenciados.contains(nome)) continue;
                try {
                    if (Files.getLastModifiedTime(arquivo).toInstant().isBefore(limite)
                            && Files.deleteIfExists(arquivo)) {
                        removidos++;
                    }
                } catch (IOException e) {
                    log.warn("Não foi possível remover um upload órfão", e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível listar o diretório de uploads", e);
        }
        return removidos;
    }

    private void adicionarNome(String url, Set<String> referenciados) {
        if (url == null) return;
        Matcher matcher = ROTA_DE_UPLOAD.matcher(url);
        if (matcher.find()) referenciados.add(matcher.group(1));
    }
}
