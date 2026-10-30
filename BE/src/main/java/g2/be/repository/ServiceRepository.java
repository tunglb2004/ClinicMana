package g2.be.repository;

import g2.be.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {
    List<Service> findAllByIsDeletedFalse();

    List<Service> findAllByIsDeletedFalseAndIsActiveTrue();

    Optional<Service> findByServiceIdAndIsDeletedFalse(String serviceId);

    Optional<Service> findByServiceIdAndIsDeletedFalseAndIsActiveTrue(String serviceId);

    List<Service> findAllByServiceIdInAndIsDeletedFalseAndIsActiveTrue(Collection<String> serviceIds);

        @Query("""
                        select distinct s.serviceId
                        from Service s
                        join s.rooms r
                        where s.serviceId in :serviceIds
                            and s.isDeleted = false
                            and r.isDeleted = false
                            and upper(r.status) = 'ACTIVE'
                        """)
        List<String> findServiceIdsWithOperationalRooms(@Param("serviceIds") Collection<String> serviceIds);
}
