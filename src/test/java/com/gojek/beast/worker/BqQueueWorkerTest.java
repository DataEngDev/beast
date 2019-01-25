package com.gojek.beast.worker;

import com.gojek.beast.commiter.Committer;
import com.gojek.beast.config.QueueConfig;
import com.gojek.beast.models.FailureStatus;
import com.gojek.beast.models.Records;
import com.gojek.beast.models.SuccessStatus;
import com.gojek.beast.sink.Sink;
import com.gojek.beast.util.WorkerUtil;
import com.google.cloud.bigquery.BigQueryException;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BqQueueWorkerTest {

    @Mock
    private Sink successfulSink;
    @Mock
    private Records messages;
    private QueueConfig queueConfig;
    private int pollTimeout;
    @Mock
    private Committer committer;
    @Mock
    private Sink failureSink;
    @Mock
    private Map<TopicPartition, OffsetAndMetadata> offsetInfos;

    @Before
    public void setUp() {
        pollTimeout = 200;
        queueConfig = new QueueConfig(pollTimeout);
        when(successfulSink.push(any())).thenReturn(new SuccessStatus());
        when(messages.getPartitionsCommitOffset()).thenReturn(offsetInfos);
    }

    @Test
    public void shouldReadFromQueueAndPushToSink() throws InterruptedException {
        BlockingQueue<Records> queue = new LinkedBlockingQueue<>();
        BqQueueWorker worker = new BqQueueWorker(queue, successfulSink, queueConfig, committer);
        queue.put(messages);

        Thread thread = new Thread(worker);
        thread.start();

        WorkerUtil.closeWorker(worker, 100);
        thread.join();
        verify(successfulSink).push(messages);
    }

    @Test
    public void shouldReadFromQueueForeverAndPushToSink() throws InterruptedException {
        BlockingQueue<Records> queue = new LinkedBlockingQueue<>();
        BqQueueWorker worker = new BqQueueWorker(queue, successfulSink, queueConfig, committer);
        Records messages2 = mock(Records.class);
        when(committer.acknowledge(any())).thenReturn(true);
        queue.put(messages);
        queue.put(messages2);

        Thread workerThread = new Thread(worker);
        workerThread.start();

        await().atMost(10, TimeUnit.SECONDS).until(() -> queue.isEmpty());
        worker.onStopEvent(new StopEvent("job done"));
        workerThread.join();
        verify(successfulSink).push(messages);
        verify(successfulSink).push(messages2);
    }

    @Test
    public void shouldAckAfterSuccessfulPush() throws InterruptedException {
        BlockingQueue<Records> queue = new LinkedBlockingQueue<>();
        BqQueueWorker worker = new BqQueueWorker(queue, successfulSink, queueConfig, committer);
        queue.put(messages);

        Thread workerThread = new Thread(worker);
        workerThread.start();

        WorkerUtil.closeWorker(worker, 200);
        workerThread.join();
        verify(successfulSink).push(messages);
        verify(committer).acknowledge(offsetInfos);
    }

    @Test
    public void shouldNotAckAfterFailurePush() throws InterruptedException {
        when(failureSink.push(messages)).thenReturn(new FailureStatus(new Exception()));
        BlockingQueue<Records> queue = new LinkedBlockingQueue<>();
        BqQueueWorker worker = new BqQueueWorker(queue, failureSink, queueConfig, committer);

        queue.put(messages);

        Thread workerThread = new Thread(worker);
        workerThread.start();

        WorkerUtil.closeWorker(worker, 100);
        workerThread.join();
        verify(failureSink).push(messages);
        verify(committer, never()).acknowledge(any());
    }

    @Test
    public void shouldNotPushToSinkIfNoMessage() throws InterruptedException {
        BlockingQueue<Records> queue = new LinkedBlockingQueue<>();
        BqQueueWorker worker = new BqQueueWorker(queue, successfulSink, queueConfig, committer);
        Thread workerThread = new Thread(worker);

        workerThread.start();
        Thread.sleep(100);
        worker.onStopEvent(new StopEvent("close worker"));

        workerThread.join();
        verify(successfulSink, never()).push(any());
    }

    @Test
    public void shouldCloseCommitterWhenBiqQueryExceptionHappens() throws InterruptedException {
        BlockingQueue<Records> queue = new LinkedBlockingQueue<>();
        queue.put(messages);
        doThrow(new BigQueryException(10, "Some Error")).when(failureSink).push(messages);
        BqQueueWorker worker = new BqQueueWorker(queue, failureSink, queueConfig, committer);
        Thread workerThread = new Thread(worker);

        workerThread.start();

        workerThread.join();

        verify(committer).close(anyString());
    }
}
