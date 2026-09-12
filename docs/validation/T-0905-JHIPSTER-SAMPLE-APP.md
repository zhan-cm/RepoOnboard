# T-0905 — JHipster Sample Application Validation

## Validation target

- Repository: <https://github.com/jhipster/jhipster-sample-app>
- Commit: `e06e87abe0be8a3a194381ce651164a734811b3f`
- Commit date: `2026-08-30T17:19:50-06:00`
- License: Apache License 2.0
- RepoOnboard starting commit: `544592719c855fc937b047a2de00a29be522a6bd`

The JHipster sample application was selected as a medium, single-module Spring
Boot Maven repository with security, configuration, service, repository, and
REST layers. The fixed revision contains 81 main Java source files and 601
repository files. The target application was never built or executed.

## Environment and procedure

- Windows, Java `21.0.6`, Maven `3.9.16`
- RepoOnboard JVM: `-Xms32m -Xmx1g`
- Maven model repository: new empty directory
- Browser viewport used for the manual UI pass: `1280 × 720`

The repository was shallow-cloned, the exact commit was verified, and
RepoOnboard was launched from its compiled production classes with a generated
runtime classpath:

```powershell
git clone --depth 1 https://github.com/jhipster/jhipster-sample-app.git target/t-0905/jhipster-sample-app
git -C target/t-0905/jhipster-sample-app rev-parse HEAD
mvn dependency:build-classpath -Dmdep.outputFile=target/t-0905/runtime-classpath.txt -DincludeScope=runtime
java -Xms32m -Xmx1g -cp "target/classes;<runtime-classpath>" io.github.zhancm.repoonboard.cli.RepoOnboardCommand target/t-0905/jhipster-sample-app --local-repository target/t-0905/empty-model-repository --no-open
```

The empty Maven model repository keeps the run offline and makes incomplete
external parent/BOM metadata visible as partial coverage rather than allowing
network resolution or target execution.

## Failure-tolerance defect and fix

The first run aborted before producing a report. The target POM contains valid
empty properties such as `<profile.api-docs/>` and
`<liquibase-plugin.password/>`; `MavenProjectMetadataReader` passed their blank
values to `MavenMetadataValue`, whose public contract rejects present-but-blank
raw values.

The reader now omits blank property metadata values while retaining their raw
empty string in the internal interpolation map. A focused regression proves
that `<empty.flag/>` is not emitted as a metadata value and that
`dev${empty.flag}` still resolves to `dev`. The same fixed build completed the
full repository analysis and served the local UI. This was a release-blocking
failure-tolerance defect and is fixed by T-0905.

The final Java 21 / Maven 3.9.16 `clean verify` passed with 81 frontend tests
and 163 Java tests. The production JAR's bundled offline UI verification also
passed.

## Result summary

| Fact | Reported result |
| --- | --- |
| Analysis status | `PARTIAL` |
| Maven modules | 1 |
| Main Java source files | 81 |
| Report components | 30 |
| Controllers | 5 |
| Services | 5 |
| Repositories | 5 |
| Configurations | 14 |
| Other components | 1 |
| HTTP endpoints | 24 |
| Maven dependency declarations | 49 |
| Component injection dependencies | 7 confirmed, 9 unresolved |
| Start Here items | 24, default view 10 |

Project detection correctly reported
`io.github.jhipster.sample:jhipster-sample-application:0.0.1-SNAPSHOT`, `jar`
packaging, Spring Boot build signals, `src/main/java`, and the single root
module. The unavailable external parent leaves some inherited versions
unresolved, so `PARTIAL` is the correct status.

## Manual fact audit

### Components

All 30 reported components were compared with direct source annotations. They
are true positives: five REST controllers, five services, five repositories,
thirteen direct configurations, one `@SpringBootApplication` configuration,
and one component.

The source contains three additional direct `@RestController` classes:
`AccountResource`, `PublicUserResource`, and `UserResource`. Each imports both
`java.util.*` and `org.springframework.web.bind.annotation.*`. The conservative
annotation resolver treats multiple wildcard imports as ambiguous even though
only the Spring wildcard can provide `RestController`. These are three known
component false negatives, each accompanied by
`SPRING_COMPONENT_ANNOTATION_AMBIGUOUS`; no component false positive was found.

### Endpoints

The 24 reported endpoints were checked against their method-level mappings and
class prefixes in the five confirmed controllers. Their methods, final paths,
handlers, and source locations are correct; no reported endpoint was false.

The three ambiguous controllers contain 13 additional method-level mappings:
seven in `AccountResource`, one in `PublicUserResource`, and five in
`UserResource`. Because MVC analysis intentionally starts from confirmed
components, these are 13 known endpoint false negatives caused by the component
classification limitation. The complete source truth set is 37 endpoints.

### Dependencies

All seven confirmed component edges match constructor-injected project types:

- `BankAccountResource` → `BankAccountRepository`
- `OperationResource` → `OperationRepository`
- `LabelResource` → `LabelRepository`
- `AuthorityResource` → `AuthorityRepository`
- `DomainUserDetailsService` → `UserRepository`
- `UserService` → `AuthorityRepository`
- `UserService` → `UserRepository`

The nine unresolved targets are external framework types rather than confirmed
project components: `MeterRegistry`, `MessageSource`, `JavaMailSender`,
`SpringTemplateEngine`, `JHipsterProperties`, `CacheManager`, `PasswordEncoder`,
`JwtEncoder`, and `AuthenticationManagerBuilder`. They are correctly retained
as unresolved and never drawn as confirmed Architecture edges.

The three missed controllers also contain seven project-local constructor
injections to `UserRepository`, `UserService`, and `MailService`. These are
known confirmed-edge false negatives caused by the same wildcard-import
component limitation. No dependency false positive was found.

## Diagnostics and noise

The `PARTIAL` result contained 19 significant diagnostics:

| Code | Count | Assessment |
| --- | ---: | --- |
| `MAVEN_MODEL_PROBLEM` | 1 | Expected with the external parent absent from the empty local repository |
| `MAVEN_PROFILE_ACTIVATION_IGNORED` | 1 | Expected for the host-dependent `eclipse` profile |
| `MAVEN_PROPERTY_UNRESOLVED` | 1 | Expected inherited Spring Boot version limitation |
| `MAVEN_DEPENDENCY_FIELD_UNRESOLVED` | 2 | Accurate unresolved dependency-version facts |
| `SPRING_COMPONENT_ANNOTATION_AMBIGUOUS` | 3 | Accurate resolver uncertainty, but exposes the measured wildcard-import false negatives |
| `SPRING_METHOD_INJECTION_UNSUPPORTED` | 2 | Accurate unsupported setter/method injection notices |
| `SPRING_DEPENDENCY_TARGET_UNRESOLVED` | 9 | Accurate external or otherwise unconfirmed injection targets |

No duplicate, irrelevant, or factually incorrect diagnostic was found. The
three wildcard-import warnings are high-value coverage signals rather than
noise, although the underlying false-negative limitation remains relevant for
V0.1 release notes.

## Performance

Cold JVM launch through local UI readiness took **9.761 seconds**. The Java
process lifetime peak working set at readiness was **190,345,216 bytes
(181.53 MiB)**. This is one Windows observation, not a benchmark; it includes
JVM startup, analysis of 81 Java files, report assembly, Start Here projection,
and local server startup.

## UI and graph readability

Overview, Architecture, and Start Here were inspected against the live report.
Browser console warnings and errors: **0**.

- The default 30-node Architecture graph fits within the configured graph
  budget but is visually dense; outer labels are clipped at this viewport.
- Hiding 14 configurations produces 16 shown / 14 filtered while keeping all
  seven confirmed relations and nine unresolved relations visible in the page
  summary and Inspector.
- Restricting the graph to REST Controller, Service, and Repository shows 15 of
  30 components and explicitly reports 15 hidden.
- Selecting `UserService` and enabling one-hop focus produces a readable graph
  of three components and two confirmed relations. The page explicitly reports
  27 filtered components and two unresolved targets in the focused scope.
- Start Here displays 10 of 24 recommendations by default and explicitly offers
  the 14 hidden recommendations; the partial-coverage warning remains visible.

The default medium graph should not be treated as presentation-ready without
filtering, but the existing kind and one-hop controls satisfy the task's
readability requirement without truncating facts or hiding coverage counts.

## Conclusion

T-0905 passes. The fixed revision and environment are reproducible, the empty
Maven-property crash is fixed with regression coverage, and no critical false
positive was found. Reported components, endpoints, and confirmed dependencies
were accurate. Wildcard-import resolution causes measurable false negatives
(3 components, 13 endpoints, and 7 confirmed component edges), but each affected
file is surfaced through an explicit ambiguity diagnostic rather than guessed.
The medium graph is dense by default and becomes readable through deterministic
type and one-hop filtering with visible hidden and unresolved counts.
