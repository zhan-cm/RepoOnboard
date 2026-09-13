# RepoOnboard

**English** | [简体中文](./README.zh-CN.md)

> Turn an unfamiliar Java, Maven, and Spring Boot repository into a traceable local codebase map.

RepoOnboard is a local-first codebase comprehension and developer onboarding tool. It statically analyzes a repository, keeps the source evidence behind important findings, and presents the result in a read-only Web UI before you start changing code.

> **Status: pre-release (`0.1.0-SNAPSHOT`)**
>
> The implementation is in release preparation. There is no downloadable V0.1 release or release tag yet; use the source-build instructions below. The source is public, but a license has not been selected, so it should not yet be described as open source or assumed to grant reuse rights.

## Demo

The V0.1 public demo target is [`jhipster/jhipster-sample-app`](https://github.com/jhipster/jhipster-sample-app) at fixed commit [`e06e87abe0be8a3a194381ce651164a734811b3f`](https://github.com/jhipster/jhipster-sample-app/commit/e06e87abe0be8a3a194381ce651164a734811b3f). That revision contains an [Apache License 2.0 file](https://github.com/jhipster/jhipster-sample-app/blob/e06e87abe0be8a3a194381ce651164a734811b3f/LICENSE.txt) and is analyzed without modifying, building, or running the third-party source.

See the [V0.1 Demo Repository guide](./docs/demo/V0.1-DEMO.md) for the exact checkout, empty-cache analysis command, expected counts, limitations, and four-page walkthrough. The fixed run produces a transparent `PARTIAL` report with one module, 81 source files, 30 components, 24 endpoints, 7 confirmed component edges, and 24 Start Here items. The partial status and known wildcard-import omissions are part of the demo, not hidden.

For a faster synthetic smoke test, first [build from source](#install-from-source), then run the repository's [small deterministic Spring fixture](./src/test/resources/fixtures/spring-analysis-project):

Windows PowerShell:

```powershell
.\repoonboard.cmd .\src\test\resources\fixtures\spring-analysis-project --no-open
```

macOS or Linux:

```bash
./repoonboard ./src/test/resources/fixtures/spring-analysis-project --no-open
```

Open the printed `http://127.0.0.1:<port>/` address and stop the process with Ctrl+C. Both paths use the current `target/repoonboard.jar` source-build artifact; they do not point to a release archive that does not exist yet.

## Install from source

### Requirements

| Requirement | Needed to run | Needed to build from source |
| --- | --- | --- |
| Java | Java 21 or newer | JDK 21 or newer |
| Node.js | No | `^20.19.0` or `>=22.12.0` |
| Maven | No | No separate install; the Wrapper supplies Maven 3.9.16 |
| Git | No | Yes, for the commands below |

A clean machine needs network access for the initial clone and for the Wrapper to download Maven plus the locked Java and frontend build dependencies. An analyzed repository is not built or executed, and core repository analysis does not fetch its POMs or artifacts from remote repositories.

Check the required tools:

```text
java -version
node --version
git --version
```

Windows PowerShell:

```powershell
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
.\mvnw.cmd clean verify
.\repoonboard.cmd --help
```

macOS or Linux:

```bash
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
./mvnw clean verify
chmod +x repoonboard
./repoonboard --help
```

The verified build produces:

```text
target/repoonboard.jar
target/repoonboard.jar.sha256
```

The executable JAR contains the Java runtime dependencies and complete offline Web UI. The launchers use `repoonboard.jar` beside the script in a future release layout, or `target/repoonboard.jar` in a source checkout. Set `REPOONBOARD_JAR` only to select an explicit alternate JAR.

Verify the checksum before running a copied or downloaded JAR.

Windows PowerShell:

```powershell
$expectedHash = (Get-Content .\target\repoonboard.jar.sha256).Split()[0]
$actualHash = (Get-FileHash .\target\repoonboard.jar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $expectedHash) { throw "RepoOnboard checksum mismatch" }
```

Linux:

```bash
(cd target && sha256sum -c repoonboard.jar.sha256)
```

macOS:

```bash
(cd target && shasum -a 256 -c repoonboard.jar.sha256)
```

## Usage

```text
repoonboard [OPTIONS] PATH
```

`PATH` must be the root of a supported Maven Spring Boot repository and contain its root `pom.xml`.

| Option | Meaning |
| --- | --- |
| `--no-open` | Start the local UI without opening the default browser. |
| `--profile ID[,ID...]` | Activate explicit Maven profile IDs; the option is repeatable. |
| `--local-repository PATH` | Read permitted external parent/BOM POMs from this local Maven cache. Defaults to `~/.m2/repository`. |
| `-h`, `--help` | Show command help. |
| `-V`, `--version` | Show the RepoOnboard version. |

Examples:

```bash
repoonboard /path/to/project
repoonboard . --no-open
repoonboard . --profile dev,local --local-repository /path/to/local/maven/repository
```

After analysis, RepoOnboard starts a read-only UI on a system-assigned `127.0.0.1` port. The process and UI remain available until Ctrl+C. The service accepts only its exact loopback Host and same-origin browser requests; it exposes fixed read-only routes and does not provide arbitrary file access.

### Exit codes and partial analysis

| Exit code | Meaning |
| ---: | --- |
| `0` | Analysis completed successfully. |
| `1` | Analysis failed, or the project is unsupported. |
| `2` | Command arguments are invalid. |
| `3` | Analysis is partial; confirmed facts were preserved, but coverage is incomplete. |

A missing local parent/BOM, invalid Java file, unresolved type, or unsupported pattern can produce `PARTIAL`. Review the warning code, module, repository-relative source location, and suggested action before relying on totals. CLI and UI diagnostics use the same report severity, code, stage, message, module, and source facts.

A directory without a root `pom.xml`, or a complete Maven model without recognized Spring Boot build evidence, is unsupported in V0.1 and returns `1`. When incomplete Maven data prevents Spring Boot confirmation, RepoOnboard conservatively returns `3` rather than claiming either support or failure as a fact.

## Current features

- **Repository Overview** — project identity, reported technology versions, modules, source roots, component/API counts, application entry points, and coverage status.
- **Module Explorer** — Maven aggregation hierarchy, metadata, source roots, exact-coordinate internal module dependencies, components, diagnostics, and evidence.
- **Architecture Workspace** — module-scoped confirmed Spring component-injection relationships, filters, search, one-hop focus, graph/list fallback, source evidence, and unresolved counts.
- **API Map** — Spring MVC method/path/handler facts, module/method/text filters, mapping conditions, unresolved states, and both method- and class-level evidence.
- **Source & Evidence Detail** — repository-relative path, real 1-based location, symbol, module, evidence, and exactly joined related facts; it does not expose source text or pretend to open an IDE.
- **Start Here** — deterministic file reading order with visible reasons based on build files, entry points, configuration, public APIs, and confirmed dependencies.
- **Failure-tolerant reports** — stable report IDs and schema `1.2`, explicit `SUCCESS`/`PARTIAL`/`FAILED` status, actionable diagnostics, and preservation of confirmed facts after local failures.

All production UI resources are packaged in the JAR and use no runtime CDN. RepoOnboard V0.1 does not use an LLM, upload repository contents, run the target application, execute Maven lifecycle/plugins/extensions, or modify analyzed source files.

## Supported stack

| Area | V0.1 support |
| --- | --- |
| Target language | Java source, including tested Java 8/11/17/21 syntax fixtures |
| Build system | Maven, including single/multi-module projects, explicit and `activeByDefault` profiles, relative parents, and local-cache parent/BOM POMs |
| Framework | Spring Boot build signals, common stereotype/configuration/application annotations, constructor/field injection facts, and Spring MVC mappings |
| RepoOnboard runtime | Java 21 or newer |
| Interface | Local read-only Web UI |
| Report | UTF-8 JSON schema `1.2`; readers retain compatibility with `1.0` and `1.1` |

Maven model resolution is intentionally restricted. Relative parents must stay inside the scan root; external parent/BOM POMs may only come from the selected local repository. RepoOnboard does not use Maven settings, implicit host OS/JDK/file/property profile activation, remote resolution, transitive dependency calculation, or BOM property inheritance.

Spring Boot detection uses a declared `spring-boot-starter-parent`, an imported `spring-boot-dependencies` BOM, or resolved/inherited Boot core or starter dependencies. A build signal does not prove that an application entry point exists.

## Known limitations

- V0.1 supports only Java + Maven + Spring Boot. Gradle and other languages/frameworks are not supported.
- Type resolution is limited to uniquely confirmed project-local declarations. Runtime wiring, reflection, generated registrations, proxies, and a full method-level call graph are outside V0.1.
- MyBatis/MyBatis-Plus Mapper-specific classification is deferred.
- Spring Data interfaces that only inherit a repository base type and have no direct `@Repository` are currently missed. In the fixed Petclinic validation this omitted 3 repository components and left 6 downstream injection relationships unresolved.
- Multiple wildcard imports can make a known Spring annotation conservatively ambiguous. In the fixed JHipster validation this omitted 3 controllers, 13 endpoints, and 7 confirmed dependency edges rather than inventing facts.
- The Architecture graph stops drawing when the filtered scope exceeds 60 components or 120 confirmed edges, but a graph inside that budget can still be semantically dense. The component list remains available.
- API rows and diagnostics are not virtualized or paginated. The ThingsBoard boundary trial completed 3,834 main Java files but observed 648.23 MiB peak process memory, 1,719 diagnostics, 581 API rows, ambiguous duplicate module labels, and graphs that could remain unreadable. RepoOnboard does **not** claim complete support for repositories of that scale.
- At a 1280 px viewport, medium-repository Architecture labels may clip and the API source column may require horizontal scrolling.
- First-contact validation showed that API lookup was easiest and Start Here could locate its first three files, but module, entry-point, dependency, and recommendation explanations still caused confusion. The 5–10 minute observation had no manual-reading control group, so V0.1 does not claim a measured speed-up over manual exploration.
- Native macOS/Linux release smoke checks are still pending. Current POSIX launcher behavior has been exercised from Git Bash, not claimed as full native platform certification.

## Validation evidence

- [V0.1 fixed public demo repository and walkthrough](./docs/demo/V0.1-DEMO.md)
- [Minimal deterministic Spring regression fixture](./src/test/resources/fixtures/spring-analysis-project)
- [Small repository: Spring Petclinic](./docs/validation/T-0904-SPRING-PETCLINIC.md)
- [Medium repository: JHipster Sample Application](./docs/validation/T-0905-JHIPSTER-SAMPLE-APP.md)
- [Large boundary trial: ThingsBoard](./docs/validation/T-0906-THINGSBOARD-LARGE-TRIAL.md)
- [First-contact onboarding observation](./docs/validation/T-0907-ONBOARDING-VALUE.md)

`./mvnw clean verify` runs the Java and frontend suites, builds the production UI, packages the executable JAR, starts that JAR against the controlled Spring fixture with an empty Maven cache, checks the report route, and writes the checksum. It does not build or execute the fixture application.

## Roadmap

M0–M9 plus release preparation for packaging, error messages, README, and the fixed public demo target are complete. M10 still includes demo media, a license, GitHub presentation cleanup, native cross-platform smoke checks, and the actual V0.1 release. See [TODO.md](./TODO.md) for the task-level source of truth.

V0.1 remains Web-first and will ship as an executable JAR plus Windows/POSIX launchers. A native desktop container, installer, bundled Java runtime, and selective English/中文 product-interface switch are V0.2 candidates, not V0.1 work. The language switch would translate product navigation, explanations, states, empty results, and errors while preserving code identifiers, paths, class names, APIs, framework terms, and original Evidence.

## Contributing

RepoOnboard is still in pre-release development. Before proposing a change, read [PROJECT.md](./PROJECT.md), [DECISIONS.md](./DECISIONS.md), [TODO.md](./TODO.md), and [AGENTS.md](./AGENTS.md). Keep changes focused on an accepted task, add the narrowest sufficient test, preserve source evidence, and do not expand the V0.1 ecosystem without an explicit scope decision.

Because the repository does not yet have a license, external contribution and reuse terms are not finalized. A contribution guide and license are release-preparation work; until then, use a GitHub issue to discuss a proposed contribution before submitting code.

## License

No license has been selected yet. Public source availability alone does not grant permission to copy, modify, or redistribute the project. License selection is required before V0.1 is released.

## Project philosophy

> **Understand first. Modify later.**
>
> **Static analysis before generative AI.**
>
> **Accurate before impressive.**
>
> **One ecosystem done well before many ecosystems done poorly.**
