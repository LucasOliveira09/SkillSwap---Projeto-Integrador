package com.skillswap.api.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LimitadorTentativasAuth {

    private static final int MAXIMO_DE_CHAVES_ATIVAS = 100_000;
    private static final long INTERVALO_LIMPEZA = 256;

    private final Map<TipoTentativaAuth, Politica> politicas = new EnumMap<>(TipoTentativaAuth.class);
    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();
    private final AtomicLong requisicoes = new AtomicLong();
    private final Clock relogio;

    @Autowired
    public LimitadorTentativasAuth(
            @Value("${skillswap.security.rate-limit.login.max-attempts:10}") int maximoLogin,
            @Value("${skillswap.security.rate-limit.login.window:15m}") Duration janelaLogin,
            @Value("${skillswap.security.rate-limit.registration.max-attempts:5}") int maximoCadastro,
            @Value("${skillswap.security.rate-limit.registration.window:1h}") Duration janelaCadastro) {
        this(maximoLogin, janelaLogin, maximoCadastro, janelaCadastro, Clock.systemUTC());
    }

    LimitadorTentativasAuth(int maximoLogin, Duration janelaLogin,
                            int maximoCadastro, Duration janelaCadastro, Clock relogio) {
        validarPolitica(maximoLogin, janelaLogin, "login");
        validarPolitica(maximoCadastro, janelaCadastro, "cadastro");
        this.relogio = relogio;
        politicas.put(TipoTentativaAuth.LOGIN, new Politica(maximoLogin, janelaLogin));
        politicas.put(TipoTentativaAuth.CADASTRO, new Politica(maximoCadastro, janelaCadastro));
    }

    public Resultado registrar(TipoTentativaAuth tipo, String enderecoCliente) {
        var politica = politicas.get(tipo);
        long agora = relogio.millis();
        long totalRequisicoes = requisicoes.incrementAndGet();
        if ((totalRequisicoes % INTERVALO_LIMPEZA) == 0
                || (janelas.size() >= MAXIMO_DE_CHAVES_ATIVAS && totalRequisicoes % 64 == 0)) {
            janelas.entrySet().removeIf(entrada -> agora >= entrada.getValue().expiraEm());
        }

        String chave = tipo.name() + ":" + hash(enderecoNormalizado(enderecoCliente));
        Janela janela;
        if (janelas.containsKey(chave)) {
            janela = atualizarJanela(chave, politica, agora);
        } else {
            synchronized (janelas) {
                if (!janelas.containsKey(chave) && janelas.size() >= MAXIMO_DE_CHAVES_ATIVAS) {
                    return new Resultado(false, segundosAte(
                            somarComSaturacao(agora, politica.duracao().toMillis()), agora));
                }
                janela = atualizarJanela(chave, politica, agora);
            }
        }
        boolean permitido = janela.tentativas() <= politica.maximo();
        long retryAfter = permitido ? 0 : segundosAte(janela.expiraEm(), agora);
        return new Resultado(permitido, retryAfter);
    }

    private Janela atualizarJanela(String chave, Politica politica, long agora) {
        return janelas.compute(chave, (ignorada, atual) -> {
            if (atual == null || agora >= atual.expiraEm()) {
                return new Janela(somarComSaturacao(agora, politica.duracao().toMillis()), 1);
            }
            return new Janela(atual.expiraEm(), Math.min(atual.tentativas() + 1, (long) politica.maximo() + 1));
        });
    }

    private static void validarPolitica(int maximo, Duration duracao, String nome) {
        if (maximo < 1 || duracao == null || duracao.isZero() || duracao.isNegative()) {
            throw new IllegalArgumentException("A política de limite de " + nome + " deve ser positiva.");
        }
        try {
            if (duracao.toMillis() < 1) {
                throw new IllegalArgumentException("A janela de limite de " + nome + " deve ter ao menos 1 milissegundo.");
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("A janela de limite de " + nome + " é grande demais.", ex);
        }
    }

    private static String enderecoNormalizado(String endereco) {
        if (endereco == null || endereco.isBlank()) return "desconhecido";
        try {
            byte[] bytes = InetAddress.getByName(endereco.trim()).getAddress();
            if (bytes.length == 16) Arrays.fill(bytes, 8, bytes.length, (byte) 0);
            return HexFormat.of().formatHex(bytes);
        } catch (UnknownHostException ex) {
            return "desconhecido";
        }
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível.", ex);
        }
    }

    private static long segundosAte(long instante, long agora) {
        long milissegundos = Math.max(1, instante - agora);
        return Math.max(1, 1 + (milissegundos - 1) / 1000);
    }

    private static long somarComSaturacao(long instante, long duracao) {
        if (duracao > Long.MAX_VALUE - instante) return Long.MAX_VALUE;
        return instante + duracao;
    }

    public record Resultado(boolean permitido, long retryAfterSegundos) {}

    private record Politica(int maximo, Duration duracao) {}

    private record Janela(long expiraEm, long tentativas) {}
}
