package br.com.wiser.recargalab.web;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.wiser.recargalab.infra.Md5;
import br.com.wiser.recargalab.infra.User;
import br.com.wiser.recargalab.infra.UserRepository;
import br.com.wiser.recargalab.security.JwtService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    record LoginRequest(String email, String senha) {}

    private final UserRepository users;
    private final JwtService jwt;

    public AuthController(UserRepository users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        Optional<User> found = users.findByEmail(req.email());
        // VULN A07:2025: sem limite de tentativas, sem MFA.
        if (found.isEmpty() || !found.get().getPasswordHash().equals(Md5.hex(req.senha()))) {
            // VULN A09:2025: falha de login não é registrada
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "credenciais inválidas"));
        }
        User u = found.get();
        String token = jwt.issue(u.getUsername(), u.getRole());

        // VULN A09:2025: senha e token em texto puro no log
        log.info("login email={} senha={} token={}", req.email(), req.senha(), token);

        // Cookie sem HttpOnly/Secure: usado pela página /contatos (Lab 3)
        ResponseCookie cookie = ResponseCookie.from("token", token).path("/").httpOnly(false).sameSite("Lax").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("token", token, "usuario", u.getUsername()));
    }
}
