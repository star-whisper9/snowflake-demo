package press.cirno.snowflakedemo.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HeartbeatBody extends RegistryBody {
    @Min(1)
    @Max(1023)
    private int workerId;

    public HeartbeatBody(String ip, String mac, Long timestamp, int workerId) {
        super(ip, mac, timestamp);
        this.workerId = workerId;
    }
}
