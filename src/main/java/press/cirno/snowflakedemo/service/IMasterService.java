package press.cirno.snowflakedemo.service;

import press.cirno.snowflakedemo.pojo.HeartbeatBody;
import press.cirno.snowflakedemo.pojo.RegistryBody;

import java.util.concurrent.ScheduledFuture;

public interface IMasterService {
    Integer registry(RegistryBody body);

    Integer heartbeat(HeartbeatBody body);

    String getWorker();

    void initWorkerList();

    ScheduledFuture<?> startCheckWorker();
}
