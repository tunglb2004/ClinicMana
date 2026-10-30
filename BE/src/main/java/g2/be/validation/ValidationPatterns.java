package g2.be.validation;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String FULL_NAME = "^[\\p{L}]+(?:[\\p{L} .'-]*[\\p{L}])?$";
    public static final String OPTIONAL_FULL_NAME = "^$|^[\\p{L}]+(?:[\\p{L} .'-]*[\\p{L}])?$";
    public static final String EMAIL = "^(?=.{6,30}$)(?!.*\\.\\.)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String OPTIONAL_EMAIL = "^$|^(?=.{6,30}$)(?!.*\\.\\.)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PHONE = "^0[0-9]{9,10}$";
    public static final String OPTIONAL_PHONE = "^$|^0[0-9]{9,10}$";
    public static final String CCCD = "^[0-9]{12}$";
    public static final String OPTIONAL_CCCD = "^$|^[0-9]{12}$";
    public static final String LOGIN_IDENTIFIER = "^(?:(?=.{6,30}$)(?!.*\\.\\.)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|0[0-9]{9,10})$";
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{8,100}$";
    public static final String SERVICE_CODE = "^[A-Za-z0-9._-]{1,50}$";
}
