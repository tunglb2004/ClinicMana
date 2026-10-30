package g2.be.service;

import g2.be.dto.request.ServiceCatalogRequest;
import g2.be.entity.Department;
import g2.be.entity.Service;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public Service createService(ServiceCatalogRequest request) {
        String serviceId = generateNextServiceId();
        Department department = findDepartment(request.getDeptId());

        validateMaxPatients(request.getMaxPatients());
        validateStandardPrice(request.getPrice());

        Service service = Service.builder()
                .serviceId(serviceId)
                .serviceName(request.getServiceName().trim())
                .price(request.getPrice())
                .description(normalizeOptionalText(request.getDescription()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(false)
                .bhytServiceCode(normalizeOptionalText(request.getBhytServiceCode()))
                .department(department)
                .maxPatients(request.getMaxPatients())
                .build();

        return serviceRepository.save(service);
    }

    @Transactional
    public Service updateService(String serviceId, ServiceCatalogRequest request) {
        Service service = findExistingService(serviceId);
        Department department = findDepartment(request.getDeptId());

        validateMaxPatients(request.getMaxPatients());
        validateStandardPrice(request.getPrice());

        service.setServiceName(request.getServiceName().trim());
        service.setPrice(request.getPrice());
        service.setDescription(normalizeOptionalText(request.getDescription()));
        service.setIsActive(request.getIsActive());
        service.setBhytServiceCode(normalizeOptionalText(request.getBhytServiceCode()));
        service.setDepartment(department);
        service.setMaxPatients(request.getMaxPatients());

        return serviceRepository.save(service);
    }

    @Transactional
    public Service updateServiceStatus(String serviceId, Boolean isActive) {
        Service service = findExistingService(serviceId);
        service.setIsActive(Boolean.TRUE.equals(isActive));
        return serviceRepository.save(service);
    }

    @Transactional
    public void deleteService(String serviceId) {
        Service service = findExistingService(serviceId);
        service.setIsActive(false);
        service.setIsDeleted(true);
        serviceRepository.save(service);
    }

    @Transactional(readOnly = true)
    public List<Service> getVisibleServices(boolean includeInactive) {
        if (includeInactive) {
            return serviceRepository.findAllByIsDeletedFalse();
        }
        return serviceRepository.findAllByIsDeletedFalseAndIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public void populateOperationalRoomAvailability(List<Service> services) {
        if (services == null || services.isEmpty()) {
            return;
        }

        List<String> paraclinicalServiceIds = services.stream()
                .filter(this::requiresOperationalRoomMapping)
                .map(Service::getServiceId)
                .filter(serviceId -> serviceId != null && !serviceId.isBlank())
                .toList();

        Set<String> serviceIdsWithOperationalRooms = paraclinicalServiceIds.isEmpty()
                ? Set.of()
                : new LinkedHashSet<>(serviceRepository.findServiceIdsWithOperationalRooms(paraclinicalServiceIds));

        services.forEach(service -> service.setHasOperationalRoom(
                !requiresOperationalRoomMapping(service)
                        || serviceIdsWithOperationalRooms.contains(service.getServiceId())));
    }

    private String generateNextServiceId() {
        List<Service> allServices = serviceRepository.findAll();
        int maxId = 0;
        for (Service s : allServices) {
            if (s.getServiceId() != null && s.getServiceId().startsWith("S-")) {
                try {
                    int idNum = Integer.parseInt(s.getServiceId().substring(2));
                    if (idNum > maxId) {
                        maxId = idNum;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("S-%03d", maxId + 1);
    }

    private void validateMaxPatients(Integer maxPatients) {
        if (maxPatients == null) {
            throw new RuntimeException("maxPatients là bắt buộc");
        }
        if (maxPatients < 1 || maxPatients > 10) {
            throw new RuntimeException("Giới hạn bệnh nhân phải nằm trong khoảng 1-10");
        }
    }

    private void validateStandardPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Gia dich vu phai lon hon 0");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Department findDepartment(Integer deptId) {
        if (deptId == null) {
            throw new AppException(ErrorCode.DEPT_NOT_FOUND);
        }

        return departmentRepository.findByDeptIdAndIsDeletedFalse(deptId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPT_NOT_FOUND));
    }

    private Service findExistingService(String serviceId) {
        return serviceRepository.findByServiceIdAndIsDeletedFalse(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));
    }

    private boolean requiresOperationalRoomMapping(Service service) {
        return service != null
                && service.getDepartment() != null
                && service.getDepartment().getDeptType() == Department.DeptType.PARACLINICAL;
    }
}
