package br.com.wiser.recargalab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RecargaLab — API PROPOSITALMENTE VULNERÁVEL usada no workshop de desenvolvimento seguro.
 * Cada vulnerabilidade está marcada no código com o comentário "VULN" e a categoria OWASP.
 * Nunca execute fora de um ambiente local e isolado.
 */
@SpringBootApplication
public class RecargaLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecargaLabApplication.class, args);
    }
}
