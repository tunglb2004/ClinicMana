package g2.be.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import g2.be.entity.Invoice;
import g2.be.entity.Patient;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SepayEinvoiceClient {

    private static final DateTimeFormatter ISSUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sepay.einvoice.base-url:https://einvoice-api-sandbox.sepay.vn}")
    private String baseUrl;

    @Value("${sepay.einvoice.client-id:}")
    private String clientId;

    @Value("${sepay.einvoice.client-secret:}")
    private String clientSecret;

    @Value("${sepay.einvoice.provider-account-id:}")
    private String configuredProviderAccountId;

    @Value("${sepay.einvoice.template-code:}")
    private String configuredTemplateCode;

    @Value("${sepay.einvoice.invoice-series:}")
    private String configuredInvoiceSeries;

    @Value("${sepay.einvoice.item-unit:lần}")
    private String itemUnit;

    @Value("${sepay.einvoice.default-tax-rate:0}")
    private BigDecimal defaultTaxRate;

    @Value("${sepay.einvoice.poll-interval-millis:3000}")
    private long pollIntervalMillis;

    @Value("${sepay.einvoice.poll-max-attempts:20}")
    private int pollMaxAttempts;

    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiresAt;

    public IssuedEinvoice issueInvoice(Invoice invoice, List<ServiceOrder> serviceOrders, LocalDateTime issuedAt) {
        if (invoice == null) {
            throw new RuntimeException("Invoice is required for SePay eInvoice issuance.");
        }
        if (serviceOrders == null || serviceOrders.isEmpty()) {
            throw new RuntimeException("No active service orders found to create SePay eInvoice.");
        }

        String accessToken = getOrRefreshAccessToken();
        ProviderTemplate providerTemplate = resolveProviderTemplate(accessToken);

        CreateInvoiceRequest request = buildCreateInvoiceRequest(invoice, serviceOrders, issuedAt, providerTemplate);
        String trackingCode = submitCreateInvoice(accessToken, request);
        CreateCheckResult checkResult = pollCreateStatus(accessToken, trackingCode);

        String referenceCode = firstNonBlank(checkResult.referenceCode(), checkResult.invoiceReferenceCode());
        String pdfUrl = checkResult.pdfUrl();
        String xmlUrl = checkResult.xmlUrl();

        if ((pdfUrl == null || pdfUrl.isBlank()) && referenceCode != null && !referenceCode.isBlank()) {
            InvoiceDetail invoiceDetail = fetchInvoiceDetail(accessToken, referenceCode);
            pdfUrl = firstNonBlank(pdfUrl, invoiceDetail.pdfUrl());
            xmlUrl = firstNonBlank(xmlUrl, invoiceDetail.xmlUrl());
        }

        if (referenceCode == null || referenceCode.isBlank() || pdfUrl == null || pdfUrl.isBlank()) {
            throw new RuntimeException("SePay returned successful create status but invoice detail is incomplete.");
        }

        return new IssuedEinvoice(referenceCode, pdfUrl, xmlUrl, trackingCode);
    }

    private synchronized String getOrRefreshAccessToken() {
        Instant now = Instant.now();
        if (cachedAccessToken != null && cachedAccessTokenExpiresAt != null && now.isBefore(cachedAccessTokenExpiresAt)) {
            return cachedAccessToken;
        }

        validateAuthenticationConfig();

        String basicAuth = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        JsonNode rootNode = exchangeForJson(HttpMethod.POST, "/v1/token", requestEntity);
        ensureSuccess(rootNode, "SePay token request failed.");

        JsonNode dataNode = rootNode.path("data");
        String accessToken = textValue(dataNode, "access_token");
        long expiresIn = longValue(dataNode, "expires_in", 86400L);

        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("SePay token response does not contain access_token.");
        }

        cachedAccessToken = accessToken;
        cachedAccessTokenExpiresAt = now.plusSeconds(Math.max(60L, expiresIn - 60L));
        return cachedAccessToken;
    }

    private ProviderTemplate resolveProviderTemplate(String accessToken) {
        String providerAccountId = configuredProviderAccountId;
        if (providerAccountId == null || providerAccountId.isBlank()) {
            providerAccountId = resolveActiveProviderAccountId(accessToken);
        }

        JsonNode detailRoot = exchangeForJson(HttpMethod.GET,
                "/v1/provider-accounts/" + providerAccountId,
                authorizedJsonEntity(accessToken, null));
        ensureSuccess(detailRoot, "Unable to load SePay provider account detail.");

        JsonNode dataNode = detailRoot.path("data");
        if (!dataNode.path("active").asBoolean(false)) {
            throw new RuntimeException("Configured SePay provider account is not active.");
        }

        JsonNode templatesNode = dataNode.path("templates");
        if (!templatesNode.isArray() || templatesNode.isEmpty()) {
            throw new RuntimeException("SePay provider account does not expose any invoice templates.");
        }

        for (JsonNode templateNode : templatesNode) {
            String templateCode = textValue(templateNode, "template_code");
            String invoiceSeries = textValue(templateNode, "invoice_series");
            String invoiceLabel = textValue(templateNode, "invoice_label");

            boolean matchesConfiguredTemplate = configuredTemplateCode == null || configuredTemplateCode.isBlank()
                    || Objects.equals(configuredTemplateCode, templateCode);
            boolean matchesConfiguredSeries = configuredInvoiceSeries == null || configuredInvoiceSeries.isBlank()
                    || Objects.equals(configuredInvoiceSeries, invoiceSeries);

            if (matchesConfiguredTemplate && matchesConfiguredSeries) {
                return new ProviderTemplate(providerAccountId, templateCode, invoiceSeries, invoiceLabel);
            }
        }

        if ((configuredTemplateCode != null && !configuredTemplateCode.isBlank())
                || (configuredInvoiceSeries != null && !configuredInvoiceSeries.isBlank())) {
            throw new RuntimeException("Configured SePay template_code/invoice_series is not available on the selected provider account.");
        }

        JsonNode firstTemplate = templatesNode.get(0);
        return new ProviderTemplate(
                providerAccountId,
                textValue(firstTemplate, "template_code"),
                textValue(firstTemplate, "invoice_series"),
                textValue(firstTemplate, "invoice_label")
        );
    }

    private String resolveActiveProviderAccountId(String accessToken) {
        JsonNode rootNode = exchangeForJson(HttpMethod.GET,
                "/v1/provider-accounts?page=1&per_page=20",
                authorizedJsonEntity(accessToken, null));
        ensureSuccess(rootNode, "Unable to list SePay provider accounts.");

        JsonNode itemsNode = rootNode.path("data").path("items");
        if (!itemsNode.isArray()) {
            throw new RuntimeException("SePay provider accounts response is invalid.");
        }

        for (JsonNode itemNode : itemsNode) {
            if (itemNode.path("active").asBoolean(false)) {
                String id = textValue(itemNode, "id");
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }

        throw new RuntimeException("No active SePay provider account found for eInvoice issuance.");
    }

    private CreateInvoiceRequest buildCreateInvoiceRequest(Invoice invoice,
                                                           List<ServiceOrder> serviceOrders,
                                                           LocalDateTime issuedAt,
                                                           ProviderTemplate providerTemplate) {
        Buyer buyer = buildBuyer(invoice);
        List<CreateInvoiceItem> items = buildItems(invoice, serviceOrders, providerTemplate);

        return new CreateInvoiceRequest(
                providerTemplate.templateCode(),
                providerTemplate.invoiceSeries(),
                issuedAt.format(ISSUE_DATE_FORMATTER),
                "VND",
                providerTemplate.providerAccountId(),
                mapPaymentMethod(invoice.getPaymentMethod()),
                false,
                buyer,
                items,
                buildOrderNumber(invoice),
                buildNotes(invoice)
        );
    }

    private Buyer buildBuyer(Invoice invoice) {
        Patient patient = invoice.getMedicalRecord() != null ? invoice.getMedicalRecord().getPatient() : null;
        User user = patient != null ? patient.getUser() : null;

        String buyerName = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : (patient != null ? patient.getPatientId() : invoice.getInvoiceId());

        return new Buyer(
                "personal",
                buyerName,
                null,
                null,
                patient != null && patient.getAddress() != null && !patient.getAddress().isBlank()
                        ? patient.getAddress()
                        : "Chưa cập nhật địa chỉ",
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                patient != null ? patient.getPatientId() : invoice.getInvoiceId(),
                user != null ? user.getCccd() : null
        );
    }

    private List<CreateInvoiceItem> buildItems(Invoice invoice, List<ServiceOrder> serviceOrders, ProviderTemplate providerTemplate) {
        List<CreateInvoiceItem> items = new ArrayList<>();
        boolean requiresTaxRate = requiresTaxRate(providerTemplate);

        List<PricedServiceOrder> pricedOrders = new ArrayList<>();
        BigDecimal totalOriginalAmount = BigDecimal.ZERO;
        for (ServiceOrder serviceOrder : serviceOrders) {
            if (serviceOrder.getService() == null || serviceOrder.getService().getPrice() == null) {
                continue;
            }

            BigDecimal normalizedPrice = normalizeCurrencyAmount(serviceOrder.getService().getPrice());
            pricedOrders.add(new PricedServiceOrder(serviceOrder, normalizedPrice));
            totalOriginalAmount = totalOriginalAmount.add(normalizedPrice);
        }

        BigDecimal targetInvoiceAmount = resolveTargetInvoiceAmount(invoice, totalOriginalAmount);
        BigDecimal remainingAmount = targetInvoiceAmount;

        int lineNumber = 1;
        for (int index = 0; index < pricedOrders.size(); index++) {
            PricedServiceOrder pricedOrder = pricedOrders.get(index);
            ServiceOrder serviceOrder = pricedOrder.serviceOrder();

            BigDecimal customerPrice = allocateCustomerPrice(
                    pricedOrder.originalPrice(),
                    totalOriginalAmount,
                    targetInvoiceAmount,
                    remainingAmount,
                    index == pricedOrders.size() - 1
            );
            remainingAmount = remainingAmount.subtract(customerPrice);

            items.add(new CreateInvoiceItem(
                    lineNumber++,
                    1,
                    serviceOrder.getService().getServiceId(),
                    serviceOrder.getService().getServiceName(),
                    itemUnit,
                    BigDecimal.ONE,
                    customerPrice,
                    requiresTaxRate ? defaultTaxRate : null
            ));
        }

        if (items.isEmpty()) {
            throw new RuntimeException("SePay invoice request has no valid items.");
        }

        return items;
    }

    private BigDecimal allocateCustomerPrice(BigDecimal originalPrice,
                                             BigDecimal totalOriginalAmount,
                                             BigDecimal targetInvoiceAmount,
                                             BigDecimal remainingAmount,
                                             boolean isLastItem) {
        if (targetInvoiceAmount.compareTo(BigDecimal.ZERO) <= 0 || totalOriginalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (isLastItem) {
            return nonNegativeAmount(remainingAmount);
        }

        BigDecimal proportionalAmount = targetInvoiceAmount
                .multiply(originalPrice)
                .divide(totalOriginalAmount, 0, RoundingMode.DOWN);
        return nonNegativeAmount(proportionalAmount);
    }

    private BigDecimal resolveTargetInvoiceAmount(Invoice invoice, BigDecimal totalOriginalAmount) {
        BigDecimal customerPaid = normalizeNullableCurrencyAmount(invoice.getCustomerPaid());
        if (customerPaid != null) {
            if (totalOriginalAmount.compareTo(BigDecimal.ZERO) > 0 && customerPaid.compareTo(totalOriginalAmount) > 0) {
                return totalOriginalAmount;
            }
            return nonNegativeAmount(customerPaid);
        }

        BigDecimal insurancePaid = normalizeNullableCurrencyAmount(invoice.getInsurancePaid());
        if (insurancePaid != null && totalOriginalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return nonNegativeAmount(totalOriginalAmount.subtract(insurancePaid));
        }

        return nonNegativeAmount(totalOriginalAmount);
    }

    private BigDecimal normalizeCurrencyAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullableCurrencyAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegativeAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : BigDecimal.ZERO;
    }

    private boolean requiresTaxRate(ProviderTemplate providerTemplate) {
        String templateCode = providerTemplate.templateCode();
        String invoiceLabel = providerTemplate.invoiceLabel();
        return "1".equals(templateCode)
                || (invoiceLabel != null && invoiceLabel.toLowerCase().contains("giá trị gia tăng"));
    }

    private String buildOrderNumber(Invoice invoice) {
        String appointmentId = invoice.getMedicalRecord() != null && invoice.getMedicalRecord().getAppointment() != null
                ? invoice.getMedicalRecord().getAppointment().getAppointmentId()
                : null;
        return appointmentId != null && !appointmentId.isBlank() ? appointmentId : null;
    }

    private String buildNotes(Invoice invoice) {
        return "Internal invoiceId=" + invoice.getInvoiceId();
    }

    private String submitCreateInvoice(String accessToken, CreateInvoiceRequest request) {
        JsonNode rootNode = exchangeForJson(
                HttpMethod.POST,
                "/v1/invoices/create",
            authorizedJsonEntity(accessToken, request)
        );
        ensureSuccess(rootNode, "SePay create invoice request failed.");

        String trackingCode = textValue(rootNode.path("data"), "tracking_code");
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new RuntimeException("SePay create invoice response does not contain tracking_code.");
        }

        return trackingCode;
    }

    private CreateCheckResult pollCreateStatus(String accessToken, String trackingCode) {
        for (int attempt = 1; attempt <= pollMaxAttempts; attempt++) {
            JsonNode rootNode = exchangeForJson(
                    HttpMethod.GET,
                    "/v1/invoices/create/check/" + trackingCode,
                    authorizedJsonEntity(accessToken, null)
            );
            ensureSuccess(rootNode, "SePay create invoice status check failed.");

            JsonNode dataNode = rootNode.path("data");
            String status = textValue(dataNode, "status");
            String message = textValue(dataNode, "message");
            JsonNode invoiceNode = dataNode.path("invoice");

            if ("Success".equalsIgnoreCase(status)) {
                return new CreateCheckResult(
                        textValue(dataNode, "reference_code"),
                        textValue(invoiceNode, "reference_code"),
                        textValue(invoiceNode, "pdf_url"),
                        textValue(invoiceNode, "xml_url"),
                        message
                );
            }

            if ("Failed".equalsIgnoreCase(status)) {
                throw new RuntimeException(message != null && !message.isBlank()
                        ? message
                        : "SePay invoice issuance failed.");
            }

            if (attempt < pollMaxAttempts) {
                sleepBeforeNextPoll();
            }
        }

        throw new RuntimeException("Timed out while waiting for SePay invoice issuance result.");
    }

    private InvoiceDetail fetchInvoiceDetail(String accessToken, String referenceCode) {
        JsonNode rootNode = exchangeForJson(
                HttpMethod.GET,
                "/v1/invoices/" + referenceCode,
                authorizedJsonEntity(accessToken, null)
        );
        ensureSuccess(rootNode, "SePay invoice detail request failed.");

        JsonNode dataNode = rootNode.path("data");
        return new InvoiceDetail(
                textValue(dataNode, "reference_code"),
                textValue(dataNode, "status"),
                textValue(dataNode, "pdf_url"),
                textValue(dataNode, "xml_url")
        );
    }

    private JsonNode exchangeForJson(HttpMethod method, String path, HttpEntity<?> requestEntity) {
        String url = buildUrl(path);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("SePay returned an empty response.");
            }
            return objectMapper.readTree(body);
        } catch (RestClientException exception) {
            throw new RuntimeException("Cannot connect to SePay sandbox: " + exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new RuntimeException("Cannot parse SePay response: " + exception.getMessage(), exception);
        }
    }

    private HttpEntity<?> authorizedJsonEntity(String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        return new HttpEntity<>(body, headers);
    }

    private void ensureSuccess(JsonNode rootNode, String defaultMessage) {
        if (rootNode.path("success").asBoolean(false)) {
            return;
        }

        String message = textValue(rootNode.path("error"), "message");
        String code = textValue(rootNode.path("error"), "code");
        if (message == null || message.isBlank()) {
            message = defaultMessage;
        }
        if (code != null && !code.isBlank()) {
            message = code + ": " + message;
        }

        throw new RuntimeException(message);
    }

    private void validateAuthenticationConfig() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new RuntimeException("SePay sandbox credentials are missing. Configure sepay.einvoice.client-id and sepay.einvoice.client-secret.");
        }
    }

    private String mapPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "KHAC";
        }

        if ("CASH".equalsIgnoreCase(paymentMethod)) {
            return "TM";
        }
        if ("QR".equalsIgnoreCase(paymentMethod)
                || "QR_SEPAY".equalsIgnoreCase(paymentMethod)
                || "BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
            return "CK";
        }

        return "KHAC";
    }

    private void sleepBeforeNextPoll() {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(1L, pollIntervalMillis));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for SePay invoice status.", interruptedException);
        }
    }

    private String buildUrl(String path) {
        String normalizedBaseUrl = baseUrl != null && !baseUrl.isBlank()
                ? baseUrl.replaceAll("/+$", "")
                : "https://einvoice-api-sandbox.sepay.vn";
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return value != null && !value.isBlank() ? value : null;
    }

    private long longValue(JsonNode node, String fieldName, long defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asLong(defaultValue);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }

    public record IssuedEinvoice(String referenceCode,
                                 String pdfUrl,
                                 String xmlUrl,
                                 String trackingCode) {
    }

    private record ProviderTemplate(String providerAccountId,
                                    String templateCode,
                                    String invoiceSeries,
                                    String invoiceLabel) {
    }

    private record CreateCheckResult(String referenceCode,
                                     String invoiceReferenceCode,
                                     String pdfUrl,
                                     String xmlUrl,
                                     String message) {
    }

    private record InvoiceDetail(String referenceCode,
                                 String status,
                                 String pdfUrl,
                                 String xmlUrl) {
    }

    private record PricedServiceOrder(ServiceOrder serviceOrder,
                                      BigDecimal originalPrice) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CreateInvoiceRequest(
            @JsonProperty("template_code") String templateCode,
            @JsonProperty("invoice_series") String invoiceSeries,
            @JsonProperty("issued_date") String issuedDate,
            String currency,
            @JsonProperty("provider_account_id") String providerAccountId,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("is_draft") boolean isDraft,
            Buyer buyer,
            List<CreateInvoiceItem> items,
            @JsonProperty("order_number") String orderNumber,
            String notes) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Buyer(
            String type,
            String name,
            @JsonProperty("legal_name") String legalName,
            @JsonProperty("tax_code") String taxCode,
            String address,
            String email,
            String phone,
            @JsonProperty("buyer_code") String buyerCode,
            @JsonProperty("national_id") String nationalId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CreateInvoiceItem(
            @JsonProperty("line_number") int lineNumber,
            @JsonProperty("line_type") int lineType,
            @JsonProperty("item_code") String itemCode,
            @JsonProperty("item_name") String itemName,
            String unit,
            BigDecimal quantity,
            @JsonProperty("unit_price") BigDecimal unitPrice,
            @JsonProperty("tax_rate") BigDecimal taxRate) {
    }
}