package press.cirno.snowflakedemo.service;

import java.util.concurrent.ScheduledFuture;

public interface IWorkerService {
    long nextId();

    ScheduledFuture<?> startHeartbeat();
}
