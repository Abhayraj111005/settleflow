package com.settleflow.reconciliation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimulatedExternalRecordProvider implements ExternalRecordProvider {

    private final List<ExternalRecord> records = new ArrayList<>();

    public SimulatedExternalRecordProvider() {
        records.add(new ExternalRecord(
                "EXT-001",
                new BigDecimal("100.00"),
                LocalDateTime.now().minusMinutes(10)
        ));

        records.add(new ExternalRecord(
                "EXT-002",
                new BigDecimal("200.00"),
                LocalDateTime.now().minusMinutes(5)
        ));
    }

    @Override
    public List<ExternalRecord> getNewRecordsSince(LocalDateTime since) {
        return records.stream()
                .filter(record -> record.getTimestamp().isAfter(since))
                .toList();
    }
}
