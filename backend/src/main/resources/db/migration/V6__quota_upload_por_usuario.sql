CREATE TABLE uploads_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(68) NOT NULL,
    tamanho_bytes BIGINT NOT NULL DEFAULT 0,
    enviado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_uploads_usuario PRIMARY KEY (id),
    CONSTRAINT uk_uploads_usuario_arquivo UNIQUE (usuario_id, nome_arquivo),
    CONSTRAINT fk_uploads_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

INSERT INTO uploads_usuario (usuario_id, nome_arquivo, tamanho_bytes, enviado_em)
SELECT referencias.usuario_id, referencias.nome_arquivo, 0, CURRENT_TIMESTAMP(6)
FROM (
    SELECT u.id AS usuario_id, SUBSTRING(u.foto_url, 14) AS nome_arquivo
    FROM usuarios u
    WHERE u.foto_url REGEXP '^/api/uploads/[a-f0-9]{64}[.](jpg|png|gif)$'
    UNION
    SELECT a.usuario_id, SUBSTRING(a.imagem, 14)
    FROM anuncios a
    WHERE a.imagem REGEXP '^/api/uploads/[a-f0-9]{64}[.](jpg|png|gif)$'
    UNION
    SELECT p.usuario_id, SUBSTRING(f.foto_url, 14)
    FROM publicacoes_portfolio p
    JOIN publicacao_portfolio_fotos f ON f.publicacao_id = p.id
    WHERE f.foto_url REGEXP '^/api/uploads/[a-f0-9]{64}[.](jpg|png|gif)$'
) AS referencias;
