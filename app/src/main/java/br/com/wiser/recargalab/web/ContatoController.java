package br.com.wiser.recargalab.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.security.CurrentUser;

/** API de contatos favoritos. O apelido é gravado sem validação e exibido sem encoding em /contatos (Lab 3). */
@RestController
@RequestMapping("/api/v1/contatos")
public class ContatoController {

    record ContatoRequest(String apelido, String telefone) {}

    private final JdbcTemplate jdbc;

    public ContatoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestAttribute(CurrentUser.ATTR) User me) {
        return jdbc.queryForList("SELECT id, apelido, telefone FROM contatos WHERE user_id = ? ORDER BY id", me.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> criar(@RequestBody ContatoRequest req, @RequestAttribute(CurrentUser.ATTR) User me) {
        Long id = jdbc.queryForObject(
                "INSERT INTO contatos (user_id, apelido, telefone) VALUES (?, ?, ?) RETURNING id",
                Long.class, me.getId(), req.apelido(), req.telefone());
        return Map.of("id", id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> atualizar(@PathVariable Long id, @RequestBody ContatoRequest req,
                                         @RequestAttribute(CurrentUser.ATTR) User me) {
        int n = jdbc.update("UPDATE contatos SET apelido = ?, telefone = ? WHERE id = ? AND user_id = ?",
                req.apelido(), req.telefone(), id, me.getId());
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "contato não encontrado");
        }
        return jdbc.queryForMap("SELECT id, apelido, telefone FROM contatos WHERE id = ?", id);
    }
}
