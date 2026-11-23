# Member 4 - Non-functional Testing and Quality Assurance

## Assigned sections

- Section 3.7 - Performance, Security and Usability Testing
- Section 3.8 - Defect Tracking and Management
- Section 3.9 - SonarQube and Software Quality Metrics
- Section 3.10 - Case Study

## Objective

Evaluate the PublicCare application's non-functional quality and document actual findings.

## Section 3.7 - Performance testing

Use Apache JMeter or another justified tool.

1. Identify suitable application endpoints.
2. Define a small load-test scenario.
3. Record thread count, ramp-up and duration.
4. Measure response times and throughput.
5. Record error percentage.
6. Explain the test environment and limitations.

Do not perform heavy load tests against a shared or production system.

## Section 3.7 - Security testing

Perform 1-2 safe manual security checks based on OWASP Top 10.

Possible areas to investigate:
- Broken Access Control
- Authentication weaknesses
- Security misconfiguration
- Injection risks

Use only your isolated local test environment and synthetic accounts.

Never attack external systems or use real users' credentials.

Record:
- Security test objective
- Preconditions
- Steps
- Expected behavior
- Actual behavior
- Evidence
- Risk/severity
- Recommendation

## Section 3.7 - Usability testing

Inspect key user workflows.

Possible areas:
- Registration and login
- Create report
- View report details
- Edit own report
- Administrator report management

Record observable usability issues and supporting screenshots.

## Section 3.8 - Defect tracking

Create a defect log.

For each defect include:
- Defect ID
- Summary
- Steps to reproduce
- Expected result
- Actual result
- Severity
- Priority
- Environment
- Evidence
- Status
- Root cause analysis, if confirmed

Do not invent defects.

## Section 3.9 - SonarQube

1. Inspect the project structure.
2. Configure an appropriate SonarQube analysis.
3. Analyze backend and frontend where feasible.
4. Record actual quality metrics.
5. Include bugs, vulnerabilities, code smells, duplication and coverage only where the tool reports them.
6. Explain missing or unsupported metrics.

Do not report sample or estimated metrics as real analysis results.

## Section 3.10 - Case study

Prepare a case study based on the actual PublicCare project.

Discuss:
- Project context
- QA activities performed
- Tools used
- Findings
- Defects and improvements
- Quality limitations
- Lessons learned
- Recommendations

Distinguish actual findings from recommendations.

## Deliverables

- JMeter test plan and result evidence
- Security test records
- Usability findings
- Defect log and RCA
- SonarQube report/screenshots
- Quality metrics
- Case study report

## Constraints

- Work only in your Member4 project copy.
- Use a local isolated test environment.
- Do not modify the original project.
- Never use real user data or credentials.
- Do not fabricate findings, metrics or test outcomes.
