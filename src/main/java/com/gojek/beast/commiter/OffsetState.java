package com.gojek.beast.commiter;

import com.gojek.beast.models.OffsetMap;
import lombok.Getter;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class OffsetState {
    @Getter
    private final long acknowledgeTimeoutMs;
    @Getter
    private final long offsetCommitTime;
    private boolean start;
    private Instant lastCommittedTime;
    private Set<OffsetMap> partitionOffsetAck;

    public OffsetState(Set<OffsetMap> partitionOffsetAck, long acknowledgeTimeoutMs, long offsetCommitTime) {
        this.partitionOffsetAck = partitionOffsetAck;
        this.acknowledgeTimeoutMs = acknowledgeTimeoutMs;
        this.offsetCommitTime = offsetCommitTime;
        lastCommittedTime = Instant.now();
    }

    public boolean shouldCloseConsumer(Map<TopicPartition, OffsetAndMetadata> currentOffset) {
        if (!start) {
            return false;
        }
        boolean ackTimedOut = (Instant.now().toEpochMilli() - lastCommittedTime.toEpochMilli()) > acknowledgeTimeoutMs;
        return ackTimedOut;
    }

    public int partitionOffsetAckSize() {
        return partitionOffsetAck.size();
    }

    public boolean removeFromOffsetAck(OffsetMap commitOffset) {
        return partitionOffsetAck.remove(commitOffset);
    }

    public void resetOffset() {
        lastCommittedTime = Instant.now();
    }

    public void startTimer() {
        start = true;
    }
}
