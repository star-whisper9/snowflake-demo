package press.cirno.snowflakedemo.repositry;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import press.cirno.snowflakedemo.pojo.WorkerPO;

@Repository
public interface WorkerDAO extends CrudRepository<WorkerPO, Integer> {
    WorkerPO findTopByOrderByIdDesc();

    WorkerPO findTopByIpAndMacOrderByIdDesc(String ip, String mac);
}
