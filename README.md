# RecargaLab

Laboratório do workshop **Desenvolvimento seguro de aplicações web** (Wiser Security × RecargaPay).

> ⚠️ **Aplicação propositalmente vulnerável.** Contém falhas reais de controle de acesso, injeção, autenticação, configuração e lógica de negócio. Execute **somente** na sua máquina, com as portas ligadas a `127.0.0.1` como estão no `docker-compose.yml`. Nunca publique estes containers em rede compartilhada, nuvem ou internet. Todos os dados são sintéticos.
>
> A RecargaLab é uma aplicação fictícia criada para o treinamento. Ela não reproduz código, arquitetura ou dados da RecargaPay.

## Requisitos

- Docker com Compose v2 (`docker compose version`).
- Cerca de 2 GB livres para imagens.
- Burp Suite Community ou Professional, com as extensões **JWT Editor** e **Autorize** (BApp Store).
- Acesso à internet **na primeira build**, para baixar imagens e dependências Maven. Faça a build antes do workshop: ela leva alguns minutos.

## Subir o laboratório

```bash
docker compose up -d --build
curl -s http://localhost:8081/actuator/health
# {"status":"UP"}
```

Abra `http://localhost:8081` (de preferência pelo navegador embutido do Burp).

| Conta | E-mail | Senha | Carteira |
|---|---|---|---|
| alice | `alice@lab.local` | `Alice#Lab1` | 1001 |
| bob | `bob@lab.local` | `Bob#Lab1` | 1002 |
| admin | `admin@lab.local` | não divulgada | 1000 |

## Serviços

| Serviço | Papel | Acesso |
|---|---|---|
| `recargalab` | API Spring Boot 3 (Java 21) + front-end simples | `127.0.0.1:8081` → porta 8080 do container |
| `postgres` | Banco com dados sintéticos | apenas rede interna |
| `operadora-mock` | Parceiro externo: recargas e comprovantes | `http://operadora-mock:9000` (interno) |
| `fake-metadata` | Simula o metadata de nuvem em `169.254.169.254` | visível só de dentro do container da API |

O `fake-metadata` compartilha o namespace de rede do container da API e cria o IP `169.254.169.254` no loopback desse namespace (por isso precisa de `NET_ADMIN`). Nada é alterado na rede da sua máquina.

## Resetar os dados

O banco é recriado a cada inicialização da API. Para voltar ao estado inicial:

```bash
docker compose restart recargalab
docker compose restart fake-metadata   # necessário após reiniciar a API (compartilham a rede)
```

## Configurar o Burp

1. Proxy › **Open browser** (navegador embutido já usa o proxy e confia na CA do Burp).
2. Target › Scope: adicione `http://localhost:8081`.
3. Faça login como alice e navegue por todas as abas para popular o Site map.

Usando outro navegador: o Chrome e o Firefox não enviam tráfego de `localhost` ao proxy por padrão. No Firefox, ative `network.proxy.allow_hijacking_localhost` em `about:config`.

## Mapa dos laboratórios

| Lab | Endpoint | Top 10:2025 | Top 10:2021 |
|---|---|---|---|
| 1 · IDOR no extrato | `GET /api/v1/wallets/{id}/statement` | A01 | A01 |
| 2 · SQL Injection | `GET /api/v1/recargas/busca?operadora=` | A05 | A03 |
| 3 · XSS armazenado | `PUT /api/v1/contatos/{id}` → página `/contatos` | A05 | A03 |
| 4 · Valor adulterado e race condition | `POST /api/v1/recargas`, `POST /api/v1/cashback/resgatar` | A06 | A04 |
| 5 · Mass assignment | `PUT /api/v1/users/me`, `GET /api/admin/usuarios` | A01 | A01 |
| 6 · JWT fraco | qualquer endpoint autenticado | A07, A04 | A07 |
| 7 · SSRF | `POST /api/v1/receipts/import` | A01 | A10 |
| 8 · Actuator e erros verbosos | `/actuator/*`, qualquer JSON malformado | A02, A10 | A05 |
| Bônus · SCA | `app/pom.xml` | A03 | A06 |
| Transversal | `POST /webhooks/operadora` (sem assinatura), logs | A08, A09 | A08, A09 |

A wordlist do Lab 6 está em `wordlists/jwt-secrets.txt`. O gabarito do instrutor fica em `docs/GABARITO.md`.

Cada vulnerabilidade está marcada no código com um comentário `VULN` e a categoria OWASP, para que, depois de explorar, os participantes localizem a linha e proponham a correção.

## Bônus de SCA

Execute na pasta `app/` (requer Maven e JDK 21 locais, ou rode dentro de um container `maven:3.9-eclipse-temurin-21`):

```bash
mvn -B org.owasp:dependency-check-maven:check
mvn -B org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom
```

A primeira execução do Dependency-Check baixa a base do NVD e pode demorar bastante; com uma chave de API do NVD (`-DnvdApiKey=...`) fica muito mais rápida. Rode antes do workshop se possível.

## Problemas comuns

- **Porta 8081 ocupada:** troque o lado esquerdo do mapeamento em `docker-compose.yml` (ex.: `127.0.0.1:8091:8080`).
- **Lab 7 não alcança `169.254.169.254`:** rode `docker compose restart fake-metadata` e confira `docker compose logs fake-metadata`.
- **API não sobe:** `docker compose logs recargalab`. Normalmente é o banco ainda iniciando; aguarde e repita `docker compose up -d`.
- **Tráfego não aparece no Burp:** use o navegador embutido ou veja a nota sobre `localhost` acima.

## Encerrar

```bash
docker compose down --rmi local
```

## Estrutura

```
recargalab/
├── docker-compose.yml
├── app/                    API Spring Boot (código vulnerável marcado com VULN)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/br/com/wiser/recargalab/{security,infra,web}
│       └── resources/{application.properties,schema.sql,data.sql,templates,static}
├── operadora-mock/         parceiro externo simulado (Python)
├── fake-metadata/          metadata de nuvem simulado (Python)
├── wordlists/              wordlist curta para o Lab 6
└── docs/GABARITO.md        respostas para o instrutor
```
