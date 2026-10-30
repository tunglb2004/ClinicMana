package g2.be.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g2.be.entity.User;


/**
 * Repository để truy vấn bảng users trong database.
 * 
 * @author Nam
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

       Optional<User> findByUsername(String username);

       Optional<User> findByEmail(String email);

       Optional<User> findByPhoneNumber(String phoneNumber);

       Boolean existsByUsername(String username);

       Boolean existsByEmail(String email);

       Boolean existsByPhoneNumber(String phoneNumber);

       /**
        * Kiểm tra CCCD đã tồn tại chưa (loại trừ user hiện tại)
        * Dùng khi update user để tránh trùng CCCD với user khác
        */
       Boolean existsByCccdAndUserIdNot(String cccd, Long userId);

       Boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, Long userId);

       /**
        * Kiểm tra CCCD đã tồn tại chưa
        * Dùng khi tạo user mới
        */
       Boolean existsByCccd(String cccd);

       /**
        * Kiểm tra mã nhân viên đã tồn tại chưa
        */
       Boolean existsByStaffId(String staffId);

       /**
        * Lấy danh sách users chưa bị xóa (soft delete) với phân trang
        * 
        * @param isDeleted - false để lấy users chưa xóa
        * @param pageable  - thông tin phân trang (page, size, sort)
        * @return Page<User> - danh sách users với thông tin phân trang
        */
       Page<User> findByIsDeleted(Boolean isDeleted, Pageable pageable);

       /**
        * Lấy toàn bộ users chưa bị xóa (không phân trang), dùng cho tác vụ đồng bộ dữ liệu.
        */
       List<User> findByIsDeletedFalse();

       /**
        * Lấy danh sách users theo trạng thái active và chưa bị xóa
        */
       Page<User> findByIsActiveAndIsDeleted(Boolean isActive, Boolean isDeleted, Pageable pageable);

       /**
        * Tìm user theo ID và chưa bị xóa
        * 
        * @param userId    - ID của user
        * @param isDeleted - false để lấy user chưa xóa
        * @return Optional<User>
        */
       /**
        * Tìm kiếm users theo từ khóa và vai trò.
        * Keyword: tìm theo username, fullName hoặc userId
        * Role: tìm theo tên role (nếu có)
        */
       @Query(value = "SELECT DISTINCT u FROM User u JOIN u.roles r WHERE u.isDeleted = false " +
                     "AND (:keyword IS NULL OR :keyword = '' OR " +
                     "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "     LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "     CAST(u.userId AS string) LIKE :keyword) " +
                     "AND (:role IS NULL OR :role = '' OR :role = 'all' " +
                     "     OR r.roleName = :role)",
              countQuery = "SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE u.isDeleted = false " +
                     "AND (:keyword IS NULL OR :keyword = '' OR " +
                     "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "     LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "     CAST(u.userId AS string) LIKE :keyword) " +
                     "AND (:role IS NULL OR :role = '' OR :role = 'all' " +
                     "     OR r.roleName = :role)")
       Page<User> searchUsers(@Param("keyword") String keyword, @Param("role") String role, Pageable pageable);

       Optional<User> findByUserIdAndIsDeleted(Long userId, Boolean isDeleted);

       @Query("SELECT u FROM User u LEFT JOIN FETCH u.doctor d LEFT JOIN FETCH d.department " +
                     "WHERE u.userId = :userId AND u.isDeleted = false")
       Optional<User> findByIdWithDoctor(@Param("userId") Long userId);

       @Query("SELECT u FROM User u LEFT JOIN FETCH u.patient p " +
                     "WHERE u.userId = :userId AND u.isDeleted = false")
       Optional<User> findByIdWithPatient(@Param("userId") Long userId);

       @Query("SELECT u FROM User u WHERE (u.email = :identifier OR u.phoneNumber = :identifier) AND u.isDeleted = false")
       Optional<User> findByEmailOrPhoneNumber(@Param("identifier") String identifier);

       /**
        * Tìm kiếm user theo keyword (username, email, fullName, phoneNumber)
        */
       @Query("SELECT u FROM User u WHERE u.isDeleted = :isDeleted AND " +
                     "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<User> findByKeywordAndIsDeleted(@Param("keyword") String keyword,
                     @Param("isDeleted") Boolean isDeleted,
                     Pageable pageable);

       /**
        * Lấy danh sách user theo role và chưa bị xóa
        */
       @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.isDeleted = :isDeleted")
       Page<User> findByRoleAndIsDeleted(@Param("roleName") String roleName, 
                                         @Param("isDeleted") Boolean isDeleted, 
                                         Pageable pageable);

       /**
        * Tìm kiếm user theo role và keyword
        */
       @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName " +
              "AND u.isDeleted = :isDeleted " +
              "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<User> findByRoleAndKeyword(@Param("roleName") String roleName, 
                                       @Param("keyword") String keyword, 
                                       @Param("isDeleted") Boolean isDeleted, 
                                       Pageable pageable);

       /**
        * Lọc user theo role, trạng thái active và chưa bị xóa
        */
       @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName " +
              "AND u.isActive = :isActive AND u.isDeleted = :isDeleted")
       Page<User> findByRoleAndIsActive(@Param("roleName") String roleName, 
                                        @Param("isActive") Boolean isActive, 
                                        @Param("isDeleted") Boolean isDeleted, 
                                        Pageable pageable);

       @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
              "WHERE u.isDeleted = false AND r.roleName IN :roleNames")
       long countDistinctByRoleNames(@Param("roleNames") List<String> roleNames);

       @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
              "WHERE u.isDeleted = false AND u.isActive = true AND r.roleName IN :roleNames")
       long countActiveDistinctByRoleNames(@Param("roleNames") List<String> roleNames);

       @Query(value = """
              SELECT
                COUNT(DISTINCT CASE WHEN r.role_name IN ('ROLE_DOCTOR','ROLE_CLINICALDOCTOR','ROLE_SPECIALISTDOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_CASHIER','ROLE_CLINIC_MANAGER','ROLE_MANAGER') THEN u.user_id END) AS total_staff,
                COUNT(DISTINCT CASE WHEN r.role_name IN ('ROLE_DOCTOR','ROLE_CLINICALDOCTOR','ROLE_SPECIALISTDOCTOR') THEN u.user_id END) AS doctor_count,
                COUNT(DISTINCT CASE WHEN r.role_name = 'ROLE_NURSE' THEN u.user_id END) AS nurse_count,
                COUNT(DISTINCT CASE WHEN u.is_active = true AND r.role_name IN ('ROLE_DOCTOR','ROLE_CLINICALDOCTOR','ROLE_SPECIALISTDOCTOR','ROLE_NURSE','ROLE_RECEPTIONIST','ROLE_CASHIER','ROLE_CLINIC_MANAGER','ROLE_MANAGER') THEN u.user_id END) AS active_staff
              FROM users u
              JOIN user_roles ur ON ur.user_id = u.user_id
              JOIN roles r ON r.role_id = ur.role_id
              WHERE u.is_deleted = false
              """, nativeQuery = true)
       Object[] getManagerStaffStats();
}

