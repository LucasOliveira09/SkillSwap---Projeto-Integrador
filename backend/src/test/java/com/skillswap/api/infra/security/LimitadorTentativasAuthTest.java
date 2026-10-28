package com.skillswap.api.infra.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LimitadorTentativasAuthTest {

    @Test
    void limitaLoginPorClienteDentroDaJanelaESeparaClientes() {
        var limitador = novoLimitador(new RelogioControlavel());

        assertTrue(limitador.registrar(TipoTentativaAuth.LOGIN, "192.0.2.10").permitido());
        assertTrue(limitador.registrar(TipoTentativaAuth.LOGIN, "192.0.2.10").permitido());
        var bloqueio = limitador.registrar(TipoTentativaAuth.LOGIN, "192.0.2.10");

        assertFalse(bloqueio.permitido());
        assertEquals(60, bloqueio.retryAfterSegundos());
        assertTrue(limitador.registrar(TipoTentativaAuth.LOGIN, "192.0.2.11").permitido());
    }

    @Test
    void separaCadastroDeLoginEExpiraAJanela() {
        var relogio = new RelogioControlavel();
        var limitador = novoLimitador(relogio);

        assertTrue(limitador.registrar(TipoTentativaAuth.LOGIN, "192.0.2.20").permitido());
        assertTrue(limitador.registrar(TipoTentativaAuth.CADASTRO, "192.0.2.20").permitido());
        assertFalse(limitador.registrar(TipoTentativaAuth.CADASTRO, "192.0.2.20").permitido());

        relogio.avancar(Duration.ofMinutes(2));

        assertTrue(limitador.registrar(TipoTentativaAuth.CADASTRO, "192.0.2.20").permitido());
    }

    @Test
    void agrupaEnderecosIpv6PeloPrefixo64() {
        var limitador = novoLimitador(new RelogioControlavel());

        assertTrue(limitador.registrar(TipoTentativaAuth.LOGIN, "2001:db8:1:2::1").permitido());
        assertTrue(limitador.registrar(TipoTentativaAuth.LOGIN, "2001:db8:1:2::2").permitido());
        assertFalse(limitador.registrar(TipoTentativaAuth.LOGIN, "2001:db8:1:2::3").permitido());
    }

    @Test
    void aplicaOCapDeFormaAtomicaSobConcorrencia() {
        var limitador = novoLimitador(new RelogioControlavel());

        long permitidas = IntStream.range(0, 100)
                .parallel()
                .mapToObj(ignorado -> limitador.registrar(TipoTentativaAuth.LOGIN, "192.0.2.50"))
                .filter(LimitadorTentativasAuth.Resultado::permitido)
                .count();

        assertEquals(2, permitidas);
    }

    private static LimitadorTentativasAuth novoLimitador(Clock relogio) {
        return new LimitadorTentativasAuth(2, Duration.ofMinutes(1), 1, Duration.ofMinutes(2), relogio);
    }

    private static final class RelogioControlavel extends Clock {
        private final AtomicLong agora = new AtomicLong(Instant.parse("2026-10-28T12:00:00Z").toEpochMilli());

        void avancar(Duration duracao) {
            agora.addAndGet(duracao.toMillis());
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(agora.get());
        }
    }
}
