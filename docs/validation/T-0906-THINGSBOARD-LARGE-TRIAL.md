# T-0906 — ThingsBoard Large Repository Trial

## Validation target

- Repository: <https://github.com/thingsboard/thingsboard>
- Release: [`v4.3.1.5`](https://github.com/thingsboard/thingsboard/releases/tag/v4.3.1.5)
- Commit: [`2cd31ac359214f94b945546830da503ddf80070f`](https://github.com/thingsboard/thingsboard/commit/2cd31ac359214f94b945546830da503ddf80070f)
- License: Apache License 2.0
- Source archive SHA-256: `8B061B51306F6382E953CF23CEC321438654B9098746D1CE59F18BAE716DE22A`
- RepoOnboard starting commit: `da90ee6eb407f67bbe986db36c3e56644b64bf79`

ThingsBoard was selected as a large, production Spring Boot Maven repository.
The fixed source revision declares Java 17 and Spring Boot 3.5.15. It contains
10,021 repository files, 60 POM files, 3,834 main Java files, and 920 test Java
files. The target application was never built or executed.

Two shallow clone attempts were interrupted by the remote connection. The
trial therefore used GitHub's source archive for the exact release commit and
recorded its SHA-256 digest. This changes only the acquisition method, not the
analyzed source revision.

## Environment and procedure

- Windows, Java `21.0.6`, Maven `3.9.16`
- RepoOnboard JVM: `-Xms64m -Xmx4g`
- Maven model repository: new empty directory
- Browser viewport used for the manual UI pass: `1280 × 720`

RepoOnboard was launched from its compiled production classes with a generated
runtime classpath. The empty Maven model repository prevented external model
resolution and made missing parent, BOM, property, and version information
visible as partial coverage. The source archive was not compiled and no Maven
plugin or ThingsBoard lifecycle was run.

The final Java 21 / Maven 3.9.16 `clean verify` passed with 81 frontend tests
and 163 Java tests. The production JAR's bundled offline UI verification also
passed.

## Scale and result summary

| Fact | Observed result |
| --- | ---: |
| Repository files | 10,021 |
| POM files | 60 |
| Active reported Maven modules | 59 |
| Root direct modules | 12 |
| Main Java source files | 3,834 |
| Test Java source files, outside V0.1 scan | 920 |
| Compilation units | 3,834 |
| Declarations / indexed declarations | 4,114 / 4,114 |
| Type references | 131,061 |
| Project-local resolved type references | 37,804 |
| Report components | 848 |
| HTTP endpoints | 581 |
| Entry points | 3 |
| Report dependencies | 1,816 |
| Report diagnostics | 1,719 |
| Start Here items | 102, default view 10 |
| Analysis status | `PARTIAL` |

The 60th POM, `msa/black-box-tests/pom.xml`, is reachable only from the inactive
`black-box-tests` Maven profile. RepoOnboard reported all 59 modules in the
active aggregation tree. The difference between POM count and module count is
therefore not a discovery defect.

The 848 components consist of 810 ordinary Spring components and 38
configuration components. The ordinary set contains 2 `Controller`, 61
`RestController`, 398 `Service`, 27 `Repository`, and 322 `Component` roles.
The largest module, `application`, contains 397 components and 569 endpoints;
`dao` contains 266 components.

## Performance and memory

Cold JVM launch through local UI readiness took **25.498 seconds**. The Java
process peak working set at readiness was **663,511,040 bytes (632.77 MiB)**.
After serving the report, Start Here projection, and the manual browser pass,
the process lifetime peak was **679,714,816 bytes (648.23 MiB)**.

The serialized `/api/report` response was 8,593,622 bytes (8.20 MiB) and took
290 ms to retrieve locally in one warm request. `/api/start-here` was 687,157
bytes (671.05 KiB) and took 25 ms. These Windows observations are boundary
measurements from one run, not benchmarks or performance guarantees.

The trial completed without an out-of-memory error, Java parse failure, or
server crash under a 4 GiB heap ceiling. Its roughly 25.5-second startup and
648 MiB observed peak nevertheless show that large-repository cost is
materially above the small and medium validation targets.

## Parser and Spring semantic limits

JavaParser produced 3,834 compilation units for 3,834 discovered main Java
files: no syntax parse failure was observed. The limiting factor was semantic
coverage rather than AST parsing:

| Diagnostic | Count | Observed limit |
| --- | ---: | --- |
| `SPRING_DEPENDENCY_TARGET_UNRESOLVED` | 916 | Most injection candidates do not resolve to one confirmed project component |
| `SPRING_LOMBOK_CONSTRUCTOR_UNSUPPORTED` | 331 | Generated Lombok constructors are not reconstructed |
| `SPRING_GENERIC_INJECTION_UNSUPPORTED` | 48 | Generic injection targets remain unsupported |
| `SPRING_METHOD_INJECTION_UNSUPPORTED` | 12 | Method/setter injection remains unsupported |
| `SPRING_MVC_PATH_UNRESOLVED` | 12 | Some mapping paths cannot be reduced to a static value |
| `SPRING_MVC_CONDITION_UNRESOLVED` | 14 | Some mapping conditions cannot be reduced to a static value |

Of 1,176 injection candidates, 769 resolved to a confirmed target and none was
reported ambiguous. The report contains 219 confirmed component-injection
edges and 916 unresolved component relationships. Of 638 MVC mapping
declarations, 581 endpoints were emitted and 35 retain an unresolved path.
These figures demonstrate useful partial facts, but not complete Spring wiring
or route coverage.

## Maven model limits

The empty external model repository preserved the active 59-module aggregation
tree but generated the following limitations:

| Diagnostic | Count |
| --- | ---: |
| `MAVEN_MODEL_PROBLEM` | 221 |
| `MAVEN_PROPERTY_UNRESOLVED` | 74 |
| `MAVEN_DEPENDENCY_FIELD_UNRESOLVED` | 59 |
| `MAVEN_PROFILE_ACTIVATION_IGNORED` | 23 |
| `SPRING_BOOT_VERSION_UNRESOLVED` | 3 |

This is the expected cost of the V0.1 offline, non-executing Maven boundary on
a repository that relies heavily on inherited and profile-dependent model
data. The module graph remains useful, but dependency metadata is not a fully
effective Maven model.

## Report model limits

Six `REPORT_ID_COLLISION` diagnostics were retained rather than dropping
entities: three duplicate Maven dependency identities, two Endpoint identities,
and one component identity. `CassandraDriverOptions` is both a Spring
`Component` and `Configuration`; the report preserves both roles using a
collision suffix, but large-repository users cannot infer that distinction from
the stable ID alone.

The Overview dependency headline calls all 1,816 report dependencies
"Component relationships", although this total also contains Maven/internal
module dependency entities. That wording is a presentation limit, not a loss
of report facts.

The 1,719 diagnostics preserve the reasons for partial coverage, but their raw
volume is not a practical triage experience. Overview summarizes codes and a
small sample; it does not rank or group individual instances into an actionable
large-repository workflow.

## UI and graph usability limits

Overview, Architecture, API Map, and Start Here were inspected against the
live report. Browser console warnings and errors: **0**.

- Selecting the `application` module yields 397 components, 66 confirmed
  relations, and 443 unresolved relations. The 60-node graph budget correctly
  pauses drawing and leaves the searchable component list available.
- The `queue` module is under the numeric budget at 52 components and 77
  confirmed relations, yet its default graph is still a dense hairball with
  clipped outer labels. A numeric threshold prevents oversized rendering but
  does not guarantee semantic readability.
- Selecting `KafkaMonolithQueueFactory` and enabling one-hop focus reduces the
  queue view to 16 of 52 components, 17 confirmed relations, and 2 unresolved
  targets. Structure becomes inspectable and the Inspector exposes all 15
  outgoing targets, although some outer graph labels remain clipped at this
  viewport.
- Module selectors repeat leaf names such as `coap`, `http`, `mqtt`, `snmp`,
  and `transport` without their paths. This is ambiguous in a deeply nested
  multi-module repository.
- API Map renders all 581 endpoint rows in the document at once. Filtering,
  handler Evidence, and coverage counts remain usable, but the unpaginated
  full list is a scalability limit.
- Start Here's progressive disclosure works: 10 of 102 recommendations appear
  by default and the remaining 92 are explicit. The deterministic ordering and
  Evidence Inspector remain usable.

## Conclusion

T-0906 passes as a boundary trial. RepoOnboard completed an exact 3,834-source,
59-active-module revision without crashing, retained partial facts and source
evidence, and protected the largest graph from misleading truncation. The trial
also establishes concrete limits: approximately 25.5 seconds to UI readiness,
648.23 MiB observed process peak, incomplete offline Maven/Spring semantic
resolution, 1,719 diagnostics with weak large-scale triage, ambiguous repeated
module labels, an unpaginated 581-row API list, and graphs that can remain
unreadable below the current numeric budget.

This record does **not** claim complete support for ThingsBoard or repositories
of similar size. These limitations are release documentation inputs; they do
not expand V0.1 scope or authorize speculative fixes inside T-0906.
