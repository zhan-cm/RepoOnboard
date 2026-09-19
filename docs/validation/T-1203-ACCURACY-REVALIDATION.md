# T-1203 — Analyzer Accuracy Revalidation

## Scope

M12 closes two false-negative classes recorded during V0.1 validation:

- Spring Data repository interfaces that directly inherit a confirmed, well-known
  repository base without declaring `@Repository`.
- Spring annotations imported through multiple wildcard packages when exactly one
  known Spring qualified name matches the annotation.

The implementation remains source-only and conservative. It does not load Spring
classes, execute Maven, build or run the target application, or infer a result when
multiple known candidates remain.

## Controlled fixture

`spring-accuracy-project` combines both cases in one deterministic report:

- `OwnerRepository extends Repository<Owner, Long>` has no annotation.
- `OwnerController` imports both `java.util.*` and
  `org.springframework.web.bind.annotation.*`.
- The report contains 3 components, 1 endpoint and 3 confirmed component-injection
  dependencies.
- Repository Evidence is located on the exact `extends Repository` use and retains
  rule ID
  `spring.component.repository_inheritance:org.springframework.data.repository.Repository`.
- Two runs produce equal reports, no Diagnostic, no fixture build output and no
  source change.

Unit coverage also verifies that direct parent-type facts preserve source location,
resolve only unique project-local declarations, accept one known Spring wildcard
candidate, reject ambiguous explicit imports, ignore classes that implement a
repository interface, and ignore unrelated interfaces named `Repository`.

## Fixed real repositories

Both targets were fetched at their previously audited commits. RepoOnboard used a
new empty Maven model repository for each run; the target repositories were not
modified, built or executed.

| Target | Commit | Before M12 | After M12 |
| --- | --- | --- | --- |
| Spring Petclinic | `818c4136ea971c21674525f9053de0d9c7ad8cfe` | 10 components; 0 confirmed / 6 unresolved component edges | 13 components; 6 confirmed / 0 unresolved component edges |
| JHipster Sample Application | `e06e87abe0be8a3a194381ce651164a734811b3f` | 30 components; 24 endpoints; 7 confirmed / 9 unresolved component edges | 33 components; 38 endpoints; 14 confirmed / 10 unresolved component edges |

### Spring Petclinic

The report remains `PARTIAL` because the empty Maven repository cannot provide the
external parent. All three expected repositories are now present with inheritance
Evidence at the source clause:

- `OwnerRepository` through `JpaRepository` at line 36.
- `PetTypeRepository` through `JpaRepository` at line 30.
- `VetRepository` through `Repository` at line 38.

All six previously unresolved constructor injections now resolve to those confirmed
components. The six `SPRING_DEPENDENCY_TARGET_UNRESOLVED` diagnostics disappeared;
only the four previously expected Maven-boundary diagnostics remain. The 17 Endpoint
facts are unchanged.

### JHipster Sample Application

`AccountResource`, `PublicUserResource` and `UserResource` are now confirmed as
`REST_CONTROLLER`. Their seven project-local constructor injections are confirmed,
while one newly visible external injection remains unresolved. The three
`SPRING_COMPONENT_ANNOTATION_AMBIGUOUS` diagnostics disappeared; Maven-boundary,
unsupported method-injection and genuine external-target diagnostics remain.

The corrected total is **38 endpoints**, not 37. The V0.1 audit counted 13 missed
method-level mappings, but `UserResource.updateUser` declares two paths in one
`@PutMapping`, producing two distinct final method/path Endpoint facts. The three
controllers therefore restore 14 Endpoint facts: 7 from `AccountResource`, 1 from
`PublicUserResource`, and 6 from `UserResource`. Source inspection confirms all 14;
the previous figure was an audit undercount rather than a new false positive.

## Result

M12 satisfies its exit gate. The measured false negatives are closed, the controlled
fixture is stable and traceable, the two fixed real-repository results match source,
and no speculative component or dependency edge was introduced. Direct inheritance
is intentionally limited to a conservative set of well-known Spring Data core and
JPA repository bases; arbitrary project-local intermediate repository hierarchies
remain outside this milestone.
