package g2.be.repository;

import g2.be.entity.MockInsuranceService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho tra cứu dữ liệu dịch vụ bảo hiểm giả lập
 */
@Repository
public interface MockInsuranceServiceRepository extends JpaRepository<MockInsuranceService, Integer> {

    /**
     * Tra cứu dịch vụ theo mã
     */
    Optional<MockInsuranceService> findByServiceCode(String serviceCode);

    /**
     * Kiểm tra tồn tại mã dịch vụ
     */
    boolean existsByServiceCode(String serviceCode);
}
