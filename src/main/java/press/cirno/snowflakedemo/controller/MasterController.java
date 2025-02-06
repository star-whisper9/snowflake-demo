package press.cirno.snowflakedemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import press.cirno.snowflakedemo.pojo.HeartbeatBody;
import press.cirno.snowflakedemo.pojo.RegistryBody;
import press.cirno.snowflakedemo.pojo.StandardResponse;
import press.cirno.snowflakedemo.service.IMasterService;

@RestController
@RequestMapping("/master")
public class MasterController {
    private final IMasterService masterService;

    @Autowired
    public MasterController(IMasterService masterService) {
        this.masterService = masterService;
    }

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

    @PostMapping("/heartbeat")
    public StandardResponse<String> heartbeat(@RequestBody @Validated HeartbeatBody body) {
        Integer workerId = masterService.heartbeat(body);
        if (workerId == -1) {
            return StandardResponse.fail("节点未注册");
        } else {
            return StandardResponse.success();
        }
    }

    @GetMapping("/id")
    public String getId() {
        String worker = masterService.getWorker();
        // 返回一个 301 重定向
        return "redirect:" + worker + "/worker/id";
    }
}
