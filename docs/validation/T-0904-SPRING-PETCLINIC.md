# T-0904 — Spring Petclinic Validation

## Validation target

- Repository: <https://github.com/spring-projects/spring-petclinic>
- Commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Commit date: `2026-08-26T11:57:54+01:00`
- License: Apache License 2.0
- RepoOnboard commit: `5556b13ca4fb050b8e72b4d0a5410827ce4dbe08`

Spring Petclinic was selected because it is a canonical, compact Spring Boot
Maven application with direct Spring MVC annotations and Spring Data repository
interfaces. The fixed revision contains one Maven module and 30 main Java source
files. The target application was never built or executed.

## Environment and procedure

- Windows, Java `21.0.6`, Maven `3.9.16`
- RepoOnboard JVM: `-Xms32m -Xmx512m`
- Maven model repository: new empty directory
- Browser viewport used for the manual UI pass: `1280 × 720`

The repository was shallow-cloned, the exact commit was verified, and
RepoOnboard was launched from its compiled production classes with a generated
runtime classpath:

```powershell
git clone --depth 1 https://github.com/spring-projects/spring-petclinic.git target/t-0904/spring-petclinic
git -C target/t-0904/spring-petclinic rev-parse HEAD
mvn dependency:build-classpath -Dmdep.outputFile=target/t-0904/runtime-classpath.txt -DincludeScope=runtime
java -Xms32m -Xmx512m -cp "target/classes;<runtime-classpath>" io.github.zhancm.repoonboard.cli.RepoOnboardCommand target/t-0904/spring-petclinic --local-repository target/t-0904/empty-model-repository --no-open
```

The empty Maven model repository is intentional: it makes the offline result
reproducible and verifies that missing external parent metadata yields partial
facts rather than network access or target execution.

## Result summary

| Fact | Reported result |
| --- | --- |
| Analysis status | `PARTIAL` |
| Maven modules | 1 |
| Main Java source files | 30 |
| Report components | 10 |
| Controllers | 6 |
| Configurations | 3 |
| Other components | 1 |
| HTTP endpoints | 17 |
| Maven dependency declarations | 28 |
| Component injection dependencies | 6 unresolved, 0 confirmed |
| Start Here items | 10 |

Project detection correctly reported `org.springframework.samples:spring-petclinic:4.0.0-SNAPSHOT`,
`jar` packaging, Java 17, Spring Boot 4.1.0, `src/main/java`, and the single root
module. The 30th and 29th raw `<dependency>` elements in `pom.xml` are plugin
dependencies, so excluding them from the 28 project dependency facts is correct.

## Manual fact audit

### Components

All ten directly annotated application types were compared with source. The
report contained the six `@Controller` classes, the two direct `@Configuration`
classes, the `@SpringBootApplication` configuration, and the `@Component`
formatter with correct files and 1-based lines.

Representative samples:

| Source fact | Expected | Reported |
| --- | --- | --- |
| `PetClinicApplication.java:28` | `CONFIGURATION` and application entry point | Correct |
| `OwnerController.java:48` | `CONTROLLER` | Correct |
| `PetTypeFormatter.java:36` | `COMPONENT` | Correct |
| `CacheConfiguration.java:31` | `CONFIGURATION` | Correct |
| `VetController.java:35` | `CONTROLLER` | Correct |

The three Spring Data interfaces `OwnerRepository`, `PetTypeRepository`, and
`VetRepository` are runtime repository beans but have no direct `@Repository`
annotation. RepoOnboard did not classify them as components. These are three
known component false negatives under the current direct-annotation rule.

### Endpoints

The source has 17 method-level Spring MVC mappings and one type-level prefix.
All 17 final method/path combinations were present, with no unresolved paths.
The type-level `/owners/{ownerId}` prefix on `PetController` was composed
correctly.

| Source fact | Expected endpoint | Reported |
| --- | --- | --- |
| `WelcomeController.java:25` | `GET /` | Correct |
| `CrashController.java:31` | `GET /oups` | Correct |
| `OwnerController.java:72` | `GET /owners/new` | Correct |
| `OwnerController.java:77` | `POST /owners/new` | Correct |
| `PetController.java:100` plus type prefix | `GET /owners/{ownerId}/pets/new` | Correct |
| `VisitController.java:97` | `POST /owners/{ownerId}/pets/{petId}/visits/new` | Correct |
| `VetController.java:65` | `GET /vets` | Correct |

No endpoint false positive or false negative was found in the complete mapping
set at this revision.

### Dependencies

All six single-constructor injection sites were detected with the correct source
component, declared target type, location, and `CONSTRUCTOR_INJECTION` Evidence:

- `OwnerController` → `OwnerRepository`
- `PetController` → `OwnerRepository`
- `PetController` → `PetTypeRepository`
- `PetTypeFormatter` → `PetTypeRepository`
- `VisitController` → `OwnerRepository`
- `VetController` → `VetRepository`

Because the three Spring Data targets were not classified as components, all six
edges were conservatively retained as `UNRESOLVED`; no target ID or confirmed
Architecture edge was invented. There were no dependency false positives. The
six runtime injection edges are known confirmed-edge false negatives caused by
the three repository component false negatives.

## Diagnostics

The `PARTIAL` result contained 10 significant diagnostics:

| Code | Count | Assessment |
| --- | ---: | --- |
| `MAVEN_MODEL_PROBLEM` | 1 | Expected with the external parent absent from the empty local repository |
| `MAVEN_POM_SOURCE_OUTSIDE_ALLOWED_ROOT` | 1 | Expected local-only boundary signal for the external parent |
| `MAVEN_PROFILE_ACTIVATION_IGNORED` | 1 | Expected for the host-dependent `m2e` profile |
| `MAVEN_DEPENDENCY_FIELD_UNRESOLVED` | 1 | Expected aggregate warning for versions inherited from the unavailable parent |
| `SPRING_DEPENDENCY_TARGET_UNRESOLVED` | 6 | Accurate consequence of unsupported Spring Data repository discovery |

The diagnostics explain every observed coverage limitation; no silent parse
failure or whole-analysis abort occurred.

## Performance

Cold JVM launch through local UI readiness took **9.302 seconds**. The Java
process lifetime peak working set at readiness was **136,458,240 bytes
(130.14 MiB)**. This is a single Windows observation, not a benchmark; it includes
JVM startup, static analysis, report assembly, Start Here projection, and local
server startup.

## UI readability

The Overview, Modules, Architecture, APIs, and Start Here pages were inspected
against the live report. Browser console warnings and errors: **0**.

- Overview clearly exposes `PARTIAL`, all headline counts, and the five coverage
  limitation codes.
- Modules keeps the one-module structure and technology facts understandable;
  long coordinates wrap but remain available.
- Architecture explicitly shows 10 components, 0 confirmed relations, and 6
  unresolved relations. It does not draw false edges. The initial horizontal row
  clips outer nodes; Fit view reveals all nodes but makes labels very small. This
  is a non-blocking readability limitation to re-check during T-0905.
- APIs makes methods, paths, handlers, and filters readable. At 1280 px the source
  column requires horizontal scrolling, while selection/Inspector still provides
  evidence. This is a non-blocking compact-desktop limitation.
- Start Here clearly shows the partial-coverage warning and a deterministic
  ten-file sequence beginning with `pom.xml`, the application entry point, and
  configuration files before endpoint-bearing controllers.

## Conclusion

T-0904 passes. Project detection, direct component facts, all endpoint facts,
diagnostics, and partial-result behavior are accurate for the fixed revision.
Spring Data repository discovery and its six downstream confirmed dependency
edges are measurable false negatives, but uncertainty is represented honestly
and no false facts enter Architecture or Start Here. The two UI density findings
are usable, non-blocking observations for the medium-repository validation.
