package com.agorohov.productmicroservice.service;

import com.agorohov.core.ProductCreatedEvent;
import com.agorohov.productmicroservice.service.dto.CreateProductDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class ProductServiceImpl implements ProductService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    public final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(CreateProductDto createProductDto) throws ExecutionException, InterruptedException {
        // todo save to DB
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(
                productId,
                createProductDto.getTitle(),
                createProductDto.getPrice(),
                createProductDto.getQuantity()
        );

        /** Первый способ реализовать асинхронную и синхронную обработку */
//        CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
//                kafkaTemplate
//                        .send(
//                                "product-created-events-topic",
//                                productId,
//                                productCreatedEvent
//                        );
//
//        future.whenComplete((result, exception) -> {
//            if (exception != null) {
//                log.error("Failed to send message: {}", exception.getMessage());
//            } else {
//                log.info("Message sent successfully: {}", result.getRecordMetadata());
//            }
//        });

        // чтобы перевести в синхронный режим
//        future.join();

        /** Второй способ реализовать синхронную обработку */
        SendResult<String, ProductCreatedEvent> result =
                kafkaTemplate
                        .send(
                                "product-created-events-topic",
                                productId,
                                productCreatedEvent
                        )
                        .get();

        log.info("Topic: {}", result.getRecordMetadata().topic());
        log.info("Partition: {}", result.getRecordMetadata().partition());
        log.info("Offset: {}", result.getRecordMetadata().offset());

        log.info("Returned productId: {}", productId);

        return productId;
    }
}
