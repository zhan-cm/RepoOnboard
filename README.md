# RepoOnboard

**English** | [简体中文](./README.zh-CN.md)

> Turn unfamiliar repositories into interactive codebase maps.

RepoOnboard is an open-source, local-first **codebase comprehension and developer onboarding tool**. It is designed to help developers understand an unfamiliar repository before they modify it.

> **Status: Early Development / Local Web UI In Progress**
>
> Maven, Java, Spring, API, component-dependency, and stable report analysis are available. The CLI serves each report to a packaged Vue application over a loopback-only endpoint. The responsive product Shell now includes a report-backed Repository Overview, Module Explorer, Architecture Workspace, and API Map; the remaining product views are still under development, and the first release is not available yet.

## Why RepoOnboard?

Understanding an unfamiliar codebase often means manually answering the same questions:

- What does this project do?
- Where does the application start?
- Which modules and components matter most?
- Which HTTP APIs are exposed?
- How do controllers, services, and repositories relate?
- Which files should I read first?

RepoOnboard aims to turn that exploration into a structured, traceable project map.

## V0.1 Scope

The first release intentionally focuses on one ecosystem:

| Area | V0.1 target |
| --- | --- |
| Language | Java |
| Build system | Maven |
| Framework | Spring Boot |
| Runtime | Java 21 or newer |
| Interface | Local read-only Web UI |

Multi-language support is a long-term direction, not a V0.1 requirement.

## Planned V0.1 Capabilities

- Detect Maven projects and modules.
- Extract project metadata and Maven dependencies.
- Discover Spring Boot application entry points.
- Identify controllers, services, repositories, components, and common mapper patterns.
- Extract Spring MVC HTTP endpoints.
- Build confirmed component dependency relationships.
- Preserve source locations and evidence for important findings.
- Present Overview, Architecture, API Map, and Start Here views locally.
- Recommend an explainable reading path through the repository.

The intended analysis pipeline is:

```text
Repository
    ↓
Maven Analysis
    ↓
Java Source Analysis
    ↓
Spring Boot Analysis
    ↓
Unified Project Model
    ↓
Local Interactive Codebase Map
```

## Intended Experience

The current command-line experience is:

```bash
cd unfamiliar-project
repoonboard .
```

After analysis, RepoOnboard starts a read-only local UI on a system-assigned `127.0.0.1` port and opens the default browser. Use `--no-open` to keep browser opening manual; the printed URL remains available until the process is stopped with Ctrl+C.

```bash
repoonboard . --no-open
```

The command detects a root `pom.xml`, displays its resolved coordinates, and prints the module hierarchy, source directories, discovered Java source roots/files, and dependencies before starting the UI. Each dependency retains groupId, artifactId, version, scope, raw/resolved values, and field source locations. Versions and scopes supplied by available parents or imported BOMs are supported; missing versions remain unknown with diagnostics. Only declared/inherited dependencies and active profiles are included, without downloading artifacts or computing a transitive graph. Unused dependencyManagement entries are not listed as dependencies.

Module aggregation and parent inheritance are recorded separately. Nested and active-profile modules are supported. Module paths and `build.sourceDirectory` resolve inside the scan root; source directories need not exist yet. Missing, malformed, escaping, duplicate, or cyclic modules produce diagnostics while other modules continue. The default Java source directory is `src/main/java` relative to each module; declared and resolved custom values remain available for source analysis.

Maven model resolution is offline. Relative parents must stay inside the scan root; external parents and imported BOMs may be read from the local Maven repository. Missing models retain available facts and produce `PARTIAL` results. BOM properties are not inherited by the importing project.

```bash
repoonboard . --profile dev --local-repository /path/to/local/maven/repository
```

`--profile` accepts repeated or comma-separated IDs. Only explicit and `activeByDefault` profiles participate; host OS/JDK/file/property activation and Maven settings are not used. The default cache is `~/.m2/repository`. Analysis does not execute Maven plugins, extensions, or lifecycle steps, or fetch remote POMs.

All POM sources reject DTD/external entities and paths escaping their allowed roots. Default limits are 1 MiB per POM, 64 distinct POM sources, and XML nesting depth 128. External source evidence uses `local-repository/` identifiers without exposing the cache's absolute path. CLI exit codes: `0` success, `1` failed analysis, `2` invalid arguments, `3` partial analysis; diagnostics are printed to standard error.

RepoOnboard will analyze the repository and produce information such as:

```text
Project: demo-shop
Build: Maven
Java: 21
Framework: Spring Boot 3.x

Modules: 4
Controllers: 18
Services: 27
Repositories: 12
HTTP endpoints: 83
```

Important results should remain traceable to their source:

```text
POST /users
    ↓
UserController.createUser()
    ↓
src/main/java/.../UserController.java:73
```

## Planned Interface

Spring Boot build detection uses a module's declared `org.springframework.boot:spring-boot-starter-parent`, imported `spring-boot-dependencies` BOM, or resolved/inherited Boot core and starter dependencies. Each signal retains source and version locations. Detection remains separate from version resolution; conflicting known versions yield an unknown version and a diagnostic. A build signal does not establish an application entry point. Indirect parent/BOM chains without a recognized dependency are not yet classified.

### Overview

Summarizes the technology stack, modules, components, endpoints, and application entry points.

### Architecture

Shows confirmed Spring component-injection relationships in a module-scoped interactive graph. Nodes and edges open a report-backed Inspector with source locations and raw evidence; unresolved or ambiguous relationships remain explicit instead of being drawn as facts. Module, component-kind, and relationship filters compose with selected-node 1-hop exploration. A searchable component list remains available as an alternative entry point, including when a matching scope exceeds the graph readability budget.

### API Map

Lists reported HTTP methods, paths, controllers, handler methods, source locations, and mapping conditions. Module, method, and text filters compose without treating `ANY` as a wildcard. Unresolved paths or conditions remain explicit, and selecting an endpoint opens its handler source plus method-level and controller-level mapping evidence.

### Start Here

Provides an explainable reading order based on build files, entry points, configuration, public APIs, and confirmed dependencies.

## Design Principles

- **Static analysis first** — derive reliable facts from source code, ASTs, build metadata, and framework metadata.
- **Accurate before impressive** — prefer smaller amounts of trustworthy information over speculative relationships.
- **Local first** — core analysis should run without uploading source code to a remote service.
- **Explainable results** — important findings should include source evidence whenever possible.
- **Partial success over total failure** — one broken file should not discard useful results from the rest of the repository.
- **One ecosystem done well** — complete Java, Maven, and Spring Boot support before expanding to more languages.

RepoOnboard V0.1 does not depend on an LLM. Future AI features, if introduced, should consume verified structured analysis rather than replace it.

## Roadmap

```text
M0  Technical Architecture                 ✓
M1  Project Foundation                     ✓
M2  Maven Analysis                         ✓
M3  Java Source Analysis                   ✓
M4  Spring Boot Analysis                  ✓
M5  API & Dependency Analysis             ✓
M6  Report Assembly & Serialization          ✓
M7  Local Web UI                           ◐
M8  Start Here
M9  Regression & Real Repository Validation
M10 Release Preparation
```

Detailed tasks and acceptance criteria are maintained in [TODO.md](./TODO.md).

V0.1 remains Web-first: it will ship as an executable JAR with Windows and POSIX launcher scripts. The Vue UI is kept host-independent so it can be reused later, but a native desktop shell, installer, and bundled Java runtime are deferred to a V0.2 technical spike rather than blocking the first release.

## Development Build

RepoOnboard development currently requires JDK 21 or newer and Node.js `^20.19.0` or `>=22.12.0`. A separate Maven installation or IDE is not required: the checked-in Maven Wrapper downloads the pinned Maven distribution, verifies its checksum, installs the locked frontend dependencies, and builds the packaged UI. End users of a release artifact will not need Node.js.

Windows:

```powershell
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
.\mvnw.cmd clean verify
```

macOS or Linux:

```bash
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
./mvnw clean verify
```

The current CLI provides Maven and Java source analysis plus Spring component, configuration, application-entry, injection, HTTP endpoint, and component-dependency facts with source evidence. Class- and method-level paths, HTTP methods, and mapping conditions are retained; `ANY` and unresolved paths remain explicit. Only uniquely confirmed project-local component targets become graph edges, while duplicate evidence is consolidated and ambiguous or missing targets remain diagnostics. MyBatis/MyBatis-Plus mapper classification is deferred. Stable report assembly and schema `1.2` JSON serialization feed the loopback service, packaged responsive product Shell, Repository Overview, Module Explorer, Architecture Workspace, and API Map. The module view uses explicit Maven aggregation relationships, confirmed exact-coordinate internal module dependencies, module-scoped counts, metadata, source roots, version facts, diagnostics, and evidence. The architecture view uses Cytoscape.js to render only confirmed component-injection edges within one module at a time, with composable filters, search, selected-node 1-hop exploration, zoom, pan, fit, selection, source evidence, responsive searchable-list fallback, and explicit partial/unavailable/over-budget states. The API view provides composable module/method/text filtering, explicit unresolved states, handler source locations, and both levels of mapping evidence. Source Navigation remains a future M7 task.

## Documentation

- [PROJECT.md](./PROJECT.md) — product vision, scope, and principles
- [DECISIONS.md](./DECISIONS.md) — accepted architecture and technical decisions
- [TODO.md](./TODO.md) — development roadmap and current task status
- [AGENTS.md](./AGENTS.md) — repository instructions for coding agents
- [Frontend Product Requirements](./docs/product/FRONTEND-PRODUCT-REQUIREMENTS.md) — page structure, interaction, visual direction, responsive behavior, and future desktop experience

## Target Users

RepoOnboard is intended for developers joining existing projects, open-source contributors, students, junior developers, and maintainers taking over unfamiliar or legacy codebases.

## Contributing

RepoOnboard is still in early development. Contribution guidelines will be added when the implementation is ready for external participation. Until then, changes should remain consistent with the project scope and accepted decisions.

## License

A license has not been finalized yet. One will be selected before the first public release.

## Project Philosophy

> **Understand first. Modify later.**
>
> **Static analysis before generative AI.**
>
> **Accurate before impressive.**
>
> **One ecosystem done well before many ecosystems done poorly.**
