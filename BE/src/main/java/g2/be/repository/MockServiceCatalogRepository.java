package g2.be.repository;

import g2.be.entity.MockServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository tra cứu danh mục dịch vụ BHYT (giá trần)
 */
@Repository
public interface MockServiceCatalogRepository extends JpaRepository<MockServiceCatalog, Integer> {

    /**
     * Tra cứu giá trần BHYT theo mã dịch vụ (chỉ lấy dịch vụ còn hiệu lực)
     */
    Optional<MockServiceCatalog> findByServiceCodeAndIsActiveTrue(String serviceCode);

    /**
     * Tra cứu theo mã dịch vụ
     */
    Optional<MockServiceCatalog> findByServiceCode(String serviceCode);
}
