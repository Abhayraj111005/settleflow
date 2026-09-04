package com.settleflow.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SettlementCreatedConsumer {

    @KafkaListener(
            topics = "settlement-created",
            groupId = "settlement-service-group"
    )
    public void consume(SettlementCreatedEvent event) {

        System.out.println(
                ">>> Settlement Created Event Received"
        );

        System.out.println(
                "Settlement ID: " + event.getSettlementId()
        );

        System.out.println(
                "Merchant ID: " + event.getMerchantId()
        );

        System.out.println(
                "Amount: " + event.getAmount()
        );

        System.out.println(
                "Order Ref: " + event.getOrderRef()
        );
    }
}