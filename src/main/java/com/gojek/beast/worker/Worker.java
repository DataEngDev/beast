package com.gojek.beast.worker;

import com.gojek.beast.models.Status;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

@Slf4j
public abstract class Worker extends Thread {

    private final String name;
    private boolean stopWorker;

    public Worker(String name) {
        super(name);
        this.name = name;
    }

    public abstract void stop(String reason);

    protected abstract Status job();

    @Override
    public void run() {
        log.info("Subscribing to class {}", getClass().getSimpleName());
        EventBus.getDefault().register(this);
        Status status;
        do {
            status = job();
        } while (!stopWorker && status.isSuccess());

        if (!status.isSuccess()) {
            EventBus.getDefault().post(new StopEvent(status.toString()));
        }
        EventBus.getDefault().unregister(this);
        log.info("Stopped worker {} with status {} stop: {}", getClass().getSimpleName(), status, stopWorker);
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onStopEvent(StopEvent event) {
        log.debug("stopping worker {} on receiving event {}", getClass().getSimpleName(), event);
        stop(event.getReason());
        stopWorker = true;
    }
}
