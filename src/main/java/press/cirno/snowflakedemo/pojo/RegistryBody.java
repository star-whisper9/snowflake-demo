package press.cirno.snowflakedemo.pojo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import press.cirno.snowflakedemo.util.ValidTime;

/**
 * Worker 注册信息
 */
@Getter
@Setter
@ToString
public class RegistryBody {
    @NotNull
    @Pattern(regexp = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    private String ip;

    @Pattern(regexp = "^([0-9A-F]{2}[-]){5}([0-9A-F]{2})$")
    @NotNull
    private String mac;

    @NotNull
    @ValidTime
    private Long timestamp;

    @NotNull
    @Pattern(regexp = "^(http|https)://([\\w.-]+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(:[0-9]+)?(/.*)?$")
    private String exposedAddress;

    public RegistryBody(String ip, String mac, Long timestamp, String exposedAddress) {
        this.ip = ip;
        this.mac = mac;
        this.timestamp = timestamp;
        this.exposedAddress = exposedAddress;
    }

    public RegistryBody() {

    }
}
