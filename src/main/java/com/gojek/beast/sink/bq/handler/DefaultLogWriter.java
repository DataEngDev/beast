package com.gojek.beast.sink.bq.handler;

import com.gojek.beast.models.Record;
import com.gojek.beast.models.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation that logs the error records to the log stream. The returned status however is failed as it implicitly
 * fails the overall execution of this beast instance. Essentially this implementation mimics the default behaviour of
 * erroneous insertions that are rejected by BQ and therefore mandates the failure status.
 */
@Slf4j
public class DefaultLogWriter implements ErrorWriter {

    @Override
    public Status writeErrorRecords(List<Record> records) {
        if (records != null && !records.isEmpty()) {
            records.forEach(record -> {
                log.debug("Error record: {} columns: {}", record.getId(), record.getColumns());
            });
        }
        return new WriteStatus(false, Optional.ofNullable(null));
    }
}
