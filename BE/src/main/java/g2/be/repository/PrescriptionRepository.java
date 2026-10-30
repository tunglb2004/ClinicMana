package g2.be.repository;

import g2.be.entity.Prescription;
import g2.be.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, String> {
    Optional<Prescription> findByMedicalRecord(MedicalRecord medicalRecord);
}
