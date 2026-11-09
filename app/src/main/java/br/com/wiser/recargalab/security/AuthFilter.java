package br.com.wiser.recargalab.security;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.wiser.recargalab.infra.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autenticação por Bearer token ou cookie "token".
 * Só verifica SE há usuário autenticado; autorização fica (ou não) a cargo de cada endpoint.
 * /actuator e /webhooks ficam abertos de propósito (Lab 8 e A08).
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final JwtService jwt;
    private final UserRepository users;

    public AuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(req);
        if (token != null) {
            try {
                Map<String, Object> claims = jwt.verify(token);
                Object sub = claims.get("sub");
                if (sub != null) {
                    users.findByUsername(sub.toString()).ifPresent(u -> req.setAttribute(CurrentUser.ATTR, u));
                }
            } catch (RuntimeException e) {
                log.debug("token rejeitado: {}", e.getMessage());
            }
        }

        String path = req.getRequestURI();
        if (req.getAttribute(CurrentUser.ATTR) == null) {
            if (path.startsWith("/api/") && !path.startsWith("/api/v1/auth/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"erro\":\"não autenticado\"}");
                return;
            }
            if (path.equals("/contatos")) {
                res.sendRedirect("/");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private static String extractToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("token".equals(c.getName()) && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
