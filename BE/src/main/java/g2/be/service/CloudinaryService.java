package g2.be.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PDF_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        validatePresent(file);
        try {
            String resourceType = isPdf(file) ? "raw" : "auto";
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", resourceType
            );

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Error uploading file to Cloudinary", e);
            throw e;
        }
    }

    public String uploadImageFile(MultipartFile file, String folderName) throws IOException {
        validatePresent(file);
        validateMaxSize(file, MAX_IMAGE_SIZE_BYTES);
        if (!isAllowedImage(file)) {
            throw new AppException(ErrorCode.UPLOAD_FILE_UNSUPPORTED_TYPE);
        }
        return uploadFile(file, folderName);
    }

    public String uploadPdfFile(MultipartFile file, String folderName) throws IOException {
        validatePresent(file);
        validateMaxSize(file, MAX_PDF_SIZE_BYTES);
        if (!isPdf(file)) {
            throw new AppException(ErrorCode.UPLOAD_FILE_UNSUPPORTED_TYPE);
        }
        return uploadFile(file, folderName);
    }

    private void validatePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UPDATE_PROFILE_MISSING_FIELDS);
        }
    }

    private void validateMaxSize(MultipartFile file, long maxSizeBytes) {
        if (file.getSize() > maxSizeBytes) {
            throw new AppException(ErrorCode.UPLOAD_FILE_SIZE_EXCEEDS);
        }
    }

    private boolean isAllowedImage(MultipartFile file) {
        String contentType = normalize(file.getContentType());
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return true;
        }

        String originalFilename = normalize(file.getOriginalFilename());
        return originalFilename.endsWith(".jpg")
                || originalFilename.endsWith(".jpeg")
                || originalFilename.endsWith(".png")
                || originalFilename.endsWith(".webp");
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = normalize(file.getContentType());
        if ("application/pdf".equals(contentType) || contentType.endsWith("+pdf")) {
            return true;
        }

        String originalFilename = normalize(file.getOriginalFilename());
        return originalFilename.endsWith(".pdf");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
