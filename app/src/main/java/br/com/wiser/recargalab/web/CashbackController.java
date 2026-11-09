package br.com.wiser.recargalab.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/cashback")
public class CashbackController {

    record ResgateRequest(String codigo) {}

    private final JdbcTemplate jdbc;

    public CashbackController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/cupons")
    public List<Map<String, Object>> cupons(@RequestAttribute(CurrentUser.ATTR) User me) {
        return jdbc.queryForList("SELECT codigo, valor, usado FROM cupons WHERE user_id = ? ORDER BY codigo", me.getId());
    }

    /**
     * VULN A06:2025 (Lab 4 · race condition): verifica e depois marca como usado, sem transação nem lock.
     * Requests paralelos passam todos pela verificação antes do primeiro marcar o cupom.
     */
    @PostMapping("/resgatar")
    public Map<String, Object> resgatar(@RequestBody ResgateRequest req,
                                        @RequestAttribute(CurrentUser.ATTR) User me) throws InterruptedException {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT codigo, valor, usado FROM cupons WHERE codigo = ? AND user_id = ?", req.codigo(), me.getId());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "cupom não encontrado");
        }
        Map<String, Object> cupom = rows.get(0);
        if (Boolean.TRUE.equals(cupom.get("usado"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "cupom já utilizado");
        }

        Thread.sleep(300); // "consulta ao parceiro de cashback" — alarga a janela da corrida

        BigDecimal valor = (BigDecimal) cupom.get("valor");
        jdbc.update("UPDATE wallets SET saldo = saldo + ? WHERE owner_id = ?", valor, me.getId());
        jdbc.update("""
                INSERT INTO transacoes (wallet_id, tipo, valor, contraparte)
                SELECT id, 'CASHBACK', ?, ? FROM wallets WHERE owner_id = ?""",
                valor, "Cupom " + req.codigo(), me.getId());
        jdbc.update("UPDATE cupons SET usado = TRUE WHERE codigo = ?", req.codigo());
        return Map.of("status", "RESGATADO", "codigo", req.codigo(), "valor", valor);
    }
}
