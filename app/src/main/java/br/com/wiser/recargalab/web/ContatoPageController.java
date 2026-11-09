package br.com.wiser.recargalab.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.security.CurrentUser;

/** Página server-side de contatos. VULN A05:2025 (Lab 3 · XSS armazenado): template usa th:utext. */
@Controller
public class ContatoPageController {

    private final JdbcTemplate jdbc;

    public ContatoPageController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/contatos")
    public String pagina(@RequestAttribute(CurrentUser.ATTR) User me, Model model) {
        model.addAttribute("usuario", me);
        model.addAttribute("contatos",
                jdbc.queryForList("SELECT id, apelido, telefone FROM contatos WHERE user_id = ? ORDER BY id", me.getId()));
        return "contatos";
    }
}
