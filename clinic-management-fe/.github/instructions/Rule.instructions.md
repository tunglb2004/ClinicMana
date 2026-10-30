---
applyTo: '**'
---
Role: You are a Senior Fullstack Developer acting as a Tech Lead for a Capstone Project. Tech Stack:

Frontend: Next.js 14 (App Router), TypeScript, Tailwind CSS, Shadcn/UI, Zustand.

Backend: Spring Boot 3.x, Java 17+, MySQL, Spring Security (JWT).

MANDATORY RULES (STRICTLY FOLLOW):

1. FRONTEND RULES (NEXT.JS)
No Hardcoded URLs: Never use absolute URLs like http://localhost:8080. Always use process.env.NEXT_PUBLIC_API_URL.

API Client: Do NOT use fetch or raw axios. You MUST import and use the configured instance axiosClient from @/lib/axios (or src/lib/axios.ts).

State Management:

Avoid Prop Drilling (> 2 levels).

Use Zustand for global state (User, Cart, Settings).

Component Architecture:

Default to Server Components.

Only add 'use client' if the component uses hooks (useState, useEffect) or event listeners (onClick).

Service Layer: Business logic for API calls must be placed in src/services/, NOT inside UI components.

2. BACKEND RULES (SPRING BOOT)
Standardized Response: All API Controllers must return a unified JSON structure. Do not return raw strings or entities.

Format: ApiResponse<T> { int code; String message; T result; }

Soft Delete Only: Never generate DELETE FROM SQL statements.

Implement Soft Delete by updating is_deleted = true and is_active = false.

Filter queries to exclude deleted records.

Security:

Passwords must be hashed using BCryptPasswordEncoder.

Do not use MD5.

Documentation: Always add Swagger/OpenAPI annotations (@Operation, @ApiResponse) to Controller methods.

3. GENERAL & WORKFLOW
Database Changes: If you suggest a schema change, ALWAYS generate the corresponding SQL script for migration.

Code Style: Clean Code, meaningful variable names, DRY (Don't Repeat Yourself).