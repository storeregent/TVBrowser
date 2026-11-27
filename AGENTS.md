TVBrowser (TVBrowse) — Global Agent Instructions
1. Project Identity

Project: TVBrowse / TVBrowser

Product: Premium Android TV-first web browser.

Primary objective: Build a reliable, fast, remote-friendly Android TV browser with a better user experience and technical reliability than existing TV browsers.

The project is being developed through multiple specialized AI agents.

Every agent must follow these instructions.

2. Source of Truth

Before starting any task, inspect:

README.md
AGENTS.md
Relevant documents inside docs/
Existing implementation inside app/
Existing validation work inside prototype/, tests/ and reports/

The approved PRD and TECH_SPEC are authoritative for product and technical requirements.

Never invent requirements or silently change approved decisions.

If documents conflict:

Identify the conflict.
Record the exact references.
Explain the impact.
Ask the Lead Agent for resolution.

Never assume that a suggestion is an approved requirement.

3. Strict Agent Role Isolation

Each agent has a specific assigned role and task.

Agents MUST:

Work only within their assigned responsibility.
Follow the exact task scope.
Avoid performing another agent's responsibilities.
Avoid modifying files outside their assigned scope.
Avoid duplicating work already assigned to another agent.
Report discovered issues to the Lead Agent.
Stop and ask when a task requires a decision outside their authority.

Agents MUST NOT:

Take over the entire project.
Independently change product scope.
Start unrelated development.
Modify approved PRD or TECH_SPEC documents without authorization.
Delete or overwrite another agent's work.
Merge pull requests or push directly to main.
Claim work is complete without evidence.
4. Current Project Stage

The project is currently in W0 validation.

W1 production development is blocked until the required W0 validation gates are completed and approved by the founder.

The current initial assignment is PROJECT AUDIT ONLY.

Do not start production coding unless explicitly authorized.

A limited technical validation prototype is permitted only when assigned and must remain isolated from production implementation.

5. Technical Direction

Follow the approved TECH_SPEC.

Current approved direction includes:

Kotlin
Jetpack Compose
Android System WebView
Android TV / D-pad-first navigation
Browser engine abstraction
Room and DataStore where specified
Maximum two live WebViews
Tab lifecycle and restoration
Renderer crash recovery
TV remote and virtual cursor support

Do not introduce a new framework, paid API, SDK, service or major dependency without approval.

Do not replace the approved architecture based on personal preference.

6. Evidence and Testing Rules

Use these exact statuses:

PASS: Test actually executed and passed.
FAIL: Test actually executed and failed.
BLOCKED: Test cannot proceed due to a dependency.
NOT TESTED: Test has not been executed.
UNVERIFIED: Available evidence is insufficient.

Never fabricate:

Device testing
Performance measurements
Memory results
Compatibility results
User feedback
Competitor findings
Platform policy approval

Clearly distinguish static analysis, emulator testing and physical-device testing.

Every important claim must have evidence or an explicit uncertainty label.

7. GitHub Workflow
Never push directly to main.
Use a dedicated branch for each assigned task.
Keep changes small and reviewable.
Never overwrite another agent's changes.
Open a pull request for code or document changes when authorized.
Do not merge your own work into main.
Never commit passwords, API keys, signing keys or private APK files.
8. File Ownership

Only modify files explicitly assigned by the Lead Agent.

If a change outside your scope is necessary:

Do not modify it.
Explain why it is needed.
Identify the affected file.
Request authorization.

Do not allow two agents to edit the same file concurrently.

9. Task Completion Report

Every agent must finish with:

Assigned task
Work completed
Files created or modified
Evidence and references
Tests performed
Test status
Unresolved issues
Dependencies
Recommended next action

Do not continue to another task automatically.

Wait for the Lead Agent's next assignment.

10. Founder Approval

The founder has final authority over:

Product scope
Architecture changes
Budget and paid dependencies
W0/W1 stage transitions
Acceptance criteria
Final release decisions

When approval is required, stop and wait.

Golden Rule

Do your assigned job only.

Do not act as the project owner unless you are explicitly assigned as the Lead Agent.

Do not make decisions outside your authority.

Protect the existing project, its approved requirements and the integrity of its evidence.
