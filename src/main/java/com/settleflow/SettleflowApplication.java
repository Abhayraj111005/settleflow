package com.settleflow;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableScheduling
@EnableKafka
@SpringBootApplication
public class SettleflowApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(SettleflowApplication.class, args);
    }
}
