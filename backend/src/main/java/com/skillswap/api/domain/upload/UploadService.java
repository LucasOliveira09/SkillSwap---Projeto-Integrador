package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.upload.dto.UploadResponseDTO;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Locale;

@Service
public class UploadService {

    private static final long TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;
    private static final int DIMENSAO_MAXIMA = 5_000;
    private static final long PIXELS_MAXIMOS = 20_000_000;

    @Value("${skillswap.upload.directory:uploads}")
    private String diretorioConfigurado;

    private Path pastaUploads;

    @PostConstruct
    void inicializar() {
        pastaUploads = Path.of(diretorioConfigurado).toAbsolutePath().normalize();
        try {
            Files.createDirectories(pastaUploads);
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

        FormatoImagem formato = validarImagem(arquivo);
        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler arquivo enviado", e);
        }
        String nomeSalvo = calcularHash(bytes) + formato.extensao();
        Path destino = pastaUploads.resolve(nomeSalvo).normalize();
        if (!destino.startsWith(pastaUploads)) {
            throw new IllegalArgumentException("Nome de arquivo inválido");
        }

        try {
            try {
                Files.write(destino, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException arquivoJaSalvo) {
                // O mesmo conteúdo enviado novamente pelo PUT conserva a mesma representação.
            }
            return new UploadResponseDTO("/api/uploads/" + nomeSalvo, nomeSalvo);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar arquivo no servidor", e);
        }
    }

    private String calcularHash(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public Resource carregarComoRecurso(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.matches("^[a-f0-9]{64}\\.(jpg|png|gif)$")) {
            throw new NaoEncontradoException("Arquivo não encontrado");
        }
        try {
            Path caminho = pastaUploads.resolve(nomeArquivo).normalize();
            if (!caminho.startsWith(pastaUploads)) {
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

    public void validarFotoPublica(String url) {
        if (url == null || !url.startsWith("/api/uploads/")) {
            throw new IllegalArgumentException("A foto deve ser enviada para o SkillSwap");
        }
        carregarComoRecurso(url.substring("/api/uploads/".length()));
    }

    private FormatoImagem validarImagem(MultipartFile arquivo) {
        try (var arquivoEntrada = arquivo.getInputStream();
             ImageInputStream entrada = ImageIO.createImageInputStream(arquivoEntrada)) {
            if (entrada == null) {
                throw new IllegalArgumentException("O conteúdo não é uma imagem válida");
            }
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw new IllegalArgumentException("O conteúdo não é uma imagem válida");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                FormatoImagem formato = FormatoImagem.de(leitor.getFormatName(), arquivo.getContentType());
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura < 1 || altura < 1 || largura > DIMENSAO_MAXIMA || altura > DIMENSAO_MAXIMA
                        || (long) largura * altura > PIXELS_MAXIMOS) {
                    throw new IllegalArgumentException("A imagem deve ter no máximo 5000px por lado e 20 megapixels");
                }
                if (leitor.read(0) == null) {
                    throw new IllegalArgumentException("O conteúdo não é uma imagem válida");
                }
                return formato;
            } finally {
                leitor.dispose();
            }
        } catch (IOException | SecurityException e) {
            throw new IllegalArgumentException("O conteúdo não é uma imagem válida", e);
        }
    }

    private enum FormatoImagem {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), GIF("gif", "image/gif");

        private final String extensao;
        private final String tipoMime;

        FormatoImagem(String extensao, String tipoMime) {
            this.extensao = extensao;
            this.tipoMime = tipoMime;
        }

        String extensao() {
            return "." + extensao;
        }

        static FormatoImagem de(String formato, String tipoInformado) {
            String mime = tipoInformado == null ? "" : tipoInformado.toLowerCase(Locale.ROOT);
            FormatoImagem resultado = switch (formato.toUpperCase(Locale.ROOT)) {
                case "JPEG", "JPG" -> JPEG;
                case "PNG" -> PNG;
                case "GIF" -> GIF;
                default -> throw new IllegalArgumentException("Formato não suportado. Envie JPG, PNG ou GIF");
            };
            if (!resultado.tipoMime.equals(mime) && !(resultado == JPEG && mime.equals("image/jpg"))) {
                throw new IllegalArgumentException("O tipo informado não corresponde ao conteúdo da imagem");
            }
            return resultado;
        }
    }
}
