package com.agorohov.emailnotification.handler;

import com.agorohov.core.ProductCreatedEvent;
import com.agorohov.emailnotification.exception.NonRetryableException;
import com.agorohov.emailnotification.exception.RetryableException;
import com.agorohov.emailnotification.persistence.entity.ProcessedEventEntity;
import com.agorohov.emailnotification.persistence.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@KafkaListener(topics = "product-created-events-topic"/*, groupId = "product-created-events"*/)
// groupId это для группы Consumer
public class ProductCreatedEventHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final RestTemplate restTemplate;
    private final ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaHandler
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,
                       @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {
        log.info("Received event: {}", productCreatedEvent.getTitle());

        // Получаем из БД ивент, чтобы проверить, обрабатывали ли мы его уже (для идемпотентности консьюмера)
        ProcessedEventEntity processedEventEntity = processedEventRepository.findByMessageId(messageId);

        if (processedEventEntity != null) {
            log.info("Duplicate messageId: {}", messageId);
            return;
        }

        // это порт сервиса mockservice
        String url = "http://localhost:8090/response/200";
//        String url = "http://localhost:8090/response/500";
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

        try {
            processedEventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException e) {
            log.error(e.getMessage());
            throw new NonRetryableException(e);
        }
    }
}
