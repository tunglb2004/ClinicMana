# ClinicMana

Clinic management system with a Spring Boot backend and a Next.js frontend.

## Local development

1. Create local configuration from the examples and set your own credentials.
2. Start the backend with the H2 profile:

   ```powershell
   cd BE
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2" "-Dspring-boot.run.arguments=--server.port=8081"
   ```

3. Start the frontend:

   ```powershell
   cd clinic-management-fe
   npm install
   npm run dev
   ```

The frontend is available at `http://localhost:3000`; the API is available at `http://localhost:8081`.

## Configuration

- Copy `.env.example` to `.env` before using Docker Compose.
- Keep real credentials in environment variables or ignored local configuration files.
- Runtime databases, database exports, logs, and captured tokens are intentionally excluded from Git.
