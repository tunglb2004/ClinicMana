# OTP System Architecture & Flow Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    OTP Verification System                   │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│              │      │              │      │              │
│   Client     │◄────►│    Spring    │◄────►│   MySQL      │
│  (Browser/   │ HTTP │    Boot      │ JPA  │  Database    │
│   Mobile)    │      │   Backend    │      │              │
│              │      │              │      │              │
└──────────────┘      └──────────────┘      └──────────────┘
                            │
                            │ [Future]
                            ▼
                      ┌──────────────┐
                      │    Email     │
                      │   Service    │
                      │ (SMTP/SES)   │
                      └──────────────┘
```

## Component Structure

```
g2.be
├── entity
│   └── VerificationToken.java          [JPA Entity]
│       ├── id: Long
│       ├── email: String
│       ├── otpCode: String
│       ├── expiryDate: LocalDateTime
│       ├── type: TokenType (Enum)
│       ├── isUsed: Boolean
│       └── createdAt: LocalDateTime
│
├── repository
│   └── VerificationTokenRepository.java [JPA Repository]
│       ├── findByEmailAndOtpCodeAndTypeAndIsUsed()
│       ├── findByEmailAndType()
│       ├── deleteExpiredTokens()
│       └── deleteByEmailAndType()
│
├── service
│   └── VerificationTokenService.java   [Business Logic]
│       ├── generateOtpCode()
│       ├── createOtpToken()
│       ├── verifyOtp()
│       ├── markAsUsed()
│       ├── verifyAndMarkAsUsed()
│       └── deleteExpiredTokens()
│
├── controller
│   └── OtpController.java              [REST API]
│       ├── POST /api/otp/send
│       ├── POST /api/otp/verify
│       └── POST /api/otp/resend
│
└── dto
    ├── request
    │   ├── SendOtpRequest.java
    │   └── VerifyOtpRequest.java
    └── response
        └── OtpResponse.java
```

## Flow Diagram: Send OTP

```
┌────────┐                ┌────────────┐              ┌─────────┐              ┌──────────┐
│ Client │                │   OTP      │              │   OTP   │              │ Database │
│        │                │ Controller │              │ Service │              │          │
└────┬───┘                └─────┬──────┘              └────┬────┘              └────┬─────┘
     │                          │                          │                        │
     │  POST /api/otp/send      │                          │                        │
     │  {email, type}           │                          │                        │
     ├─────────────────────────►│                          │                        │
     │                          │                          │                        │
     │                          │  createOtpToken()        │                        │
     │                          │  (email, type)           │                        │
     │                          ├─────────────────────────►│                        │
     │                          │                          │                        │
     │                          │                          │  generateOtpCode()     │
     │                          │                          │  → "123456"            │
     │                          │                          │                        │
     │                          │                          │  deleteByEmailAndType()│
     │                          │                          ├───────────────────────►│
     │                          │                          │                        │
     │                          │                          │  save(token)           │
     │                          │                          ├───────────────────────►│
     │                          │                          │                        │
     │                          │                          │◄───────────────────────┤
     │                          │  VerificationToken       │  Token saved           │
     │                          │◄─────────────────────────┤                        │
     │                          │                          │                        │
     │                          │  [TODO] Send Email       │                        │
     │                          │  emailService.send()     │                        │
     │                          │                          │                        │
     │                          │  Print to Console        │                        │
     │                          │  (Development)           │                        │
     │                          │                          │                        │
     │  200 OK                  │                          │                        │
     │  OtpResponse             │                          │                        │
     │◄─────────────────────────┤                          │                        │
     │  {message, email,        │                          │                        │
     │   expiresInMinutes: 5}   │                          │                        │
     │                          │                          │                        │
```

## Flow Diagram: Verify OTP

```
┌────────┐                ┌────────────┐              ┌─────────┐              ┌──────────┐
│ Client │                │   OTP      │              │   OTP   │              │ Database │
│        │                │ Controller │              │ Service │              │          │
└────┬───┘                └─────┬──────┘              └────┬────┘              └────┬─────┘
     │                          │                          │                        │
     │  POST /api/otp/verify    │                          │                        │
     │  {email, otpCode, type}  │                          │                        │
     ├─────────────────────────►│                          │                        │
     │                          │                          │                        │
     │                          │  verifyAndMarkAsUsed()   │                        │
     │                          ├─────────────────────────►│                        │
     │                          │                          │                        │
     │                          │                          │  verifyOtp()           │
     │                          │                          │  findByEmailAndOtp...()│
     │                          │                          ├───────────────────────►│
     │                          │                          │                        │
     │                          │                          │◄───────────────────────┤
     │                          │                          │  Optional<Token>       │
     │                          │                          │                        │
     │                          │                          │  Check:                │
     │                          │                          │  • Token exists?       │
     │                          │                          │  • Not used?           │
     │                          │                          │  • Not expired?        │
     │                          │                          │                        │
     │                          │                          │  If valid:             │
     │                          │                          │  markAsUsed()          │
     │                          │                          │  token.setIsUsed(true) │
     │                          │                          ├───────────────────────►│
     │                          │                          │                        │
     │                          │  VerificationToken       │                        │
     │                          │◄─────────────────────────┤                        │
     │                          │                          │                        │
     │  200 OK                  │                          │                        │
     │  MessageResponse         │                          │                        │
     │◄─────────────────────────┤                          │                        │
     │  {message: "Success"}    │                          │                        │
     │                          │                          │                        │
     │                          │  If invalid:             │                        │
     │  500 Error               │  throw RuntimeException  │                        │
     │◄─────────────────────────┤◄─────────────────────────┤                        │
     │  {error message}         │                          │                        │
     │                          │                          │                        │
```

## State Diagram: OTP Token Lifecycle

```
                    ┌──────────────┐
                    │   CREATED    │
                    │  is_used=0   │
                    │  expiry > now│
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │                         │
              │ User verifies           │ 5 minutes pass
              │ correctly               │
              ▼                         ▼
      ┌───────────────┐         ┌──────────────┐
      │     USED      │         │   EXPIRED    │
      │  is_used=1    │         │  expiry<now  │
      └───────────────┘         └──────────────┘
              │                         │
              │                         │
              └────────────┬────────────┘
                           │
                           │ Cleanup job runs
                           ▼
                    ┌──────────────┐
                    │   DELETED    │
                    │  (from DB)   │
                    └──────────────┘
```

## Sequence Diagram: Complete Registration Flow with OTP

```
User          Frontend        Backend          OTP Service      Database        Email
 │                │               │                 │              │             │
 │  Fill form     │               │                 │              │             │
 │───────────────►│               │                 │              │             │
 │                │               │                 │              │             │
 │  Click "Send   │               │                 │              │             │
 │   OTP" button  │               │                 │              │             │
 │───────────────►│               │                 │              │             │
 │                │               │                 │              │             │
 │                │ POST          │                 │              │             │
 │                │ /otp/send     │                 │              │             │
 │                ├──────────────►│                 │              │             │
 │                │               │                 │              │             │
 │                │               │ createOtpToken()│              │             │
 │                │               ├────────────────►│              │             │
 │                │               │                 │              │             │
 │                │               │                 │ Generate     │             │
 │                │               │                 │ "123456"     │             │
 │                │               │                 │              │             │
 │                │               │                 │ Save token   │             │
 │                │               │                 ├─────────────►│             │
 │                │               │                 │              │             │
 │                │               │                 │ [TODO]       │             │
 │                │               │                 │ Send email   │             │
 │                │               │                 ├─────────────────────────►  │
 │                │               │                 │              │             │
 │                │               │                 │              │    Email    │
 │◄───────────────────────────────────────────────────────────────────────────── │
 │  "OTP: 123456" │               │                 │              │             │
 │                │               │                 │              │             │
 │                │ Success       │                 │              │             │
 │                │ response      │                 │              │             │
 │                │◄──────────────┤                 │              │             │
 │                │               │                 │              │             │
 │  Show "OTP     │               │                 │              │             │
 │   sent" msg    │               │                 │              │             │
 │◄───────────────┤               │                 │              │             │
 │                │               │                 │              │             │
 │  Enter OTP     │               │                 │              │             │
 │  "123456"      │               │                 │              │             │
 │───────────────►│               │                 │              │             │
 │                │               │                 │              │             │
 │                │ POST          │                 │              │             │
 │                │ /otp/verify   │                 │              │             │
 │                ├──────────────►│                 │              │             │
 │                │               │                 │              │             │
 │                │               │ verifyOtp()     │              │             │
 │                │               ├────────────────►│              │             │
 │                │               │                 │              │             │
 │                │               │                 │ Query token  │             │
 │                │               │                 ├─────────────►│             │
 │                │               │                 │              │             │
 │                │               │                 │ Validate     │             │
 │                │               │                 │ & mark used  │             │
 │                │               │                 ├─────────────►│             │
 │                │               │                 │              │             │
 │                │               │ Token validated │              │             │
 │                │               │◄────────────────┤              │             │
 │                │               │                 │              │             │
 │                │ Success       │                 │              │             │
 │                │◄──────────────┤                 │              │             │
 │                │               │                 │              │             │
 │  Show success  │               │                 │              │             │
 │  & proceed to  │               │                 │              │             │
 │  complete reg. │               │                 │              │             │
 │◄───────────────┤               │                 │              │             │
 │                │               │                 │              │             │
```

## Database Schema

```sql
┌─────────────────────────────────────────────────────────┐
│              Table: verification_tokens                  │
├──────────────────┬──────────────┬─────────────┬─────────┤
│ Column           │ Type         │ Nullable    │ Key     │
├──────────────────┼──────────────┼─────────────┼─────────┤
│ id               │ BIGINT       │ NOT NULL    │ PRIMARY │
│ email            │ VARCHAR(100) │ NOT NULL    │         │
│ otp_code         │ VARCHAR(10)  │ NOT NULL    │         │
│ expiry_date      │ TIMESTAMP    │ NOT NULL    │         │
│ type             │ VARCHAR(20)  │ NOT NULL    │         │
│ is_used          │ BOOLEAN      │ DEFAULT 0   │         │
│ created_at       │ TIMESTAMP    │ DEFAULT NOW │         │
└──────────────────┴──────────────┴─────────────┴─────────┘

Indexes:
  - PRIMARY KEY (id)
  
Constraints:
  - type IN ('REGISTER', 'RESET_PASSWORD')
```

## Security Flow

```
┌────────────────────────────────────────────────┐
│           Security Measures                     │
└────────────────────────────────────────────────┘

1. Generation
   ├─ Use SecureRandom (cryptographically strong)
   ├─ 6 digits = 1,000,000 combinations
   └─ New OTP replaces old one

2. Storage
   ├─ Store with expiry timestamp
   ├─ Boolean flag for "used" status
   └─ [Future] Consider hashing OTP in DB

3. Validation
   ├─ Check email matches
   ├─ Check OTP matches
   ├─ Check type matches
   ├─ Check not used (is_used = false)
   ├─ Check not expired (expiry_date > now)
   └─ Mark as used after successful validation

4. Expiration
   ├─ 5 minutes validity window
   ├─ Automatic cleanup (scheduled task)
   └─ User can request new OTP

5. Rate Limiting [TODO]
   ├─ Max 3 requests per 15 minutes per email
   ├─ Prevents brute force attacks
   └─ Prevents spam/abuse
```

## Error Handling Flow

```
Verify OTP Request
        │
        ▼
    ┌───────────────┐
    │ Find token    │
    │ in database   │
    └───────┬───────┘
            │
      ┌─────┴─────┐
      │   Found?  │
      └─────┬─────┘
            │
     ┌──────┴──────┐
     │             │
    YES           NO
     │             │
     ▼             ▼
┌─────────┐   ┌────────────────────┐
│ Validate│   │ Error:             │
│ details │   │ "OTP không hợp lệ" │
└────┬────┘   └────────────────────┘
     │
     ├── Is used? ──YES──► Error: "OTP đã được sử dụng"
     │
     ├── Expired? ──YES──► Error: "OTP đã hết hạn"
     │
     └── All OK ──► Mark as used ──► Success
```

## Legend

```
Symbols Used:
  │   : Flow line
  ├   : Branch
  ►   : Direction/Arrow
  ┌─┐ : Box start/end
  └─┘ : Box start/end
  ▼   : Down arrow
  ◄   : Left arrow
```
