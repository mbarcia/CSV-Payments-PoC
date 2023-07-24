package com.example.poc.client;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Component(value="client")
public class PaymentProviderClient implements PaymentProvider {
    @Value("${api.url}")
    private String resourceUrl;
    @Value("${api.account}")
    private String account;
    @Value("${api.apiKey}")
    private String apiKey;

    @Autowired
    ObjectMapper mapper;

    @Override
    public AckPaymentSent sendPayment(SendPaymentRequest requestMap) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            headers.setBearerAuth(authorization().getToken());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String response = request(requestMap, headers, HttpMethod.POST, "/pay");

        try {
            return mapper.readValue(response, AckPaymentSent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            headers.setBearerAuth(authorization().getToken());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();

        String response = request(request, headers, HttpMethod.GET, "/status/" + ackPaymentSent.getConversationID());

        PaymentStatus result = mapper.readValue(response, PaymentStatus.class);
        result.setAckPaymentSent(ackPaymentSent);
        return result;
    }

    public AuthResponse authorization() throws JsonProcessingException {
        // TODO implement something like Spring Boot OAuth Bearer Tokens
        //  The current implementation is BAD because it polls token for each request

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Api-Key", apiKey);

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("account", account);

        String response = request(request, headers, HttpMethod.POST, "/auth");

        return mapper.readValue(response, AuthResponse.class);
    }

    public <T> String request(T requestMap, HttpHeaders headers, HttpMethod method, String uri) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<T> request = new HttpEntity<>(requestMap, headers);

        ResponseEntity<String> response = restTemplate
                .exchange(resourceUrl + uri, method, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }

        throw new RuntimeException();
    }
}
