package com.gojek.beast.launch;

import com.gojek.beast.config.AppConfig;
import com.gojek.beast.config.BackOffConfig;
import com.gojek.beast.factory.BeastFactory;
import com.gojek.beast.worker.StopEvent;
import com.gojek.beast.worker.Worker;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) {
        AppConfig appConfig = ConfigFactory.create(AppConfig.class, System.getenv());
        BackOffConfig backOffConfig = ConfigFactory.create(BackOffConfig.class, System.getenv());
        BeastFactory beastFactory = new BeastFactory(appConfig, backOffConfig);

        Worker consumerThread = beastFactory.createConsumerWorker();
        consumerThread.start();

        List<Worker> workers = beastFactory.createBqWorkers();
        workers.forEach(Thread::start);

        Thread committerThread = beastFactory.createOffsetCommitter();
        committerThread.start();

        addShutDownHooks();

        try {
            consumerThread.join();
            committerThread.join();
            for (Thread worker : workers) {
                worker.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("Exception::KafkaConsumer and committer join failed: {}", e.getMessage());
        } finally {
            beastFactory.close();
        }
    }


    private static void addShutDownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EventBus.getDefault().post(new StopEvent("Received Shutdown interrupt"));
        }));
    }
}
