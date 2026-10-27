CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    bio VARCHAR(500),
    cidade VARCHAR(80),
    foto_url VARCHAR(255),
    especialidades VARCHAR(500),
    criado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE anuncios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(1500) NOT NULL,
    categoria VARCHAR(255) NOT NULL,
    cidade VARCHAR(80) NOT NULL,
    modalidade VARCHAR(255) NOT NULL,
    preco DECIMAL(10, 2),
    habilidade_desejada VARCHAR(120),
    status VARCHAR(255) NOT NULL,
    usuario_id BIGINT NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    codigo_legado VARCHAR(50),
    imagem VARCHAR(255),
    categoria_exibicao VARCHAR(255),
    unidade VARCHAR(255),
    autor_exibicao VARCHAR(255),
    avatar VARCHAR(255),
    estrelas INT,
    exemplo BOOLEAN NOT NULL DEFAULT FALSE,
    texto_busca VARCHAR(2200),
    CONSTRAINT pk_anuncios PRIMARY KEY (id),
    CONSTRAINT uk_anuncios_codigo_legado UNIQUE (codigo_legado),
    CONSTRAINT fk_anuncios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE propostas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    anuncio_id BIGINT NOT NULL,
    proponente_id BIGINT NOT NULL,
    mensagem_inicial VARCHAR(500) NOT NULL,
    status VARCHAR(255) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_propostas PRIMARY KEY (id),
    CONSTRAINT fk_propostas_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios (id),
    CONSTRAINT fk_propostas_proponente FOREIGN KEY (proponente_id) REFERENCES usuarios (id)
);

CREATE TABLE acordos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    proposta_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    concluido_em DATETIME(6),
    CONSTRAINT pk_acordos PRIMARY KEY (id),
    CONSTRAINT fk_acordos_proposta FOREIGN KEY (proposta_id) REFERENCES propostas (id)
);

CREATE TABLE mensagens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    acordo_id BIGINT NOT NULL,
    remetente_id BIGINT NOT NULL,
    conteudo VARCHAR(2000) NOT NULL,
    enviado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_mensagens PRIMARY KEY (id),
    CONSTRAINT fk_mensagens_acordo FOREIGN KEY (acordo_id) REFERENCES acordos (id),
    CONSTRAINT fk_mensagens_remetente FOREIGN KEY (remetente_id) REFERENCES usuarios (id)
);

CREATE TABLE avaliacoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    acordo_id BIGINT NOT NULL,
    avaliador_id BIGINT NOT NULL,
    avaliado_id BIGINT NOT NULL,
    nota INT NOT NULL,
    nota_anuncio INT,
    depoimento VARCHAR(1000),
    criado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_avaliacoes PRIMARY KEY (id),
    CONSTRAINT uk_avaliacoes_acordo_avaliador UNIQUE (acordo_id, avaliador_id),
    CONSTRAINT fk_avaliacoes_acordo FOREIGN KEY (acordo_id) REFERENCES acordos (id),
    CONSTRAINT fk_avaliacoes_avaliador FOREIGN KEY (avaliador_id) REFERENCES usuarios (id),
    CONSTRAINT fk_avaliacoes_avaliado FOREIGN KEY (avaliado_id) REFERENCES usuarios (id)
);

CREATE TABLE item_vitrine (
    id INT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(255) NOT NULL,
    grupo VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    imagem VARCHAR(255) NOT NULL,
    busca VARCHAR(255),
    categoria VARCHAR(255),
    ordem INT NOT NULL,
    CONSTRAINT pk_item_vitrine PRIMARY KEY (id),
    CONSTRAINT uk_item_vitrine_codigo UNIQUE (codigo)
);

CREATE TABLE publicacoes_portfolio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    legenda VARCHAR(500),
    usuario_id BIGINT NOT NULL,
    anuncio_id BIGINT,
    criado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_publicacoes_portfolio PRIMARY KEY (id),
    CONSTRAINT fk_publicacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_publicacoes_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios (id)
);

CREATE TABLE publicacao_portfolio_fotos (
    publicacao_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    foto_url VARCHAR(120) NOT NULL,
    CONSTRAINT pk_publicacao_portfolio_fotos PRIMARY KEY (publicacao_id, ordem),
    CONSTRAINT fk_publicacao_fotos_publicacao FOREIGN KEY (publicacao_id) REFERENCES publicacoes_portfolio (id)
);
