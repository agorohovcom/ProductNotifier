package com.agorohov.emailnotification.handler;

import com.agorohov.core.ProductCreatedEvent;
import com.agorohov.emailnotification.exception.NonRetryableException;
import com.agorohov.emailnotification.exception.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = "product-created-events-topic"/*, groupId = "product-created-events"*/)     // groupId это для группы Consumer
public class ProductCreatedEventHandler {

    private final RestTemplate restTemplate;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public ProductCreatedEventHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent) {
        log.info("Received event: {}", productCreatedEvent.getTitle());

//        String url = "http://localhost:8090/response/200";   // это порт сервиса mockservice
        String url = "http://localhost:8090/response/500";   // это порт сервиса mockservice
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                log.info("Received response: {}", response.getBody());
            }
        } catch (ResourceAccessException e) {
            log.error(e.getMessage());
            throw new RetryableException(e);
        } catch (HttpServerErrorException e) {
            log.error(e.getMessage());
            throw new NonRetryableException(e);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new NonRetryableException(e);
        }
    }
}
