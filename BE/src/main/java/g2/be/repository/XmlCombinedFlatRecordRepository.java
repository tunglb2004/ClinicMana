package g2.be.repository;

import g2.be.entity.XmlCombinedFlatRecord;
import g2.be.entity.XmlCombinedFlatRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XmlCombinedFlatRecordRepository extends JpaRepository<XmlCombinedFlatRecord, XmlCombinedFlatRecordId> {
    List<XmlCombinedFlatRecord> findByMaLk(String maLk);
}
