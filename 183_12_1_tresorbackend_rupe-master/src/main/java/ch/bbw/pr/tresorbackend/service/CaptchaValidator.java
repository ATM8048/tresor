package ch.bbw.pr.tresorbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class CaptchaValidator {
    @Value("${google.recaptcha.secret}")
    private String secret;

    private final WebClient webClient = WebClient.create("https://www.google.com");

    public boolean verify(String token) {
        String url = "/recaptcha/api/siteverify";

        Map<String, String> request = Map.of(
                "secret", secret,
                "response", token
        );

        Map response = webClient.post()
                .uri("/recaptcha/api/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("secret", secret)
                        .with("response", token))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response != null && Boolean.TRUE.equals(response.get("success"));
    }
}
