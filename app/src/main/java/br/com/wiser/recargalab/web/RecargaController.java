package br.com.wiser.recargalab.web;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.wiser.recargalab.infra.OperadoraClient;
import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class RecargaController {

    private static final Logger log = LoggerFactory.getLogger(RecargaController.class);

    /** VULN A06:2025 (Lab 4): valor, desconto e userId vêm do cliente. */
    record RecargaRequest(String produtoId, String telefone, BigDecimal valor, BigDecimal desconto, Long userId) {}

    private final JdbcTemplate jdbc;
    private final OperadoraClient operadora;

    public RecargaController(JdbcTemplate jdbc, OperadoraClient operadora) {
        this.jdbc = jdbc;
        this.operadora = operadora;
    }

    @GetMapping("/produtos")
    public List<Map<String, Object>> produtos() {
        return jdbc.queryForList("SELECT id, operadora, nome, preco FROM produtos ORDER BY operadora, preco");
    }

    @GetMapping("/recargas")
    public List<Map<String, Object>> minhas(@RequestAttribute(CurrentUser.ATTR) User me) {
        return jdbc.queryForList("""
                SELECT id, operadora, produto_id, telefone, debitado, status, protocolo, criado_em
                FROM recargas WHERE user_id = ? ORDER BY id DESC LIMIT 20""", me.getId());
    }

    /** VULN A05:2025 (Lab 2 · SQL Injection): entrada concatenada na consulta. */
    @GetMapping("/recargas/busca")
    public List<Map<String, Object>> busca(@RequestParam(defaultValue = "") String operadora,
                                           @RequestAttribute(CurrentUser.ATTR) User me) {
        log.info("busca: " + operadora); // VULN A09:2025: log injection
        String sql = "SELECT r.id, u.username AS usuario, r.operadora, r.produto_id, r.telefone, "
                + "r.debitado, r.status, r.criado_em "
                + "FROM recargas r JOIN users u ON u.id = r.user_id "
                + "WHERE r.user_id = " + me.getId() + " AND r.operadora = '" + operadora + "' "
                + "ORDER BY r.id";
        return jdbc.queryForList(sql);
    }

    @PostMapping("/recargas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> recarregar(@RequestBody RecargaRequest req) {
        Map<String, Object> produto = jdbc.queryForMap(
                "SELECT id, operadora, nome, preco FROM produtos WHERE id = ?", req.produtoId());

        BigDecimal desconto = req.desconto() == null ? BigDecimal.ZERO : req.desconto();
        BigDecimal debitado = req.valor().subtract(desconto);   // VULN: preço confiado ao cliente

        int ok = jdbc.update("UPDATE wallets SET saldo = saldo - ? WHERE owner_id = ? AND saldo >= ?",
                debitado, req.userId(), debitado);               // VULN: carteira escolhida pelo cliente
        if (ok == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "saldo insuficiente");
        }

        String protocolo = operadora.recarregar(req.telefone(), req.produtoId());
        Long id = jdbc.queryForObject("""
                INSERT INTO recargas (user_id, operadora, produto_id, telefone, valor_produto, debitado, status, protocolo)
                VALUES (?, ?, ?, ?, ?, ?, 'CONCLUIDA', ?) RETURNING id""",
                Long.class, req.userId(), produto.get("operadora"), req.produtoId(), req.telefone(),
                produto.get("preco"), debitado, protocolo);
        jdbc.update("""
                INSERT INTO transacoes (wallet_id, tipo, valor, contraparte)
                SELECT id, 'RECARGA', ?, ? FROM wallets WHERE owner_id = ?""",
                debitado.negate(), produto.get("nome") + " · " + req.telefone(), req.userId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("recargaId", id);
        res.put("status", "CONCLUIDA");
        res.put("produto", produto.get("nome"));
        res.put("debitado", debitado);
        res.put("protocolo", protocolo);
        return res;
    }
}
