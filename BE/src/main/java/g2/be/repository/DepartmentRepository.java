package g2.be.repository;

import g2.be.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn bảng departments.
 * Hỗ trợ: tìm kiếm theo tên, kiểm tra trùng tên, phân trang.
 * 
 * @author Nam
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * Tìm kiếm khoa theo tên (không phân biệt hoa thường)
     * Dùng cho chức năng tìm kiếm ở danh sách khoa
     */
    List<Department> findAllByIsDeletedFalseOrderByDeptIdDesc();

    /**
     * Kiểm tra tên khoa đã tồn tại chưa
     * Dùng khi tạo khoa mới để tránh trùng tên
     */
    List<Department> findAllByIsDeletedFalseAndIsActiveTrueOrderByDeptIdDesc();

    /**
     * Kiểm tra tên khoa đã tồn tại chưa (loại trừ khoa hiện tại)
     * Dùng khi cập nhật khoa để tránh trùng tên với khoa khác
     */
    Optional<Department> findByDeptIdAndIsDeletedFalse(Integer deptId);

    Boolean existsByDeptNameAndIsDeletedFalse(String deptName);

    Boolean existsByDeptNameAndIsDeletedFalseAndDeptIdNot(String deptName, Integer deptId);
}
