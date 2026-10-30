package g2.be.service;

import g2.be.dto.request.ServiceCatalogRequest;
import g2.be.entity.Department;
import g2.be.entity.Service;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    @Test
    void createService_Success() {
        ServiceCatalogRequest request = new ServiceCatalogRequest();
        request.setServiceName("Khám");
        request.setPrice(BigDecimal.valueOf(100));
        request.setDeptId(1);
        request.setMaxPatients(10);

        Department dept = Department.builder().deptId(1).build();
        when(departmentRepository.findByDeptIdAndIsDeletedFalse(1)).thenReturn(Optional.of(dept));
        when(serviceRepository.save(any(Service.class))).thenAnswer(i -> i.getArgument(0));

        Service result = serviceManagementService.createService(request);

        assertNotNull(result);
        assertEquals(dept, result.getDepartment());
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void createService_Fail_DeptNotFound() {
        ServiceCatalogRequest request = new ServiceCatalogRequest();
        request.setDeptId(999);
        request.setMaxPatients(10);

        when(departmentRepository.findByDeptIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            serviceManagementService.createService(request);
        });
        assertEquals(ErrorCode.DEPT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteServiceShouldSoftDeleteAndDeactivate() {
        Service service = Service.builder()
                .serviceId("S-001")
                .isActive(true)
                .isDeleted(false)
                .build();

        when(serviceRepository.findByServiceIdAndIsDeletedFalse("S-001")).thenReturn(Optional.of(service));

        serviceManagementService.deleteService("S-001");

        assertTrue(service.getIsDeleted());
        assertFalse(service.getIsActive());
        verify(serviceRepository).save(service);
    }
}
