DROP TABLE IF EXISTS transacoes, recargas, contatos, cupons, wallets, produtos, users CASCADE;

CREATE TABLE users (
  id               BIGSERIAL PRIMARY KEY,
  username         VARCHAR(40)  NOT NULL UNIQUE,
  nome             VARCHAR(120),
  email            VARCHAR(120) NOT NULL UNIQUE,
  telefone         VARCHAR(20),
  password_hash    VARCHAR(64)  NOT NULL,
  role             VARCHAR(20)  NOT NULL DEFAULT 'USER',
  cashback_percent INT          NOT NULL DEFAULT 2,
  kyc_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE'
);

CREATE TABLE wallets (
  id       BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
  saldo    NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE transacoes (
  id          BIGSERIAL PRIMARY KEY,
  wallet_id   BIGINT NOT NULL REFERENCES wallets(id),
  tipo        VARCHAR(30) NOT NULL,
  valor       NUMERIC(12,2) NOT NULL,
  contraparte VARCHAR(160),
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE produtos (
  id        VARCHAR(20) PRIMARY KEY,
  operadora VARCHAR(20) NOT NULL,
  nome      VARCHAR(60) NOT NULL,
  preco     NUMERIC(10,2) NOT NULL
);

CREATE TABLE recargas (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL REFERENCES users(id),
  operadora     VARCHAR(20),
  produto_id    VARCHAR(20),
  telefone      VARCHAR(20),
  valor_produto NUMERIC(10,2),
  debitado      NUMERIC(12,2),
  status        VARCHAR(30),
  protocolo     VARCHAR(40),
  criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE contatos (
  id       BIGSERIAL PRIMARY KEY,
  user_id  BIGINT NOT NULL REFERENCES users(id),
  apelido  VARCHAR(500),
  telefone VARCHAR(20)
);

CREATE TABLE cupons (
  codigo  VARCHAR(40) PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  valor   NUMERIC(10,2) NOT NULL,
  usado   BOOLEAN NOT NULL DEFAULT FALSE
);
