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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@Service
@Data
public class WorkerService implements IWorkerService {

    // 可配置块
    private String clockDriftStrategy;
    private long waitTime;
    private long startTime;

    // 应用服务相关
    private boolean registered = false;
    private long lastHeartbeat = 0L;
    private RegistryBody body;

    // 雪花算法相关
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
        while (timestamp < lastTimestamp) {
            if (lastTimestamp - timestamp <= 30L && clockDriftStrategy.equalsIgnoreCase("auto")) {
                timestamp = waitNextMillis(lastTimestamp);
            } else if (clockDriftStrategy.equalsIgnoreCase("wait") && lastTimestamp - timestamp <= waitTime) {
                timestamp = waitNextMillis(lastTimestamp);
            } else {
                log.error("时钟回拨，拒绝生成 ID");
                throw new TimeAccuracyException("时钟回拨，拒绝生成 ID");
            }
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
     * 初始化 Worker<br >
     * 在 Spring 容器启动后按需调用
     */
    @Override
    public synchronized void init() {
        if (register() < 1) {
            log.error("Worker 注册失败");
            throw new WorkerManagementException("Worker 注册失败");
        }
        lastTimestamp = getTime();
        clockDriftStrategy = appConfig.getClockDriftStrategy().equalsIgnoreCase("reject")
                || appConfig.getClockDriftStrategy().equalsIgnoreCase("wait")
                ? appConfig.getClockDriftStrategy() : "auto";
        if (appConfig.getWaitTime() > 1000L) {
            log.warn("时钟回拨等待时间过长，最大仅支持 1000 ms");
            waitTime = 1000L;
        } else {
            waitTime = appConfig.getWaitTime();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            startTime = sdf.parse(appConfig.getStartTime()).getTime();
        } catch (Exception e) {
            log.error("解析起始时间失败: {}", e.getMessage());
            try {
                startTime = sdf.parse("2024-08-12 08:12:00").getTime();
            } catch (Exception ignored) {
            }
        }
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
     * 心跳间隔 10 秒<br >
     * 在初始化后调用
     *
     * @return 心跳任务句柄
     */
    @Override
    public ScheduledFuture<?> startHeartbeat() {
        return service.scheduleAtFixedRate(this::heartbeat, 5, 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 心跳<br >
     * 每 10 秒发送一次心跳，任意一次失败则退出
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

    /**
     * 注销 Worker<br >
     * 应用程序销毁调用
     */
    @Override
    public void unregister() {
        if (!registered) {
            log.error("Worker 未注册，无法注销");
        }

        try {
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                    appConfig.getMasterAddress() + "/master/unregister",
                    workerId,
                    Boolean.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                Assert.notNull(response.getBody(), "注销请求返回为空");
                Boolean responseBody = response.getBody();
                if (responseBody) {
                    registered = false;
                } else {
                    log.error("注销失败");
                }
            } else {
                log.error("注销请求失败: HTTP {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("注销发生错误: {}", e.getMessage());
        }
    }
}
