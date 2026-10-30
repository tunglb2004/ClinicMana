package g2.be.repository;

import g2.be.entity.MockCitizenData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho tra cứu dữ liệu công dân giả lập
 */
@Repository
public interface MockCitizenDataRepository extends JpaRepository<MockCitizenData, Integer> {

    /**
     * Tra cứu công dân theo số CCCD
     */
    Optional<MockCitizenData> findByCccdNumber(String cccdNumber);

    /**
     * Tra cứu công dân theo mã thẻ BHYT
     */
    Optional<MockCitizenData> findByInsuranceCode(String insuranceCode);

    /**
     * Kiểm tra tồn tại CCCD
     */
    boolean existsByCccdNumber(String cccdNumber);

    /**
     * Kiểm tra tồn tại mã thẻ BHYT
     */
    boolean existsByInsuranceCode(String insuranceCode);
}
