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
}
