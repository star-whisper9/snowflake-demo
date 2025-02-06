package press.cirno.snowflakedemo.pojo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "worker")
@Data
public class WorkerPO {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String ip;

    @Column(nullable = false, unique = true)
    private String mac;

    @Column(name = "exposed_address", nullable = false)
    private String exposedAddress;

    @Column(nullable = false)
    private Long lastHeartbeat;

    public WorkerPO(String ip, String mac, Long lastHeartbeat, String exposedAddress) {
        this.ip = ip;
        this.mac = mac;
        this.lastHeartbeat = lastHeartbeat;
        this.exposedAddress = exposedAddress;
    }

    public WorkerPO() {

    }
}
