package com.settleflow;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class SettleflowApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(SettleflowApplication.class, args);
    }
}