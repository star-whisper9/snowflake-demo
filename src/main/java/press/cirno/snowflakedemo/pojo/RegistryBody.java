package press.cirno.snowflakedemo.pojo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import press.cirno.snowflakedemo.util.ValidTime;

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
    @Pattern(regexp = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}[\\d:]*$")
    private String exposedAddress;

    public RegistryBody(String ip, String mac, Long timestamp, String exposedAddress) {
        this.ip = ip;
        this.mac = mac;
        this.timestamp = timestamp;
        this.exposedAddress = exposedAddress;
    }

    public RegistryBody(String ip, String mac, Long timestamp) {
        this.ip = ip;
        this.mac = mac;
        this.timestamp = timestamp;
    }

    public RegistryBody() {

    }
}
