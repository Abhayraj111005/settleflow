package com.settleflow.settleflow;

import com.settleflow.entity.ProcessedEvent;
import com.settleflow.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SettleflowApplicationTests {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void onlyOneProcessedEventShouldBeCreatedForConcurrentSameEventId()
            throws Exception {

        UUID eventId = UUID.randomUUID();

        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        Runnable insertTask = () -> {

            try {
                startLatch.await();

                ProcessedEvent event = new ProcessedEvent();
                event.setEventId(eventId);
                event.setProcessedAt(LocalDateTime.now());

                processedEventRepository.saveAndFlush(event);

            } catch (Exception ignored) {
                // One thread is expected to lose
                // the primary-key race.
            }
        };

        executorService.submit(insertTask);
        executorService.submit(insertTask);

        startLatch.countDown();

        executorService.shutdown();

        boolean finished = executorService.awaitTermination(
                5,
                TimeUnit.SECONDS
        );

        if (!finished) {
            throw new IllegalStateException(
                    "Test threads did not finish in time"
            );
        }

        long count =
                processedEventRepository.countByEventId(eventId);

        assertEquals(1, count);
    }
}