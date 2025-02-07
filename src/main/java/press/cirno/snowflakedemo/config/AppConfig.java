package press.cirno.snowflakedemo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AppConfig {
    @Value("${sf.exposed-address}")
    private String exposedAddress;

    @Value("${sf.master-address}")
    private String masterAddress;

    @Value("${sf.master.enabled}")
    private boolean master = false;

    @Value("${sf.worker.enabled}")
    private boolean worker = false;

    // 回拨解决策略：reject/wait/auto
    @Value("${sf.algo.clock-drift-strategy}")
    private String clockDriftStrategy = "auto";

    // wait 模式最长等待时间
    @Value("${sf.algo.wait-time}")
    private long waitTime = 30L;

    @Value("${sf.algo.start-time}")
    private String startTime = "2024-08-12 08:12:00";
}
