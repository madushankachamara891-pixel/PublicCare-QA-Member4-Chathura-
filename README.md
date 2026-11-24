# PublicCare - QA Assignment Workspace

## Project overview

PublicCare is a citizen issue reporting and tracking platform.

Technology stack:
- Frontend: React 19 and Vite
- Backend: Java 21 and Spring Boot 3.5
- Database: PostgreSQL
- Database migrations: Flyway
- Backend testing: JUnit 5, Mockito, Spring Security Test and H2

## Folder structure

- `project/` - isolated copy of the PublicCare source code
- `qa/` - project analysis, requirements traceability matrix and test plan
- `MEMBER_TASKS.md` - your assigned QA responsibilities
- `CLAUDE.md` - instructions for Claude Code
- `CLAUDE_PROMPT.md` - starting prompt for your work

## Setup

### Backend

Requirements:
- Java 21
- PostgreSQL for full local application execution

Open a terminal in `project/backend`.

Configure local database values using the supplied `.env.example`.
Create your own local `.env` file. Never commit it.

Run backend unit tests:

```powershell
.\mvnw.cmd test
```

Run the backend application only after configuring a separate local test database:

```powershell
.\mvnw.cmd spring-boot:run
```

### Frontend

Requirements:
- Node.js and npm

Open a terminal in `project`.

Install dependencies:

```powershell
npm install
```

Run lint:

```powershell
npm run lint
```

Build:

```powershell
npm run build
```

Run frontend:

```powershell
npm run dev
```

Note: The frontend currently has no configured Vitest/Jest test script in the supplied package.json. Inspect before proposing frontend automation.

## QA documents

Read these files before starting:

1. `qa/PROJECT_ANALYSIS_REPORT.md`
2. `qa/PART01_REQUIREMENTS_AND_RTM.md`
3. `qa/PART01_TEST_PLAN_AND_STLC.md`

Use the existing requirements, test cases and project behavior as the basis for your work.

## Important rules

1. Work only inside your own `project/` copy.
2. Do not access or modify the original project.
3. Do not push changes to the original repository.
4. Do not invent test execution results, coverage, defects, performance values or screenshots.
5. Mark tests as NOT EXECUTED until they have actually been run.
6. Record environment limitations and blocked tests clearly.
7. Do not commit passwords, tokens, database credentials or real user data.
8. Use synthetic test data only.
9. Keep evidence files and screenshots organized.
10. Report commands, tool versions and actual results.

## Evidence

Create an `evidence/` directory for your own work.

Include:
- Test execution screenshots or logs
- Test case results
- Coverage reports where applicable
- Automation execution evidence
- Defect records
- Tool configuration and quality reports

Do not use fabricated evidence.

## Member 4 results

[Sections 3.7–3.10 report](qa/MEMBER4_QA_REPORT.md) · [Raw execution evidence](evidence/member4/) · [Reproduction commands](qa/member4/REPRODUCE.md)
