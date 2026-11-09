package br.com.wiser.recargalab.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    record TxDto(Long id, Long walletId, String owner, String tipo, BigDecimal valor,
                 String contraparte, Instant criadoEm) {}

    private final JdbcTemplate jdbc;

    public WalletController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/me")
    public Map<String, Object> minhaCarteira(@RequestAttribute(CurrentUser.ATTR) User me) {
        return jdbc.queryForMap("SELECT id, saldo FROM wallets WHERE owner_id = ?", me.getId());
    }

    /** VULN A01:2025 (Lab 1 · IDOR/BOLA): autenticado, mas nada liga a carteira ao usuário logado. */
    @GetMapping("/{walletId}/statement")
    public List<TxDto> statement(@PathVariable Long walletId) {
        return jdbc.query("""
                SELECT t.id, t.wallet_id, u.email, t.tipo, t.valor, t.contraparte, t.criado_em
                FROM transacoes t
                JOIN wallets w ON w.id = t.wallet_id
                JOIN users u ON u.id = w.owner_id
                WHERE t.wallet_id = ?
                ORDER BY t.criado_em DESC""",
                (rs, i) -> new TxDto(rs.getLong("id"), rs.getLong("wallet_id"), rs.getString("email"),
                        rs.getString("tipo"), rs.getBigDecimal("valor"), rs.getString("contraparte"),
                        rs.getTimestamp("criado_em").toInstant()),
                walletId);
    }
}
