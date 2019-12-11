package com.gojek.beast.sink.bq;

import com.gojek.beast.models.OffsetInfo;
import com.gojek.beast.models.Record;
import com.gojek.beast.models.Status;
import com.gojek.beast.sink.bq.handler.DefaultLogWriter;
import com.gojek.beast.sink.bq.handler.ErrorWriter;
import org.junit.Test;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DefaultLogWriterTest {

    @Test
    public void testDefaultLogWriterReturnsStatus() {
        ErrorWriter defaultWriter = new DefaultLogWriter();
        Record record = new Record(new OffsetInfo("test", 1, 1L, System.currentTimeMillis()), new HashMap<>());
        Status result = defaultWriter.writeErrorRecords(Arrays.asList(record));
        assertFalse("Should be failed", result.isSuccess());
    }

    @Test
    public void testDefaultLogWriterDoesNotFailOnEmptyRecords() {
        ErrorWriter defaultWriter = new DefaultLogWriter();
        Status result = defaultWriter.writeErrorRecords(null);
        assertFalse(result.isSuccess());
        result = defaultWriter.writeErrorRecords(new ArrayList<>());
        assertFalse(result.isSuccess());
    }
}

