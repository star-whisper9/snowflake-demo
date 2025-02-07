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

    // ip mac 组合识别一个唯一 Worker
    @Column(nullable = false, unique = true)
    private String ip;

    @Column(nullable = false, unique = true)
    private String mac;

    // Worker 用于外部访问的地址
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
