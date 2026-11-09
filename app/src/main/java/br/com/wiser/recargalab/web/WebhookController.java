package br.com.wiser.recargalab.web;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook de confirmação da operadora.
 * VULN A08:2025: não há assinatura, timestamp nem autenticação do emissor.
 * Qualquer pessoa pode marcar uma recarga como ESTORNADA e receber o valor de volta.
 */
@RestController
public class WebhookController {

    record Confirmacao(Long recargaId, String status) {}

    private final JdbcTemplate jdbc;

    public WebhookController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/webhooks/operadora")
    public Map<String, Object> confirmar(@RequestBody Confirmacao c) {
        Map<String, Object> recarga = jdbc.queryForMap(
                "SELECT id, user_id, debitado, status FROM recargas WHERE id = ?", c.recargaId());

        if ("ESTORNADA".equals(c.status()) && !"ESTORNADA".equals(recarga.get("status"))) {
            BigDecimal valor = (BigDecimal) recarga.get("debitado");
            Long userId = ((Number) recarga.get("user_id")).longValue();
            jdbc.update("UPDATE wallets SET saldo = saldo + ? WHERE owner_id = ?", valor, userId);
            jdbc.update("""
                    INSERT INTO transacoes (wallet_id, tipo, valor, contraparte)
                    SELECT id, 'ESTORNO', ?, ? FROM wallets WHERE owner_id = ?""",
                    valor, "Estorno da recarga " + c.recargaId(), userId);
        }
        jdbc.update("UPDATE recargas SET status = ? WHERE id = ?", c.status(), c.recargaId());
        return Map.of("recebido", true);
    }
}
