package press.cirno.snowflakedemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import press.cirno.snowflakedemo.exception.TimeAccuracyException;
import press.cirno.snowflakedemo.pojo.HeartbeatBody;
import press.cirno.snowflakedemo.pojo.RegistryBody;
import press.cirno.snowflakedemo.pojo.WorkerPO;
import press.cirno.snowflakedemo.repositry.WorkerDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MasterService implements IMasterService {

    private final List<WorkerPO> workerList;
    private final WorkerDAO workerDAO;
    private final ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public MasterService(WorkerDAO workerDAO) {
        this.workerDAO = workerDAO;
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        this.workerList = new ArrayList<>(1023);
    }


    /**
     * 注册 Worker 并返回分配的 Worker ID
     *
     * @param body 注册信息
     * @return Worker ID || -1: Worker 已存在 || -2: Worker 列表已满
     */
    @Override
    public Integer registry(RegistryBody body) {
        if (isWorkerExist(body.getIp(), body.getMac())) {
            return -1;
        } else if (isWorkerListFull()) {
            return -2;
        }

        WorkerPO workerPO = new WorkerPO(
                body.getIp(),
                body.getMac(),
                body.getTimestamp()
        );
        // 保存出错会抛出异常
        WorkerPO savedWorkerPO = workerDAO.save(workerPO);
        workerList.add(savedWorkerPO);
        return savedWorkerPO.getId();
    }

    private boolean isWorkerExist(String ip, String mac) {
        WorkerPO workerPO = workerDAO.findTopByIpAndMacOrderByIdDesc(ip, mac);
        return workerPO != null;
    }

    private boolean isWorkerListFull() {
        WorkerPO workerPO = workerDAO.findTopByOrderByIdDesc();
        return workerPO != null && workerPO.getId() >= 1023;
    }

    /**
     * Worker 心跳，更新库内心跳时间
     *
     * @param body 心跳信息
     * @return Worker ID || -1: Worker 不存在
     */
    @Override
    public Integer heartbeat(HeartbeatBody body) {
        WorkerPO workerPO = workerDAO.findById(body.getWorkerId()).orElse(null);
        if (workerPO != null) {
            try {
                validateTimestamp(body.getTimestamp());
                workerPO.setLastHeartbeat(body.getTimestamp());
            } catch (TimeAccuracyException e) {
                log.warn("心跳时间不准确，使用本机时间: {}", body);
                workerPO.setLastHeartbeat(Long.parseLong(e.getMessage()));
            }
            // 保存出错会抛出异常
            return workerDAO.save(workerPO).getId();
        } else {
            log.warn("收到未注册节点的心跳: {}", body);
            return -1;
        }
    }

    private void validateTimestamp(Long timestamp) {
        Long now = System.currentTimeMillis();
        if (now - timestamp > 3000L) {
            throw new TimeAccuracyException(now.toString());
        }
    }

    /**
     * 获取随机一个 Worker 的地址
     *
     * @return Worker 地址
     */
    @Override
    public String getWorker() {
        WorkerPO workerPO = workerList.get(new Random().nextInt(workerList.size()));
        return workerPO.getExposedAddress();
    }

    /**
     * 检查 Worker 心跳状况<br >
     * 重量锁，避免心跳更替时的请求返回了不应返回的 Worker
     */
    private synchronized void checkWorker() {
        Iterable<WorkerPO> workerPOList = workerDAO.findAll();
        workerList.clear();
        for (WorkerPO workerPO : workerPOList) {
            if (System.currentTimeMillis() - workerPO.getLastHeartbeat() > 10000L) {
                workerDAO.delete(workerPO);
            }
            workerList.add(workerPO);
        }
    }

    /**
     * <strong>仅在 Master 模式调用，Worker 不需要</strong><br >
     * 初始化 Worker 列表
     */
    @Override
    public void initWorkerList() {
        Iterable<WorkerPO> workerPOList = workerDAO.findAll();
        workerList.clear();
        for (WorkerPO workerPO : workerPOList) {
            workerList.add(workerPO);
        }
    }

    /**
     * <strong>仅在 Master 模式调用，Worker 不需要</strong><br >
     * 启动 Worker 检查定时任务
     *
     * @return 任务句柄
     */
    @Override
    public ScheduledFuture<?> startCheckWorker() {
        return scheduledExecutorService.scheduleAtFixedRate(
                this::checkWorker,
                0,
                10,
                TimeUnit.SECONDS);
    }
}
