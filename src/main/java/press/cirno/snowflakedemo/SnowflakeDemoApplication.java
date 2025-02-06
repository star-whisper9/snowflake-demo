package press.cirno.snowflakedemo;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import press.cirno.snowflakedemo.config.AppConfig;
import press.cirno.snowflakedemo.exception.WorkerManagementException;
import press.cirno.snowflakedemo.service.IMasterService;
import press.cirno.snowflakedemo.service.IWorkerService;

import java.util.concurrent.ScheduledFuture;

@Slf4j
@SpringBootApplication
public class SnowflakeDemoApplication implements ApplicationListener<ApplicationReadyEvent> {

    private final AppConfig appConfig;
    private final IMasterService iMasterService;
    private final IWorkerService iWorkerService;

    @Autowired
    public SnowflakeDemoApplication(AppConfig appConfig, IMasterService iMasterService, IWorkerService iWorkerService) {
        this.appConfig = appConfig;
        this.iMasterService = iMasterService;
        this.iWorkerService = iWorkerService;
    }

    private ScheduledFuture<?> masterFuture;
    private ScheduledFuture<?> workerFuture;

    public static void main(String[] args) {
        SpringApplication.run(SnowflakeDemoApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        int i = 0;
        if (appConfig.isMaster()) {
            iMasterService.initWorkerList();
            masterFuture = iMasterService.startCheckWorker();
            i++;
        }
        if (appConfig.isWorker()) {
            try {
                iWorkerService.init();
            } catch (WorkerManagementException e) {
                System.exit(500);
            }
            workerFuture = iWorkerService.startHeartbeat();
            i++;
        }
        if (i == 0) {
            log.error("未指定任何工作模式，程序退出");
            System.exit(400);
        }
    }

    @PreDestroy
    public void destroy() {
        if (appConfig.isMaster()) {
            masterFuture.cancel(true);
        }
        if (appConfig.isWorker()) {
            workerFuture.cancel(true);
        }
    }

}
