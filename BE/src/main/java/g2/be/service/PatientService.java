package g2.be.service;

import g2.be.dto.request.UpdatePatientRequest;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.PatientResponse;
import g2.be.entity.Patient;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.PatientRepository;
import g2.be.repository.UserRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> getPatients(int page, int size, String keyword, String sortBy,
            String sortDir, boolean today) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Patient> patientPage;
        if (today) {
            LocalDateTime startOfDay = ClinicTime.startOfToday();
            LocalDateTime endOfDay = ClinicTime.endOfToday();
            patientPage = patientRepository.findByKeywordAndDateRange(keyword, startOfDay, endOfDay, pageable);
        } else {
            patientPage = patientRepository.findByKeyword(keyword, pageable);
        }

        List<PatientResponse> content = patientPage.getContent().stream()
                .map(PatientResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<PatientResponse>builder()
                .content(content)
                .pageNumber(patientPage.getNumber())
                .pageSize(patientPage.getSize())
                .totalElements(patientPage.getTotalElements())
                .totalPages(patientPage.getTotalPages())
                .first(patientPage.isFirst())
                .last(patientPage.isLast())
                .build();
    }

    @Transactional
    public PatientResponse updatePatient(String patientId, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        User user = patient.getUser();
        if (user.getIsDeleted()) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        // Update User info
        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AppException(ErrorCode.REGISTER_EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new AppException(ErrorCode.REGISTER_PHONE_EXISTS);
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getCccd() != null)
            user.setCccd(request.getCccd());
        if (request.getGender() != null) {
            try {
                user.setGender(User.Gender.valueOf(request.getGender()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid gender or handle error
            }
        }
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);

        // Update Patient specific info
        if (request.getDateOfBirth() != null)
            patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getBloodType() != null)
            patient.setBloodType(request.getBloodType());
        if (request.getHealthInsuranceNumber() != null)
            patient.setHealthInsuranceNumber(request.getHealthInsuranceNumber());
        if (request.getAllergies() != null)
            patient.setAllergies(request.getAllergies());
        if (request.getAddress() != null)
            patient.setAddress(request.getAddress());
        if (request.getHeight() != null)
            patient.setHeight(request.getHeight());
        if (request.getWeight() != null)
            patient.setWeight(request.getWeight());
        if (request.getBackgroundDisease() != null)
            patient.setBackgroundDisease(request.getBackgroundDisease());
        if (request.getEmergencyContactName() != null)
            patient.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelationship() != null)
            patient.setEmergencyContactRelationship(request.getEmergencyContactRelationship());

        Patient updatedPatient = patientRepository.save(patient);
        return PatientResponse.fromEntity(updatedPatient);
    }

    @Transactional
    public void deletePatient(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        userManagementService.deleteUser(patient.getUser().getUserId());
    }
}
