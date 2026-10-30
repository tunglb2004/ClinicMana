# Lộ trình học Automation Test — thực hành trên dự án Clinic Management

> Tài liệu dành cho người mới bắt đầu, viết riêng cho dự án này:
> **Backend** Spring Boot 4 / Java 17 (`BE/`) + **Frontend** Next.js 16 / React 19 / TypeScript (`clinic-management-fe/`).

---

## 0. Lời mở đầu từ giảng viên

Sau 10 năm làm automation, điều tôi thấy người mới hay sai nhất **không phải là thiếu tool**, mà là:

1. **Nhảy thẳng vào Selenium/Playwright** để tự động hóa giao diện, trong khi chưa hiểu test case là gì, test cái gì, ở tầng nào.
2. **Viết script chứ không viết test** — chạy được một lần, lần sau đổi dữ liệu là vỡ.
3. **Không có assertion rõ ràng** — script "chạy qua" nhưng không kiểm chứng điều gì.

Dự án của em có một lợi thế rất lớn: **đã có sẵn tài liệu test thủ công** (`Report 5 _ TestCase_Document.xlsx`, `UnitTest.xlsx`, `SystemTest`, `IntegrationTest`) và **đã có ~30 file unit test** trong `BE/src/test`. Tức là em không học trên giấy — em sẽ biến chính các test case thủ công của nhóm thành test tự động.

**Nguyên tắc học của khóa này:** *Hiểu → Đọc code có sẵn → Sửa nhỏ → Viết mới → Tự động chạy trên CI.*

---

## 1. Kiến thức nền tảng bắt buộc

### 1.1 Automation test là gì, KHÔNG phải là gì

| Là | Không phải |
|---|---|
| Code kiểm tra code, chạy lặp lại được, cho kết quả Pass/Fail rõ ràng | Thay thế hoàn toàn tester thủ công |
| Lưới an toàn khi sửa code (regression) | Công cụ tìm bug mới hiệu quả nhất (exploratory test thủ công giỏi hơn) |
| Tài liệu sống mô tả hệ thống hoạt động thế nào | Thứ viết một lần rồi bỏ đó |

**Khi nào nên tự động hóa?** Test chạy lặp lại nhiều lần (regression, smoke), luồng nghiệp vụ quan trọng (đăng nhập, đặt lịch khám, thanh toán), dữ liệu nhiều tổ hợp.
**Khi nào không nên?** Tính năng còn thay đổi liên tục, kiểm tra cảm quan UI, test chỉ chạy 1 lần.

### 1.2 Kim tự tháp kiểm thử (Test Pyramid) — khái niệm quan trọng nhất

```
            /\
           /E2E\         ← ít (5–10%): Playwright mở trình duyệt, chạy luồng thật
          /------\          chậm, dễ "flaky", đắt để bảo trì
         /  API /  \
        / Integration\   ← vừa (20–30%): gọi REST API, MockMvc, DB H2
       /--------------\
      /   Unit Test    \ ← nhiều (60–70%): JUnit + Mockito, test từng hàm/class
     /------------------\   nhanh (ms), ổn định, rẻ
```

Ánh xạ vào dự án:

| Tầng | Trong dự án | Công cụ | Trạng thái hiện tại |
|---|---|---|---|
| Unit | `BE/src/test/java/g2/be/service/*`, `controller/*` | JUnit 5, Mockito, AssertJ | ✅ Đã có ~30 file |
| Integration | `BE/src/test/java/g2/be/integration/*` | `@SpringBootTest` + H2 | ⚠️ Có 1 file |
| API | Các endpoint `/api/...` (ví dụ `/api/auth/login`) | MockMvc / REST Assured / Postman + Newman | ⚠️ Chỉ có script `test-e2e-flow.js` (chưa có assertion chuẩn) |
| E2E UI | `clinic-management-fe` (trang `/login`, `/dashboard`…) | **Playwright** (TypeScript) | ❌ Chưa có |

### 1.3 Thuật ngữ phải thuộc

- **Test case**: điều kiện tiền đề + bước + dữ liệu + **kết quả mong đợi**.
- **Test suite**: nhóm các test case.
- **Assertion**: câu lệnh so sánh thực tế với mong đợi (`assertEquals`, `expect(...).toBe(...)`). *Không có assertion = không phải test.*
- **AAA pattern**: **A**rrange (chuẩn bị) → **A**ct (hành động) → **A**ssert (kiểm tra). Mọi test đều nên theo cấu trúc này.
- **Mock / Stub**: đối tượng giả thay cho phụ thuộc thật (DB, email, payment) để test cô lập.
- **Fixture / Test data**: dữ liệu chuẩn bị sẵn cho test.
- **Flaky test**: test lúc pass lúc fail mà code không đổi — kẻ thù số 1 của automation.
- **Regression test**: chạy lại test cũ để chắc chắn code mới không làm hỏng chức năng cũ.
- **Smoke test**: bộ test nhỏ, nhanh, kiểm tra chức năng sống còn (login, mở dashboard).
- **CI (Continuous Integration)**: tự động chạy test mỗi khi push code (GitHub Actions).
- **Coverage**: % dòng code được test chạy qua (JaCoCo). Là chỉ số tham khảo, *không phải mục tiêu*.

### 1.4 Kỹ thuật thiết kế test case (dùng cho mọi tầng)

Automation chỉ giỏi bằng test case em thiết kế. Bốn kỹ thuật tối thiểu:

1. **Phân vùng tương đương (Equivalence Partitioning)** — chia input thành nhóm hành xử giống nhau, mỗi nhóm lấy 1 đại diện.
   *Ví dụ login:* identifier là email hợp lệ / số điện thoại hợp lệ / chuỗi rỗng / định dạng sai.
2. **Phân tích giá trị biên (Boundary Value Analysis)** — test tại biên: min-1, min, max, max+1.
   *Ví dụ:* mật khẩu tối thiểu 6 ký tự → test 5, 6, 7 ký tự.
3. **Bảng quyết định (Decision Table)** — khi nhiều điều kiện kết hợp.
   *Ví dụ:* thanh toán = (có BHYT? ) × (đã thanh toán trước? ) × (phương thức: tiền mặt/chuyển khoản).
4. **Chuyển trạng thái (State Transition)** — đối tượng có vòng đời.
   *Ví dụ:* Lịch hẹn: `PENDING → CONFIRMED → CHECKED_IN → IN_EXAMINATION → COMPLETED`, hoặc `→ CANCELLED`. Test cả chuyển hợp lệ **và không hợp lệ** (COMPLETED → PENDING phải bị chặn).

> 👉 Bài tập đầu tiên: mở `Report 5 _ TestCase_Document.xlsx`, chọn 10 test case, ghi chú mỗi case thuộc kỹ thuật nào và nên tự động ở tầng nào (Unit/API/UI).

---

## 2. Cài đặt môi trường

### 2.1 Phần mềm cần cài

| Công cụ | Phiên bản | Dùng để | Kiểm tra |
|---|---|---|---|
| JDK | 17 (Temurin) | Chạy BE + JUnit | `java -version` |
| Maven | dùng `mvnw` có sẵn trong `BE/` | Build + chạy test Java | `./mvnw -v` |
| Node.js | 20 LTS hoặc 22 LTS | Chạy FE + Playwright | `node -v` |
| Git | mới nhất | Quản lý code | `git --version` |
| IDE Java | IntelliJ IDEA Community | Viết/chạy/debug JUnit | — |
| IDE TS | VS Code + extension **Playwright Test for VSCode** | Viết/chạy Playwright | — |
| Postman | mới nhất | Khám phá API thủ công trước khi tự động hóa | — |

Extension VS Code nên có: *Playwright Test for VSCode*, *Extension Pack for Java*, *ESLint*, *GitLens*.

### 2.2 Chạy được dự án trước khi test

Không chạy được app thì không test E2E được. Thứ tự:

```bash
# Backend (dùng profile H2 để không đụng DB thật)
cd BE
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
# Swagger UI: http://localhost:8080/swagger-ui.html  ← bản đồ API, hãy mở ra xem
```

```bash
# Frontend
cd clinic-management-fe
npm install
npm run dev        # http://localhost:3000
```

### 2.3 ⚠️ Cảnh báo an toàn — đọc kỹ

- `BE/src/main/resources/application.properties` đang chứa **thông tin kết nối DB thật trên AWS RDS** (host, user, password). **Tuyệt đối không chạy test tự động trỏ vào DB này** — test sẽ tạo/xóa dữ liệu thật. Luôn dùng H2 (profile `h2`) hoặc DB test riêng.
- Nên chuyển mật khẩu sang biến môi trường (`${DB_PASSWORD}`) và đổi mật khẩu DB, vì nó đã nằm trong lịch sử git.
- `BE/src/test/java/g2/be/HashCracker.java` không phải test — nên xóa khỏi thư mục test.
- File `test_output*.txt`, `*.log`, `test-results/` nên đưa vào `.gitignore`.

---

## 3. Lộ trình 8 tuần

| Tuần | Chủ đề | Đầu ra (deliverable) |
|---|---|---|
| 1 | Nền tảng: mục 1, đọc test có sẵn, chạy `mvnw test` | Báo cáo: bao nhiêu test pass/fail, vì sao fail |
| 2 | Unit test với JUnit 5 + Mockito | Viết thêm 10 unit test cho 1 service chưa có test |
| 3 | Kỹ thuật nâng cao: `@ParameterizedTest`, AssertJ, JaCoCo coverage | Báo cáo coverage, 1 test tham số hóa |
| 4 | API test: MockMvc + `@SpringBootTest` + H2 | Bộ test API cho `/api/auth/*` |
| 5 | Playwright cơ bản: locator, assertion, codegen | Test login thành công / thất bại |
| 6 | Playwright nâng cao: Page Object Model, fixture, đăng nhập qua API | Test luồng đặt lịch khám |
| 7 | Test data, độc lập giữa các test, xử lý flaky | Bộ test chạy song song ổn định 10 lần liên tiếp |
| 8 | CI/CD với GitHub Actions + báo cáo | Pipeline tự chạy test khi push |

---

## 4. Tầng 1 — Unit Test (JUnit 5 + Mockito)

### 4.1 Đọc hiểu test có sẵn

Mở `BE/src/test/java/g2/be/controller/AuthControllerTest.java`. Nhận diện:

```java
@ExtendWith(MockitoExtension.class)   // bật Mockito cho JUnit 5
class AuthControllerTest {
    @Mock private AuthService authService;          // phụ thuộc giả
    @InjectMocks private AuthController authController; // class được test, mock được tiêm vào
    ...
}
```

### 4.2 Chạy test

```bash
cd BE
./mvnw test                                   # chạy tất cả
./mvnw test -Dtest=AuthServiceTest            # chạy 1 class
./mvnw test -Dtest=AuthServiceTest#login*     # chạy theo tên method
```

Kết quả nằm ở `BE/target/surefire-reports/`.

### 4.3 Mẫu một unit test chuẩn (AAA)

```java
@ExtendWith(MockitoExtension.class)
class RoomServiceExampleTest {

    @Mock RoomRepository roomRepository;
    @InjectMocks RoomService roomService;

    @Test
    @DisplayName("Lấy phòng không tồn tại thì ném exception")
    void getRoom_notFound_throwsException() {
        // Arrange
        when(roomRepository.findById("R999")).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RuntimeException.class, () -> roomService.getRoomById("R999"));
        verify(roomRepository).findById("R999");   // kiểm tra tương tác
    }
}
```

> Tên method thực tế có thể khác — hãy mở `RoomService` để xem. Mục tiêu là học **cấu trúc**, không copy nguyên.

### 4.4 Những thứ cần thành thạo

- `@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName`, `@Nested`, `@Disabled`
- `@ParameterizedTest` + `@CsvSource` — 1 test chạy nhiều bộ dữ liệu (rất hợp với BVA):
  ```java
  @ParameterizedTest
  @CsvSource({"12345,false", "123456,true", "1234567,true"})
  void passwordLength(String pwd, boolean expectedValid) { ... }
  ```
- Mockito: `when(...).thenReturn(...)`, `thenThrow`, `verify(mock, times(1))`, `never()`, `ArgumentCaptor`
- AssertJ (đọc dễ hơn): `assertThat(list).hasSize(3).extracting("name").contains("Khoa Nội");`

### 4.5 Bài tập
1. Chạy `./mvnw test`, ghi lại test nào fail và tìm nguyên nhân.
2. Liệt kê các service trong `BE/src/main/java/g2/be/service` **chưa có** file test tương ứng. Chọn 1, viết 5–10 test (happy path + lỗi + biên).
3. Thêm JaCoCo vào `pom.xml`, xem báo cáo coverage ở `target/site/jacoco/index.html`.

---

## 5. Tầng 2 — API / Integration Test

### 5.1 Bước 0: khám phá API bằng Postman
Trước khi tự động hóa, gọi tay bằng Postman:

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{ "identifier": "<email hoặc SĐT>", "password": "<mật khẩu>" }
```
Ghi lại: status code, cấu trúc JSON trả về (`accessToken`…), trường hợp sai mật khẩu trả về gì.
Các endpoint auth khác: `/register/send-otp`, `/register`, `/refresh-token`, `/logout`, `/me`, `/forgot-password`, `/reset-password`.

### 5.2 Test API bằng MockMvc (cùng ngôn ngữ Java, chạy trong `mvnw test`)

Dựa trên mẫu đã có ở `integration/ClinicStructureIntegrationTest.java` (dùng H2 in-memory):

```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:api-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
    // + các property cloudinary/sepay như file integration có sẵn
})
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired MockMvc mockMvc;

    @Test
    void login_wrongPassword_returns4xx() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"identifier":"admin@test.com","password":"sai-mat-khau"}
                """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }
}
```

> Spring Boot 4 đổi package của một số annotation test (`@AutoConfigureMockMvc`, `@MockitoBean` thay cho `@MockBean`). Hãy để IntelliJ tự gợi ý import.

### 5.3 Checklist khi test một API
- [ ] Status code đúng (200/201/400/401/403/404/409)
- [ ] Body đúng cấu trúc và giá trị (`jsonPath("$.accessToken").exists()`)
- [ ] Validation: thiếu trường, sai định dạng, quá dài
- [ ] **Phân quyền**: Patient gọi API của Doctor/Cashier phải bị 403 — dự án có nhiều vai trò (Patient, Receptionist, Cashier, Doctor, Admin) nên đây là vùng dễ có bug
- [ ] Dữ liệu trong DB thay đổi đúng sau khi gọi

### 5.4 Nâng cấp `test-e2e-flow.js`
File này đang là **script**, chưa phải **test**: nó `console.log` thay vì assert, phụ thuộc tài khoản thật, chạy dừng ở lỗi đầu tiên. Bài tập tuần 4: viết lại luồng này thành test Playwright API (mục 6.6) với assertion cho từng bước.

---

## 6. Tầng 3 — E2E UI Test với Playwright

### 6.1 Vì sao chọn Playwright (không phải Selenium)?
- Cùng TypeScript với FE → một ngôn ngữ cho cả team.
- **Auto-wait**: tự đợi phần tử sẵn sàng → ít flaky hơn Selenium rất nhiều.
- Có sẵn: chạy song song, trace viewer, quay video, screenshot, test API, codegen.

### 6.2 Cài đặt (trong thư mục FE)

```bash
cd clinic-management-fe
npm init playwright@latest
```
Chọn: TypeScript → thư mục `e2e` → thêm GitHub Actions workflow: Yes → cài browsers: Yes.

Sửa `playwright.config.ts`:

```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: [['html'], ['list']],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
```

Thêm vào `package.json`:
```json
"test:e2e": "playwright test",
"test:e2e:ui": "playwright test --ui"
```

### 6.3 Test đầu tiên — đăng nhập (dựa trên `src/components/feature/auth/LoginForm.tsx`)

`e2e/login.spec.ts`:

```ts
import { test, expect } from '@playwright/test';

test.describe('Đăng nhập', () => {
  test('sai mật khẩu thì hiển thị lỗi và ở lại trang login', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email hoặc số điện thoại').fill('nguoidung@test.com');
    await page.getByPlaceholder('Nhập mật khẩu').fill('sai-mat-khau');
    await page.getByRole('button', { name: 'Đăng nhập' }).click();

    await expect(page).toHaveURL(/\/login/);
    // TODO: assert thông báo lỗi hiển thị (toast) — tự tìm locator phù hợp
  });

  test('đăng nhập đúng thì vào dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email hoặc số điện thoại').fill(process.env.E2E_USER!);
    await page.getByPlaceholder('Nhập mật khẩu').fill(process.env.E2E_PASS!);
    await page.getByRole('button', { name: 'Đăng nhập' }).click();

    await expect(page).toHaveURL(/\/dashboard/);
  });
});
```

Chạy:
```bash
npx playwright test            # chạy headless
npx playwright test --ui       # giao diện trực quan, rất nên dùng khi học
npx playwright show-report     # xem báo cáo HTML
npx playwright codegen http://localhost:3000/login   # ghi lại thao tác thành code
```

> **Không hard-code tài khoản/mật khẩu trong code test.** Dùng biến môi trường (`.env` + thư viện `dotenv`, và đưa `.env` vào `.gitignore`).

### 6.4 Locator — kỹ năng quan trọng nhất của UI automation

Thứ tự ưu tiên (từ tốt đến kém):

1. `getByRole('button', { name: 'Đăng nhập' })` — giống cách người dùng thấy, bền nhất
2. `getByLabel('Mật khẩu')`, `getByPlaceholder(...)`, `getByText(...)`
3. `getByTestId('login-submit')` — cần dev thêm `data-testid="login-submit"` vào component
4. CSS `page.locator('#identifier')`
5. ❌ XPath dài kiểu `//div[3]/form/div[2]/button` — vỡ ngay khi UI đổi

> Gợi ý: thống nhất với team FE thêm `data-testid` cho các phần tử quan trọng (nút submit, bảng dữ liệu, modal).

### 6.5 Page Object Model (POM)

Khi có nhiều test, gom locator + hành động của mỗi trang vào một class:

```ts
// e2e/pages/LoginPage.ts
import { Page, expect } from '@playwright/test';

export class LoginPage {
  constructor(private page: Page) {}
  identifier = () => this.page.getByPlaceholder('Email hoặc số điện thoại');
  password   = () => this.page.getByPlaceholder('Nhập mật khẩu');
  submit     = () => this.page.getByRole('button', { name: 'Đăng nhập' });

  async goto() { await this.page.goto('/login'); }
  async login(user: string, pass: string) {
    await this.identifier().fill(user);
    await this.password().fill(pass);
    await this.submit().click();
  }
}
```

UI đổi → chỉ sửa 1 chỗ thay vì 50 test.

### 6.6 Kỹ thuật nâng cao (tuần 6–7)
- **Đăng nhập 1 lần, dùng lại `storageState`** cho mọi test (không login qua UI mỗi test → nhanh gấp nhiều lần).
- **Chuẩn bị dữ liệu qua API**, chỉ dùng UI cho phần đang cần test. Ví dụ test "Bác sĩ xem bệnh nhân trong hàng đợi": tạo lịch hẹn bằng `request.post('/api/...')`, rồi mới mở UI bác sĩ.
- **Fixtures** (`test.extend`) để tiêm `loginPage`, `apiClient`… vào test.
- **Mỗi test độc lập**: tự tạo dữ liệu riêng (tên có hậu tố ngẫu nhiên như file integration có sẵn đang làm với `UUID`), không phụ thuộc thứ tự chạy.
- **Không dùng `page.waitForTimeout(3000)`** — dùng `expect(...).toBeVisible()` để Playwright tự đợi.

### 6.7 Luồng E2E nên ưu tiên tự động hóa (theo nghiệp vụ phòng khám)
1. Đăng nhập theo từng vai trò → đúng dashboard
2. Bệnh nhân đặt lịch khám
3. Lễ tân check-in → bệnh nhân vào hàng đợi (`/queue-display`)
4. Bác sĩ khám → tạo hồ sơ bệnh án, chỉ định dịch vụ, kê đơn
5. Thu ngân thanh toán → xuất hóa đơn
6. Truy cập trái quyền → `/unauthorized`

---

## 7. Tích hợp CI — GitHub Actions

`.github/workflows/tests.yml` (khung tham khảo):

```yaml
name: Tests
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17', cache: maven }
      - run: cd BE && ./mvnw -B test
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20 }
      - run: cd clinic-management-fe && npm ci && npx playwright install --with-deps
      # khởi động BE (profile h2) ở background trước khi chạy E2E
      - run: cd clinic-management-fe && npx playwright test
      - uses: actions/upload-artifact@v4
        if: always()
        with: { name: playwright-report, path: clinic-management-fe/playwright-report }
```

Mật khẩu/tài khoản test đặt trong **GitHub Secrets**, không commit.

---

## 8. Những sai lầm kinh điển (tôi đã thấy hàng trăm lần)

| Sai lầm | Hậu quả | Cách đúng |
|---|---|---|
| `sleep`/`waitForTimeout` cố định | Chậm và vẫn flaky | Dùng auto-wait / `expect` |
| Test phụ thuộc nhau (test 2 cần test 1 chạy trước) | Fail dây chuyền, không chạy song song được | Mỗi test tự chuẩn bị dữ liệu |
| Dùng DB thật/production | Phá dữ liệu thật | H2 / DB test riêng |
| Một test kiểm tra 10 thứ | Fail không biết lỗi ở đâu | 1 test = 1 hành vi |
| Locator XPath dài | Vỡ khi UI đổi | `getByRole`, `data-testid` |
| Test fail thì `@Disabled` cho qua | Mất lưới an toàn | Sửa hoặc xóa có lý do |
| Tự động hóa mọi thứ qua UI | Bộ test chậm, mong manh | Đẩy xuống tầng API/Unit khi được |
| Hard-code mật khẩu trong code | Lộ thông tin | Biến môi trường / Secrets |

---

## 9. Checklist tự đánh giá

**Mức 1 — Cơ bản**
- [ ] Giải thích được Test Pyramid và AAA
- [ ] Chạy được `./mvnw test` và đọc kết quả
- [ ] Viết được unit test với Mockito (mock, verify)
- [ ] Thiết kế test case bằng EP + BVA

**Mức 2 — Trung cấp**
- [ ] Viết test API với MockMvc, kiểm tra status + JSON + phân quyền
- [ ] Viết test Playwright với locator chuẩn, có assertion
- [ ] Áp dụng Page Object Model
- [ ] Debug test fail bằng Trace Viewer

**Mức 3 — Sẵn sàng đi làm**
- [ ] Bộ test chạy song song, ổn định, độc lập dữ liệu
- [ ] Chuẩn bị dữ liệu qua API, login bằng `storageState`
- [ ] Pipeline CI chạy tự động, có báo cáo
- [ ] Biết chọn tầng phù hợp cho từng test case

---

## 10. Tài liệu tham khảo

- JUnit 5 User Guide — https://junit.org/junit5/docs/current/user-guide/
- Mockito — https://site.mockito.org/
- AssertJ — https://assertj.github.io/doc/
- Spring Boot Testing — https://docs.spring.io/spring-boot/reference/testing/
- Playwright — https://playwright.dev/docs/intro (đặc biệt: *Locators*, *Best Practices*, *Authentication*, *Page Object Models*)
- Sách: *"The Art of Unit Testing"* (Roy Osherove), *"Unit Testing Principles, Practices, and Patterns"* (Vladimir Khorikov)
- Chứng chỉ (tùy chọn): ISTQB Foundation Level — hệ thống hóa lý thuyết kiểm thử

---

> **Lời khuyên cuối:** Mỗi ngày viết ít nhất 1 test và đọc lại 1 test của người khác. Sau 8 tuần, em sẽ có một portfolio thật — bộ test tự động cho một hệ thống quản lý phòng khám hoàn chỉnh — để đưa vào CV.
