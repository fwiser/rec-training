package br.com.wiser.recargalab.web;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/** VULN A01:2025 (Lab 7 · SSRF): o servidor busca qualquer URL informada pelo cliente. */
@RestController
public class ReceiptController {

    private final RestTemplate rest;

    public ReceiptController(RestTemplate rest) {
        this.rest = rest;
    }

    @PostMapping("/api/v1/receipts/import")
    public ResponseEntity<String> importar(@RequestParam String url) {
        ResponseEntity<String> upstream = rest.getForEntity(URI.create(url), String.class);
        MediaType tipo = upstream.getHeaders().getContentType();
        return ResponseEntity.ok()
                .contentType(tipo != null ? tipo : MediaType.TEXT_PLAIN)
                .body(upstream.getBody());
    }
}
