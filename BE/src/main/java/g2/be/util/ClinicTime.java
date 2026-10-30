package g2.be.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class ClinicTime {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ClinicTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static LocalDateTime startOfToday() {
        return today().atStartOfDay();
    }

    public static LocalDateTime endOfToday() {
        return LocalDateTime.of(today(), LocalTime.MAX);
    }
}
