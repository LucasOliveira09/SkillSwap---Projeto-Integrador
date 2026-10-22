package com.skillswap.api.infra.exception;

public class NaoAutorizadoException extends RuntimeException {
    public NaoAutorizadoException(String message) {
        super(message);
    }
}
