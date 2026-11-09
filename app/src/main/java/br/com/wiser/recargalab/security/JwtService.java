package br.com.wiser.recargalab.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Emissão e validação "caseira" de JWT.
 * VULN A07:2025 / A04:2025 (Lab 6):
 *  - segredo HS256 trivial, vindo de configuração em texto puro;
 *  - aceita tokens com "alg": "none" (sem assinatura);
 *  - validade de 30 dias, sem iss/aud.
 */
@Component
public class JwtService {

    private static final long VALIDADE_SEGUNDOS = 30L * 24 * 3600;

    private final byte[] secret;
    private final ObjectMapper mapper;

    public JwtService(@Value("${lab.jwt.secret}") String secret, ObjectMapper mapper) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.mapper = mapper;
    }

    public String issue(String subject, String role) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", subject);
        payload.put("role", role);
        payload.put("iat", now);
        payload.put("exp", now + VALIDADE_SEGUNDOS);
        String signingInput = b64(json(header)) + "." + b64(json(payload));
        return signingInput + "." + b64(hmac(signingInput));
    }

    public Map<String, Object> verify(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT malformado");
        }
        Map<String, Object> header = parse(parts[0]);
        Map<String, Object> claims = parse(parts[1]);
        String alg = String.valueOf(header.get("alg"));

        if ("none".equalsIgnoreCase(alg)) {
            // VULN: token sem assinatura é aceito
        } else if ("HS256".equals(alg)) {
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] given = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, given)) {
                throw new IllegalArgumentException("assinatura inválida");
            }
        } else {
            throw new IllegalArgumentException("algoritmo não suportado: " + alg);
        }

        Object exp = claims.get("exp");
        if (exp instanceof Number n && n.longValue() < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("token expirado");
        }
        return claims;
    }

    private Map<String, Object> parse(String part) {
        try {
            return mapper.readValue(Base64.getUrlDecoder().decode(part), new TypeReference<Map<String, Object>>() {});
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("segmento do JWT inválido", e);
        }
    }

    private byte[] json(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
