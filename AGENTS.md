可以，下面这版我专门按 **Codex 仓库级执行说明** 来设计，和你现有的 `PROJECT.md / DECISIONS.md / TODO.md / README.md` 配套。

直接复制到：

```text
E:\Projects\RepoOnboard\AGENTS.md
```

即可。

````markdown
# RepoOnboard — Agent Instructions

> Repository-level instructions for Codex and other coding agents.

---

## 1. Purpose

This file defines how coding agents should work inside the RepoOnboard repository.

It is an execution guide, not a product specification.

Before making changes, agents must use the project documents as the source of truth.

---

## 2. Required Reading Order

Before starting any task, read the following files in this order:

1. `PROJECT.md`
2. `DECISIONS.md`
3. `TODO.md`
4. `STATE.md`

Read `README.md` when the task affects:

- user-facing behavior
- installation
- CLI usage
- public documentation
- features visible to GitHub users

Do not rely only on the current prompt if repository documentation provides more specific constraints.

---

## 3. Source of Truth

Use the following priority order:

```text
PROJECT.md
    ↓
DECISIONS.md
    ↓
TODO.md
    ↓
Current Implementation
    ↓
Task Prompt
````

### PROJECT.md

Defines:

* product vision
* product scope
* V0.1 boundaries
* long-term direction
* product principles
* explicit non-goals

Do not violate `PROJECT.md`.

---

### DECISIONS.md

Defines accepted:

* architecture decisions
* technology choices
* implementation strategies
* data model decisions
* analyzer boundaries

Any decision marked:

```text
Status: ACCEPTED
```

must be treated as authoritative unless the user explicitly requests reconsideration.

Do not silently replace an accepted technology or architecture.

---

### TODO.md

Defines:

* current milestone
* current development task
* acceptance criteria
* task dependencies
* known issues
* deferred work

When the user provides a task ID such as:

```text
T-0201
```

work only on that task unless explicitly instructed otherwise.

---

### STATE.md

Describes the repository's current consolidated state, including:

* completed capabilities
* current architecture
* known limitations
* current and next tasks

Maintain it as a current snapshot, not an append-only development log. If `STATE.md` conflicts with verified current code, use the code evidence and correct `STATE.md` as part of the assigned task.

---

## 4. Product Context

RepoOnboard is a:

> Multi-language codebase comprehension and developer onboarding platform.

Its purpose is to help developers understand unfamiliar repositories faster.

Long-term product direction:

```text
Repository
    ↓
Language / Framework Analyzer
    ↓
Unified Project Model
    ↓
Architecture / Dependency Analysis
    ↓
Interactive Codebase Map
```

---

## 5. Current V0.1 Scope

V0.1 supports only:

```text
Language:
Java

Build System:
Maven

Framework:
Spring Boot
```

The long-term multi-language vision does NOT authorize implementing additional languages during V0.1.

Do not add support for:

* Python
* JavaScript
* TypeScript
* Go
* Rust
* C#
* C/C++
* other frameworks

unless explicitly requested and `PROJECT.md` has been updated accordingly.

---

## 6. V0.1 Product Principles

All implementation work should follow these principles.

### Static Analysis First

Prefer deterministic analysis based on:

* source code
* AST
* build metadata
* framework metadata
* project structure

---

### Deterministic Before Generative

If information can be derived reliably from code, do not use an LLM to infer it.

Example:

```java
@RestController
public class UserController {
}
```

should be identified through static analysis.

Do not introduce AI to classify deterministic code structures.

---

### Local First

Core repository analysis must work locally.

Do not introduce mandatory:

* cloud services
* remote APIs
* SaaS dependencies
* external databases

for core V0.1 analysis.

---

### Explainable Results

Important analysis results should retain source evidence where possible.

Examples:

```text
Source File
Source Line
Source Symbol
Evidence Type
```

A user should be able to understand why RepoOnboard produced a result.

---

### Accurate Before Impressive

Prefer:

```text
Correct
Reliable
Traceable
Explainable
```

over:

```text
Visually impressive but unreliable
```

Do not create speculative architecture relationships.

---

### Avoid Overengineering

Do not introduce complexity only because it may be useful later.

Avoid premature:

* plugin frameworks
* microservices
* distributed architecture
* dynamic runtime extension systems
* DSLs
* multi-tenant systems
* complex cloud infrastructure

V0.1 should remain simple and testable.

---

## 7. Explicit V0.1 Non-Goals

Do not implement the following unless explicitly requested:

* LLM integration
* RAG
* vector databases
* AI agents
* MCP servers
* code generation
* automatic code modification
* automatic refactoring
* code review
* bug fixing
* technical debt scoring
* full method-level call graphs
* runtime tracing
* Git history analysis
* contributor analysis
* GitHub Star analysis
* user accounts
* cloud sync
* SaaS
* team workspace
* VS Code extensions
* JetBrains plugins
* multi-language support

If such a capability appears useful during development:

> record it as deferred work instead of implementing it.

---

## 8. Task Execution Rules

Every coding task should follow this process:

```text
Read project documents
        ↓
Identify current task
        ↓
Inspect existing implementation
        ↓
Determine minimum required changes
        ↓
Implement
        ↓
Run relevant tests
        ↓
Validate acceptance criteria
        ↓
Update TODO.md
        ↓
Report result
```

---

## 9. Work Only on the Assigned Task

If the assigned task is:

```text
T-0201 — Maven Project Detection
```

implement only what is required for Maven project detection.

Do not automatically continue with:

* Maven metadata parsing
* dependency extraction
* Java AST parsing
* Spring component analysis
* frontend work

unless those are part of the same explicit task.

---

## 10. Do Not Pre-Implement Future Tasks

Do not implement functionality merely because:

> "It will probably be needed later."

If future work becomes apparent:

1. note the issue
2. add or suggest a TODO entry if appropriate
3. continue with the current task

Do not expand the implementation scope automatically.

---

## 11. Do Not Perform Unrelated Refactors

Avoid modifying unrelated working code.

A task should not trigger:

* repository-wide renames
* broad architecture rewrites
* dependency replacements
* module restructuring
* formatting entire unrelated directories

unless necessary for the task.

Prefer the smallest correct change.

---

## 12. Existing Code Before New Code

Before implementing a feature:

1. search the repository
2. inspect related code
3. determine whether functionality already exists
4. reuse existing abstractions when they are appropriate

Do not create duplicate analyzers, models, utilities, or infrastructure.

---

## 13. Architecture Decision Rules

If `DECISIONS.md` contains an accepted ADR:

```text
Status: ACCEPTED
```

follow it.

Do not change an accepted decision because another option appears:

* newer
* more popular
* more elegant
* personally preferable

---

## 14. When an Existing Decision Appears Wrong

If implementation evidence shows that an accepted decision is problematic:

Do NOT silently replace it.

Instead report:

```text
Existing Decision:
ADR-XXXX

Observed Problem:
...

Evidence:
...

Suggested Alternative:
...

Impact:
...
```

Wait for explicit approval before creating a replacement ADR or changing the architecture.

---

## 15. New Technical Decisions

A new ADR may be appropriate when a decision is:

* high impact
* difficult to reverse
* architectural
* likely to be debated again
* not obvious from the implementation

Examples:

* AST parser selection
* CLI framework
* project model structure
* analyzer boundaries
* graph representation
* local Web architecture

Do not create ADRs for trivial implementation details.

---

## 16. Repository Model Rules

RepoOnboard should maintain a reasonably generic conceptual model.

Core concepts may include:

```text
Project
Module
SourceFile
Component
Endpoint
EntryPoint
Dependency
SourceLocation
```

Avoid making the entire Core model Spring-specific.

For example, prefer:

```text
Component
kind = CONTROLLER
language = JAVA
framework = SPRING_BOOT
```

over making all downstream systems depend on:

```text
SpringController
SpringService
SpringRepository
```

However:

> Do not build a universal AST or excessively generic type system for V0.1.

Use abstraction only where it provides immediate structural value.

---

## 17. Analyzer Boundary Rules

Conceptually separate:

```text
Build-System Analysis
Language Analysis
Framework Analysis
Core Project Model
Presentation
```

For V0.1 this may include:

```text
Maven Analyzer
Java Analyzer
Spring Boot Analyzer
```

Framework-specific logic should remain identifiable and isolated where practical.

Do not create a complex plugin runtime solely to enforce this separation.

---

## 18. Static Analysis Accuracy Rules

When analyzing repositories:

### Prefer confirmed relationships

Example:

```text
Controller
    ↓
Service
```

when supported by clear source evidence.

---

### Avoid speculative relationships

If a dependency cannot be resolved reliably:

* mark it unresolved
* omit it
* report uncertainty

Do not invent an edge simply to make the graph look complete.

---

### Preserve evidence

Where possible, store:

```text
sourceFile
line
column
symbol
evidenceType
```

---

## 19. Error Handling Rules

Real repositories may contain:

* invalid source files
* unsupported syntax
* partially generated code
* incomplete modules
* broken Maven metadata
* missing dependencies

RepoOnboard should prefer:

```text
Partial successful analysis
```

over:

```text
Abort entire repository analysis
```

A failure in one file should not automatically terminate the complete scan unless continuation is impossible.

Errors should be:

* clear
* actionable
* associated with relevant files/modules when possible

---

## 20. Testing Requirements

Code is not complete merely because it compiles.

For each task, run the most relevant tests.

Preferred test layers:

```text
Unit Test
    ↓
Fixture Project Test
    ↓
Integration Test
    ↓
Real Repository Validation
```

Not every small task requires all four levels.

Use the narrowest sufficient verification.

---

## 21. Fixture Project Rules

Fixtures should represent intentionally controlled repository structures.

Examples:

```text
simple-spring-project
multi-module-project
spring-api-project
dependency-project
```

Fixtures should be:

* minimal
* deterministic
* easy to understand
* designed around specific analyzer behavior

Do not turn fixture projects into full applications.

---

## 22. Real Repository Validation

Passing fixture tests is not sufficient for mature analyzer functionality.

For important milestones, validate against real open-source Spring Boot repositories.

Observe:

* parsing failures
* unsupported patterns
* noisy relationships
* graph readability
* performance
* unexpected project structures

Do not modify third-party repositories as part of validation.

---

## 23. Test Completion Rule

Before marking a TODO item complete:

```text
Implementation complete
+
Relevant tests pass
+
Acceptance criteria satisfied
```

If tests fail:

```text
DO NOT mark the task complete.
```

Use:

```text
[-] In Progress
```

or:

```text
[!] Blocked
```

as appropriate.

---

## 24. Build Rules

Once build commands are finalized in `DECISIONS.md`, use those commands consistently.

Do not rely solely on IntelliJ IDEA for:

* build
* test
* packaging

The project must remain buildable from the command line.

If a Maven Wrapper exists, prefer it.

Typical future commands may look like:

```bash
./mvnw test
```

or on Windows:

```powershell
.\mvnw.cmd test
```

Use the actual repository configuration rather than assuming these commands exist.

---

## 25. Cross-Platform Considerations

RepoOnboard is expected to eventually work across:

* Windows
* macOS
* Linux

Avoid unnecessary platform-specific assumptions.

Be careful with:

* filesystem separators
* shell commands
* path normalization
* temporary directories
* browser opening
* executable scripts
* line endings

Prefer Java platform APIs where practical.

---

## 26. Dependency Rules

Before adding a new dependency, ask:

1. Is this required by the current task?
2. Does the repository already provide equivalent functionality?
3. Is the dependency mature and maintained?
4. Does it significantly simplify implementation?
5. Does it add unnecessary runtime weight?

Do not add libraries only to save a few trivial lines of code.

Do not reimplement complex mature functionality unnecessarily.

---

## 27. Security and Privacy

RepoOnboard analyzes user source code.

Therefore:

* do not upload repository contents by default
* do not introduce hidden telemetry
* do not expose source contents to remote services
* do not log secrets unnecessarily
* avoid printing full sensitive configuration values

Local-first behavior should remain the default.

---

## 28. Generated Files

Generated RepoOnboard output should remain clearly separated from user source code.

Preferred conceptual location:

```text
.repoonboard/
```

Do not modify the analyzed repository's source code during normal analysis.

RepoOnboard V0.1 is read-oriented.

---

## 29. Documentation Update Rules

### PROJECT.md

Do not modify unless the user explicitly changes:

* product direction
* V0.1 scope
* product principles
* major product boundaries

---

### DECISIONS.md

Update only when:

* a technical decision has been explicitly accepted
* an existing ADR has been superseded
* an important architecture decision is approved

Do not update it for ordinary implementation details.

---

### TODO.md

Update when:

* task status changes
* a task is completed
* a blocker is discovered
* a new deferred issue is formally recorded

Do not rewrite the entire roadmap during a normal coding task.

---

### README.md

Update when user-facing behavior changes, such as:

* installation
* CLI syntax
* supported features
* supported environments
* public limitations

Do not modify README for invisible internal refactors unless documentation becomes inaccurate.

---

## 30. TODO Update Discipline

When completing:

```text
T-XXXX
```

only change the relevant task status and closely related information.

Avoid reformatting or rewriting unrelated TODO sections.

Example:

```text
Before:

## T-0201 — Maven Project Detection

Status:

[ ]
```

After successful completion:

```text
## T-0201 — Maven Project Detection

Status:

[x]
```

Keep changes minimal.

---

## 31. Git Change Discipline

Prefer focused changes.

A single task should ideally produce a logically focused diff.

Do not combine:

```text
Feature implementation
+
Unrelated cleanup
+
Architecture redesign
+
Documentation overhaul
```

into one task unless explicitly requested.

---

## 32. Comments and Code Style

Prefer code that explains itself through:

* clear naming
* small focused methods
* clear domain types
* simple control flow

Use comments primarily for:

* non-obvious reasoning
* parser edge cases
* framework-specific behavior
* algorithmic decisions

Do not add comments that merely restate obvious code.

---

## 33. Logging

Logging should help diagnose analysis behavior.

Useful logging examples:

```text
Detected Maven module: shop-user

Failed to parse source file:
src/main/java/.../Broken.java

Continuing analysis...
```

Avoid excessive logs for every AST node or trivial event.

---

## 34. Performance Discipline

Do not prematurely optimize V0.1.

However, avoid obviously inefficient behavior such as:

* repeatedly parsing the same file
* recursively rescanning the same directory
* rebuilding identical project metadata
* loading unnecessary large files multiple times

Measure before introducing advanced optimization.

Deferred unless evidence requires them:

* complex caches
* incremental compilation models
* distributed analysis
* advanced parallel pipelines

---

## 35. UI Rules

The Web UI exists to improve understanding, not to showcase frontend complexity.

Prioritize:

```text
Clarity
Readability
Navigation
Progressive disclosure
```

over:

```text
Animation
Visual spectacle
Dense graphs
```

Architecture graphs should not become unreadable "hairballs."

Filtering and hierarchy should be preferred over displaying every node at once.

---

## 36. Start Here Rules

The `Start Here` feature must remain explainable.

If a file is recommended, provide a reason where possible.

Example:

```text
LoginController.java

Reason:
- Exposes authentication endpoint
- Depends on LoginService
- Central entry point for login flow
```

Do not create opaque ranking scores without meaningful explanation.

V0.1 should prefer understandable heuristics over AI ranking.

---

## 37. AI Features

Do not introduce AI features during V0.1.

Future AI should consume structured RepoOnboard output:

```text
Source Code
    ↓
Static Analysis
    ↓
Project Model
    ↓
Dependency Graph
    ↓
Source Evidence
    ↓
Optional AI
```

The architecture should never depend on:

```text
Repository
    ↓
LLM
    ↓
Unverified interpretation
```

as the primary analysis mechanism.

---

## 38. Task Planning

For a non-trivial task:

1. inspect relevant code
2. identify affected files
3. form a short implementation plan internally
4. implement
5. verify

Do not generate a large new planning document unless requested.

Repository documentation already defines the broader plan.

---

## 39. Clarification Policy

Do not ask the user for clarification when the answer can be reliably determined from:

* `PROJECT.md`
* `DECISIONS.md`
* `TODO.md`
* repository code
* existing tests

If a non-critical implementation detail is unspecified:

> make the smallest reasonable assumption consistent with project principles.

Report the assumption when relevant.

Ask the user only when the ambiguity materially changes:

* product scope
* architecture
* public behavior
* irreversible implementation direction

---

## 40. Blocker Policy

If a task cannot be completed because of a genuine blocker:

Do not fake completion.

Report:

```text
Task:
T-XXXX

Status:
BLOCKED

Blocker:
...

Evidence:
...

Recommended next step:
...
```

Update TODO status only if appropriate.

---

## 41. Final Response Format

After completing a normal coding task, keep the response concise.

Use:

```text
Task:
T-XXXX

Completed:
- ...

Key Changes:
- ...

Tests:
PASS / FAIL
- ...

Remaining Issues:
- None
```

Avoid:

* long tutorials
* repeating full source files
* explaining every method
* unrelated future feature suggestions
* re-describing RepoOnboard

unless explicitly requested.

---

## 42. Definition of Done

A task is done only when:

```text
Scope implemented
+
Acceptance criteria satisfied
+
Relevant tests pass
+
No known task-blocking defect remains
+
TODO updated when appropriate
```

"Code written" is not sufficient.

---

## 43. Current Project Stage

RepoOnboard is currently in:

> M7 — Local Web UI

The current next task is:

```text
T-0705 — Architecture Workspace design handoff
```

T-0701 through T-0704 have established the CLI-to-loopback-UI runtime closure, shared visual system, report-backed Repository Overview, and Stitch-aligned Module Explorer. Before coding T-0705 or any later frontend view, first provide the user with a page-level data contract audit and a Stitch design proposal derived from TODO.md, the public report model, and current implementation. Wait for the user to finish and import the Stitch design before implementing that UI. Architecture exploration and later views remain separate tasks. Desktop packaging is deferred beyond V0.1; do not introduce Tauri, Electron, a native installer, or a bundled runtime during M7.

Always follow the current `TODO.md` and `STATE.md` if this snapshot becomes stale.

---

## 44. Default Agent Workflow

For every new task:

```text
1. Read PROJECT.md
2. Read DECISIONS.md
3. Read TODO.md
4. Read AGENTS.md and STATE.md
5. Read relevant existing code
6. Identify assigned task
7. Check task acceptance criteria
8. Implement only necessary changes
9. Run relevant tests
10. Verify acceptance criteria
11. Update TODO.md and STATE.md if appropriate
12. Return concise completion report
```

---

## 45. Core Working Philosophy

Always prefer:

> **Understand before modifying.**

> **Correct before clever.**

> **Evidence before assumption.**

> **Simple before extensible.**

> **Current requirements before hypothetical future requirements.**

> **One task completed well before several tasks partially completed.**

````

这版 `AGENTS.md` 的重点不是继续描述 RepoOnboard 功能，而是**规定 Codex 如何参与这个项目**。

所以现在你的根目录基本就成型了：

```text
RepoOnboard/
│
├── AGENTS.md
│   └── AI / Codex 怎么工作
│
├── PROJECT.md
│   └── 产品是什么、为什么做
│
├── DECISIONS.md
│   └── 技术上决定怎么做
│
├── TODO.md
│   └── 当前具体做什么
│
├── README.md
│   └── GitHub 用户看到什么
│
├── .gitignore
└── .gitattributes
````

以后你给 Codex 的任务就可以非常短，例如：

```text
Read AGENTS.md and the referenced project documents.

Work on T-0001 only.

Do not begin implementation beyond the current task.
```

或者等进入编码期以后：

```text
Implement T-0201 according to TODO.md.

Follow AGENTS.md and all accepted ADRs.
Run the relevant tests when finished.
```
