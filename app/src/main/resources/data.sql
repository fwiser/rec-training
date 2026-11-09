-- Dados 100% sintéticos. Senhas armazenadas em MD5 sem salt (A04:2025, proposital).
INSERT INTO users (id, username, nome, email, telefone, password_hash, role, cashback_percent, kyc_status) VALUES
 (1000, 'admin', 'Administrador RecargaLab', 'admin@lab.local', '+5511900000000', '82d7e4bcc6afdca18e0f76d75e9c30ad', 'ADMIN', 0, 'APROVADO'),
 (1001, 'alice', 'Alice Souza',  'alice@lab.local', '+5511988887777', '9d9e6f269966e6421ce2faa2e825dff9', 'USER', 2, 'APROVADO'),
 (1002, 'bob',   'Bob Lima',     'bob@lab.local',   '+5521977776666', 'de919b7621bc3bf9351c8029024ea07c', 'USER', 2, 'APROVADO'),
 (1003, 'carla', 'Carla Mendes', 'carla@lab.local', '+5531966665555', '9652803e9d311278a66f2d6ad5fe9d12', 'USER', 2, 'APROVADO'),
 (1004, 'diego', 'Diego Rocha',  'diego@lab.local', '+5541955554444', 'db9eb1fd913b386a10903d25b3e83e8e', 'USER', 3, 'PENDENTE'),
 (1005, 'elisa', 'Elisa Prado',  'elisa@lab.local', '+5551944443333', '8a9b0f5822d733809d3e7620a86f8c2f', 'USER', 2, 'APROVADO'),
 (1006, 'fabio', 'Fábio Nunes',  'fabio@lab.local', '+5581933332222', '41d69538a67f5e56427ad8a19e7e54a0', 'USER', 2, 'REPROVADO');

INSERT INTO wallets (id, owner_id, saldo) VALUES
 (1000, 1000, 10000.00), (1001, 1001, 245.90), (1002, 1002, 812.40), (1003, 1003, 57.15),
 (1004, 1004, 1320.00), (1005, 1005, 9.80), (1006, 1006, 430.25);

INSERT INTO transacoes (id, wallet_id, tipo, valor, contraparte, criado_em) VALUES
 (88201, 1001, 'PIX_RECEBIDO',  150.00, '***.987.654-**', now() - interval '6 days'),
 (88202, 1001, 'RECARGA',       -30.00, 'Vivo R$ 30 · +5511988887777', now() - interval '5 days'),
 (88203, 1001, 'CASHBACK',        0.60, 'Cashback recarga', now() - interval '5 days'),
 (88204, 1001, 'PAGAMENTO',     -89.90, 'Conta de luz', now() - interval '3 days'),
 (88205, 1001, 'PIX_ENVIADO',   -40.00, '***.321.000-**', now() - interval '1 day'),
 (88231, 1002, 'PIX_RECEBIDO',  350.00, '***.123.456-**', now() - interval '4 days'),
 (88232, 1002, 'RECARGA',       -20.00, 'Claro R$ 20 · +5521977776666', now() - interval '3 days'),
 (88233, 1002, 'PIX_RECEBIDO',  500.00, '***.555.222-**', now() - interval '2 days'),
 (88234, 1002, 'PAGAMENTO',    -120.00, 'Boleto internet', now() - interval '1 day');

INSERT INTO transacoes (wallet_id, tipo, valor, contraparte, criado_em)
SELECT 1000 + (g % 7),
       (ARRAY['PIX_RECEBIDO','PIX_ENVIADO','RECARGA','PAGAMENTO','CASHBACK'])[1 + g % 5],
       round((random() * 300)::numeric, 2) * (CASE WHEN g % 5 IN (1, 2, 3) THEN -1 ELSE 1 END),
       '***.' || lpad((g * 37 % 1000)::text, 3, '0') || '.' || lpad((g * 71 % 1000)::text, 3, '0') || '-**',
       now() - (g || ' hours')::interval
FROM generate_series(1, 60) AS g
WHERE 1000 + (g % 7) NOT IN (1001, 1002);

INSERT INTO produtos (id, operadora, nome, preco) VALUES
 ('VIVO-15', 'Vivo', 'Vivo R$ 15', 15.00), ('VIVO-30', 'Vivo', 'Vivo R$ 30', 30.00), ('VIVO-50', 'Vivo', 'Vivo R$ 50', 50.00),
 ('CLARO-20', 'Claro', 'Claro R$ 20', 20.00), ('CLARO-40', 'Claro', 'Claro R$ 40', 40.00),
 ('TIM-25', 'Tim', 'Tim R$ 25', 25.00), ('TIM-50', 'Tim', 'Tim R$ 50', 50.00);

INSERT INTO recargas (user_id, operadora, produto_id, telefone, valor_produto, debitado, status, protocolo, criado_em)
SELECT 1000 + (g % 7),
       (ARRAY['Vivo','Claro','Tim'])[1 + g % 3],
       (ARRAY['VIVO-30','CLARO-20','TIM-25'])[1 + g % 3],
       '+55119' || lpad((g * 7919 % 100000000)::text, 8, '0'),
       (ARRAY[30.00, 20.00, 25.00])[1 + g % 3],
       (ARRAY[30.00, 20.00, 25.00])[1 + g % 3],
       'CONCLUIDA',
       'OP-' || lpad((g * 104729 % 1000000)::text, 6, '0'),
       now() - (g || ' hours')::interval
FROM generate_series(1, 140) AS g;

INSERT INTO contatos (id, user_id, apelido, telefone) VALUES
 (77, 1001, 'Mãe',        '+5511999990000'),
 (78, 1001, 'Trabalho',   '+5511933334444'),
 (79, 1002, 'Irmão',      '+5521988881111'),
 (80, 1002, 'Pizzaria',   '+5521922223333'),
 (81, 1003, 'Vizinho',    '+5531977770000');

INSERT INTO cupons (codigo, user_id, valor) VALUES
 ('BEMVINDO-ALICE', 1001, 10.00),
 ('BEMVINDO-BOB',   1002, 10.00);

SELECT setval('users_id_seq', 2000);
SELECT setval('wallets_id_seq', 2000);
SELECT setval('transacoes_id_seq', 90000);
SELECT setval('recargas_id_seq', 5000);
SELECT setval('contatos_id_seq', 100);
