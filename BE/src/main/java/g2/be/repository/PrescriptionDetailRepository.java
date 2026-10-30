package g2.be.repository;

import g2.be.entity.PrescriptionDetail;
import g2.be.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Long> {
    List<PrescriptionDetail> findByPrescription(Prescription prescription);
    void deleteByPrescription(Prescription prescription);
}
