package com.example.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ClientController {

    private final RestTemplate restTemplate;

    @Value("${greeting.service.url:http://greeting-service}")
    private String greetingServiceUrl;

    public ClientController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/call")
    public String callService() {
        return restTemplate.getForObject(greetingServiceUrl + "/hello", String.class);
    }
}
