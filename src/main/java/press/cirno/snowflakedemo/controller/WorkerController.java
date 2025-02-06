package press.cirno.snowflakedemo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import press.cirno.snowflakedemo.exception.TimeAccuracyException;
import press.cirno.snowflakedemo.exception.WorkerManagementException;
import press.cirno.snowflakedemo.service.IWorkerService;

@RestController
@RequestMapping("/worker")
public class WorkerController {
    private final IWorkerService workerService;

    @Autowired
    public WorkerController(IWorkerService workerService) {
        this.workerService = workerService;
    }

    /**
     * 获取 ID
     *
     * @return ID || -1
     */
    @GetMapping("/id")
    public long getId() {
        try {
            return workerService.nextId();
        } catch (WorkerManagementException | TimeAccuracyException e) {
            return -1;
        }
    }
}
