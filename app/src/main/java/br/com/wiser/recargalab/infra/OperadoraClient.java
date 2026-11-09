package br.com.wiser.recargalab.infra;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Cliente do parceiro externo (operadora-mock). */
@Component
public class OperadoraClient {

    private static final Logger log = LoggerFactory.getLogger(OperadoraClient.class);

    private final RestTemplate rest;
    private final String baseUrl;
    private final String apiKey;

    public OperadoraClient(RestTemplate rest,
                           @Value("${lab.operadora.url}") String baseUrl,
                           @Value("${lab.operadora.api-key}") String apiKey) {
        this.rest = rest;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String recarregar(String telefone, String produtoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        Map<String, Object> body = new HashMap<>();
        body.put("telefone", telefone);
        body.put("produtoId", produtoId);
        try {
            Map<?, ?> resp = rest.postForObject(baseUrl + "/recargas", new HttpEntity<>(body, headers), Map.class);
            return resp != null && resp.get("protocolo") != null ? resp.get("protocolo").toString() : "SEM-PROTOCOLO";
        } catch (RestClientException e) {
            log.warn("operadora indisponível: {}", e.getMessage());
            return "PENDENTE";
        }
    }
}
