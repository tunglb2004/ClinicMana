package g2.be.repository;

import g2.be.entity.Nurse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn bảng nurses.
 * 
 * @author Nam
 */
@Repository
public interface NurseRepository extends JpaRepository<Nurse, String> {

    // Tìm nurse theo userId (dùng khi gán/gỡ member)
    Optional<Nurse> findByUser_UserId(Long userId);

    // Lấy danh sách y tá thuộc 1 khoa (dùng khi xem chi tiết khoa)
    List<Nurse> findByDepartment_DeptId(Integer deptId);
}
