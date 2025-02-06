package press.cirno.snowflakedemo.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import press.cirno.snowflakedemo.config.AppConfig;
import press.cirno.snowflakedemo.exception.TimeAccuracyException;
import press.cirno.snowflakedemo.exception.WorkerManagementException;
import press.cirno.snowflakedemo.pojo.HeartbeatBody;
import press.cirno.snowflakedemo.pojo.RegistryBody;
import press.cirno.snowflakedemo.pojo.StandardResponse;
import press.cirno.snowflakedemo.util.NetworkUtil;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@Service
@Data
public class WorkerService implements IWorkerService {

    // 上线基准时间
    private static final long startTime = 1723421520000L;
    private boolean registered = false;
    private long lastHeartbeat = 0L;
    private RegistryBody body;

    private long lastTimestamp = 0L;
    private long sequence = 0;
    private int workerId = -1;

    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService service;

    @Autowired
    public WorkerService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.restTemplate = new RestTemplate();
        this.service = new ScheduledThreadPoolExecutor(1);
        this.body = new RegistryBody();
    }

    /**
     * 生成 ID
     *
     * @return ID
     */
    @Override
    public synchronized long nextId() {
        if (!registered) {
            log.error("Worker 未注册");
            throw new WorkerManagementException("Worker 未注册");
        }

        long timestamp = getTime();
        if (timestamp < lastTimestamp) {
            log.error("时钟回拨，拒绝生成 ID");
            throw new TimeAccuracyException("时钟回拨，拒绝生成 ID");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 0xFFF;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - startTime) << 22) | ((long) workerId << 10) | sequence;
    }

    /**
     * 等待到下一毫秒
     *
     * @param lastTimestamp 上次时间戳
     * @return 等待到的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getTime();
        while (timestamp <= lastTimestamp) {
            timestamp = getTime();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     *
     * @return 时间戳
     */
    private long getTime() {
        return System.currentTimeMillis();
    }

    /**
     * 初始化 Worker
     */
    @Override
    public synchronized void init() {
        if (register() < 1) {
            log.error("Worker 注册失败");
            throw new WorkerManagementException("Worker 注册失败");
        }
        lastTimestamp = getTime();
    }

    /**
     * 注册 Worker，三次重试
     *
     * @return Worker ID || -1: 注册失败
     */
    private int register() {
        List<String> address = NetworkUtil.getLocalNetworkInfo();
        if (address == null) {
            log.error("无法获取本机 IP / Mac 地址，注册失败");
            return -1;
        }
        this.body.setIp(address.get(0));
        this.body.setMac(address.get(1));
        this.body.setTimestamp(getTime());
        this.body.setExposedAddress(appConfig.getExposedAddress());

        int retry = 0;
        while (retry < 3) {
            try {
                ResponseEntity<StandardResponse> response = restTemplate.postForEntity(
                        appConfig.getMasterAddress() + "/master/registry",
                        this.body,
                        StandardResponse.class
                );
                if (response.getStatusCode() == HttpStatus.OK) {
                    Assert.notNull(response.getBody(), "注册请求返回为空");
                    StandardResponse<Integer> responseBody = response.getBody();
                    if (responseBody.getCode() == 0 && responseBody.getData() > 0) {
                        registered = true;
                        workerId = responseBody.getData();
                        return responseBody.getData();
                    } else {
                        log.error("注册失败: {}", responseBody.getMessage());
                        retry++;
                    }
                } else {
                    log.error("注册请求失败: HTTP {}", response.getStatusCode());
                    retry++;
                }
            } catch (Exception e) {
                log.error("注册发生错误: {}", e.getMessage());
                retry++;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("重试注册失败: 线程中断");
                return -1;
            }
        }
        return -1;
    }

    /**
     * 开始心跳线程<br >
     * 心跳间隔 10 秒
     *
     * @return 心跳任务句柄
     */
    @Override
    public ScheduledFuture<?> startHeartbeat() {
        return service.scheduleAtFixedRate(this::heartbeat, 5, 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 心跳
     */
    private void heartbeat() {
        if (!registered) {
            log.error("心跳错误: Worker 未注册");
            System.exit(500);
        }

        List<String> address = NetworkUtil.getLocalNetworkInfo();
        if (address == null) {
            log.error("无法获取本机 IP / Mac 地址，心跳失败");
            System.exit(500);
        }

        if (!address.get(0).equals(body.getIp()) || !address.get(1).equals(body.getMac())) {
            log.error("心跳错误: IP / Mac 地址发生变化");
            System.exit(500);
        }

        HeartbeatBody heartbeatBody = new HeartbeatBody(
                address.get(0),
                address.get(1),
                getTime(),
                appConfig.getExposedAddress(),
                workerId
        );

        try {
            ResponseEntity<StandardResponse> response = restTemplate.postForEntity(
                    appConfig.getMasterAddress() + "/master/heartbeat",
                    heartbeatBody,
                    StandardResponse.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                Assert.notNull(response.getBody(), "心跳请求返回为空");
                StandardResponse<String> responseBody = response.getBody();
                if (responseBody.getCode() != 0) {
                    log.error("心跳失败: {}", responseBody.getMessage());
                    System.exit(500);
                }
            } else {
                log.error("心跳请求失败: HTTP {}", response.getStatusCode());
                System.exit(500);
            }
        } catch (Exception e) {
            log.error("心跳发生错误: {}", e.getMessage());
            System.exit(500);
        }
    }
}
