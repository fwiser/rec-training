package br.com.wiser.recargalab.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.infra.UserRepository;
import br.com.wiser.recargalab.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public UserController(UserRepository users, JdbcTemplate jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    /** VULN API3:2023: devolve atributos internos (hash de senha, papel, KYC). */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(CurrentUser.ATTR) User me) {
        return perfil(me);
    }

    /** VULN A01:2025 (Lab 5 · mass assignment): a entidade inteira é ligada ao JSON de entrada. */
    @PutMapping("/me")
    public Map<String, Object> atualizar(@RequestBody User body, @RequestAttribute(CurrentUser.ATTR) User me) {
        if (body.getNome() != null) me.setNome(body.getNome());
        if (body.getEmail() != null) me.setEmail(body.getEmail());
        if (body.getTelefone() != null) me.setTelefone(body.getTelefone());
        if (body.getPasswordHash() != null) me.setPasswordHash(body.getPasswordHash());
        if (body.getRole() != null) me.setRole(body.getRole());
        if (body.getCashbackPercent() != null) me.setCashbackPercent(body.getCashbackPercent());
        if (body.getKycStatus() != null) me.setKycStatus(body.getKycStatus());
        users.update(me);
        return perfil(users.findById(me.getId()).orElseThrow());
    }

    private Map<String, Object> perfil(User u) {
        Map<String, Object> wallet = jdbc.queryForMap("SELECT id, saldo FROM wallets WHERE owner_id = ?", u.getId());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", u.getId());
        p.put("username", u.getUsername());
        p.put("nome", u.getNome());
        p.put("email", u.getEmail());
        p.put("telefone", u.getTelefone());
        p.put("role", u.getRole());
        p.put("cashbackPercent", u.getCashbackPercent());
        p.put("passwordHash", u.getPasswordHash());
        p.put("kycStatus", u.getKycStatus());
        p.put("walletId", wallet.get("id"));
        p.put("saldo", wallet.get("saldo"));
        return p;
    }
}
