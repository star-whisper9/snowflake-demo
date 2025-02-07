package press.cirno.snowflakedemo.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import press.cirno.snowflakedemo.exception.WorkerManagementException;
import press.cirno.snowflakedemo.pojo.HeartbeatBody;
import press.cirno.snowflakedemo.pojo.RegistryBody;
import press.cirno.snowflakedemo.pojo.StandardResponse;
import press.cirno.snowflakedemo.service.IMasterService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/master")
public class MasterController {
    private final IMasterService masterService;

    @Autowired
    public MasterController(IMasterService masterService) {
        this.masterService = masterService;
    }

    /**
     * Worker 注册接口
     *
     * @param body 注册信息
     * @return 成功信息 + Worker ID || 失败信息 + null 响应体
     */
    @PostMapping("/registry")
    public StandardResponse<Integer> registry(@RequestBody @Validated RegistryBody body) {
        Integer workerId = masterService.registry(body);
        if (workerId == -1) {
            return StandardResponse.fail("同 IP / Mac 节点已注册过");
        } else if (workerId == -2) {
            return StandardResponse.fail("节点已满");
        } else {
            return StandardResponse.success(workerId);
        }
    }

    /**
     * Worker 心跳接口
     *
     * @param body 心跳信息
     * @return 成功信息 + null 响应体 || 失败信息 + null 响应体
     */
    @PostMapping("/heartbeat")
    public StandardResponse<String> heartbeat(@RequestBody @Validated HeartbeatBody body) {
        Integer workerId = masterService.heartbeat(body);
        if (workerId == -1) {
            return StandardResponse.fail("节点未注册");
        } else {
            return StandardResponse.success();
        }
    }

    /**
     * 对外的 ID 生成接口，基于简单随机的负载均衡<br >
     * 返回一个 301 重定向到随机 Worker 的 ID 端点
     *
     * @param response HttpServletResponse
     * @throws IOException 重定向可能抛错
     */
    @GetMapping("/id")
    public void getId(HttpServletResponse response) throws IOException {
        try {
            String worker = masterService.getWorker() + "worker/id";
            log.warn("Redirect to {}", worker);
            // 返回一个 301 重定向
            response.sendRedirect(worker);
        } catch (WorkerManagementException e) {
            response.setStatus(500);
            response.getWriter().write(e.getMessage());
        }
    }

    /**
     * Worker 注销接口
     *
     * @param body 注销信息
     * @return 成功 || 失败
     */
    @PostMapping("/unregister")
    public boolean unregister(@RequestBody @Validated RegistryBody body) {
        return masterService.unregister(body);
    }
}
