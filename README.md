# RepoOnboard

**English** | [简体中文](./README.zh-CN.md)

> Turn unfamiliar repositories into interactive codebase maps.

RepoOnboard is an open-source, local-first **codebase comprehension and developer onboarding tool**. It is designed to help developers understand an unfamiliar repository before they modify it.

> **Status: Early Development / Java Source Analysis**
>
> Maven analysis, Spring Boot build detection, and module-owned Java source-file discovery are available. Basic Java AST parsing is next; the first usable release is not available yet.

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

The future command-line experience is designed to be simple:

```bash
cd unfamiliar-project
repoonboard .
```

At the current stage, this command detects a root `pom.xml`, displays its resolved coordinates, and prints the module hierarchy, source directories, discovered Java source roots/files, and dependencies. Each dependency retains groupId, artifactId, version, scope, raw/resolved values, and field source locations. Versions and scopes supplied by available parents or imported BOMs are supported; missing versions remain unknown with diagnostics. Only declared/inherited dependencies and active profiles are included, without downloading artifacts or computing a transitive graph. Unused dependencyManagement entries are not listed as dependencies.

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

Shows important module and component relationships without turning the graph into an unreadable hairball.

### API Map

Lists HTTP methods, paths, controllers, handler methods, and source evidence.

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
M3  Java Source Analysis
M4  Spring Boot Analysis
M5  API & Dependency Analysis
M6  Report Assembly & Serialization
M7  Local Web UI
M8  Start Here
M9  Regression & Real Repository Validation
M10 Release Preparation
```

Detailed tasks and acceptance criteria are maintained in [TODO.md](./TODO.md).

## Development Build

RepoOnboard currently requires JDK 21 or newer. A separate Maven installation or IDE is not required: the checked-in Maven Wrapper downloads the pinned Maven distribution and verifies its checksum.

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

The current CLI provides an early Maven repository analysis and Java source-file discovery workflow. Java AST, Spring component, API, dependency-graph, and Web UI analysis are still under development.

## Documentation

- [PROJECT.md](./PROJECT.md) — product vision, scope, and principles
- [DECISIONS.md](./DECISIONS.md) — accepted architecture and technical decisions
- [TODO.md](./TODO.md) — development roadmap and current task status
- [AGENTS.md](./AGENTS.md) — repository instructions for coding agents

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
