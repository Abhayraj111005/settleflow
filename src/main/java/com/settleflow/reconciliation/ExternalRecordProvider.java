package com.settleflow.reconciliation;

import java.time.LocalDateTime;
import java.util.List;

public interface ExternalRecordProvider {

    List<ExternalRecord> getNewRecordsSince(LocalDateTime since);
}
