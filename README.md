# RepoOnboard

**English** | [简体中文](./README.zh-CN.md)

> Turn unfamiliar repositories into interactive codebase maps.

RepoOnboard is an open-source, local-first **codebase comprehension and developer onboarding tool**. It is designed to help developers understand an unfamiliar repository before they modify it.

> **Status: Early Development / Project Foundation**
>
> The architecture and build foundation are in place, but the first usable release is not available yet.

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
M1  Project Foundation                     In Progress
M2  Maven Analysis
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

The build currently validates the project foundation. It does not yet provide a usable repository analysis workflow.

## Documentation

- [PROJECT.md](./PROJECT.md) — product vision, scope, and principles
- [DECISIONS.md](./DECISIONS.md) — accepted architecture and technical decisions
- [TODO.md](./TODO.md) — development roadmap and current task status
- [AGENTS.md](./AGENTS.md) — repository instructions for coding agents

## Target Users

RepoOnboard is intended for developers joining existing projects, open-source contributors, students, junior developers, and maintainers taking over unfamiliar or legacy codebases.

## Contributing

RepoOnboard is still in its project foundation stage. Contribution guidelines will be added when the implementation is ready for external contributions. Until then, changes should remain consistent with the project scope and accepted decisions.

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
