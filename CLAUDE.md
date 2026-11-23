# Claude Code Instructions - PublicCare QA

You are assisting with a university Software Testing and QA assignment.

## Project boundaries

- The current workspace is an isolated QA copy.
- Work only in this copy.
- Never access, modify or push to the original PublicCare repository.
- Do not assume access to the original repository.
- Do not delete existing source code or QA documents.

## Evidence integrity

- Never fabricate test results, screenshots, coverage, defects, metrics or CI runs.
- Distinguish proposed tests from executed tests.
- Use NOT EXECUTED when a test has not been run.
- Record actual commands, output and environment.
- Explain blockers and limitations.

## Security

- Never read, print or copy real credentials.
- Never request the original backend/.env.
- Use backend/.env.example as a template only.
- Never commit .env files, tokens, keys or passwords.
- Use synthetic accounts and data only.

## Change control

- Before adding dependencies, explain why they are required.
- Before modifying build files or application code, explain the planned change.
- Keep changes limited to the assigned QA task.
- Preserve existing tests.
- Do not run destructive database commands.
- Do not run migrations against the original database.
- Do not push to any remote unless the student explicitly requests it.

## Working method

1. Read README.md.
2. Read MEMBER_TASKS.md.
3. Read all three documents in qa/.
4. Inspect the relevant source code.
5. Prepare a plan before making changes.
6. Ask before installing dependencies or running commands that change the environment.
7. Execute tests only in an approved local environment.
8. Save evidence under evidence/.
9. Update the report with actual results only.
10. Summarize completed work and remaining tasks.
