CREATE TABLE anuncio_disponibilidades (
    id BIGINT NOT NULL AUTO_INCREMENT,
    anuncio_id BIGINT NOT NULL,
    dia_semana VARCHAR(8) NOT NULL,
    periodo VARCHAR(5) NOT NULL,
    CONSTRAINT pk_anuncio_disponibilidades PRIMARY KEY (id),
    CONSTRAINT uk_anuncio_disponibilidade_dia_periodo UNIQUE (anuncio_id, dia_semana, periodo),
    CONSTRAINT fk_disponibilidades_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios (id)
);
