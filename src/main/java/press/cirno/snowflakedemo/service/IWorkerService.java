package press.cirno.snowflakedemo.service;

import java.util.concurrent.ScheduledFuture;

public interface IWorkerService {
    long nextId();

    void init();

    ScheduledFuture<?> startHeartbeat();
}
