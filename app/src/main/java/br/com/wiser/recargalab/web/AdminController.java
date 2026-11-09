package br.com.wiser.recargalab.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.infra.UserRepository;
import br.com.wiser.recargalab.security.CurrentUser;

/** Área administrativa. O papel é lido do banco — por isso o Lab 5 (mass assignment) abre esta porta. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/usuarios")
    public List<User> usuarios(@RequestAttribute(CurrentUser.ATTR) User me) {
        if (!"ADMIN".equals(me.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "acesso restrito a administradores");
        }
        return users.findAll();
    }
}
