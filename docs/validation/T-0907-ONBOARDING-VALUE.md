# T-0907 — Onboarding Value Validation

Date: 2026-09-12

## Outcome

The first-contact session produced a mixed result rather than evidence for a broad "faster onboarding" claim.

- The participant found the requested API task fastest.
- The participant found the first three Start Here recommendations, which is observable value for reading-order discovery.
- The participant was confused by the entry-point, module, and dependency tasks.
- The participant could not discover why the Start Here files were recommended, even though the reasons existed in the projection.
- Total elapsed time was reported as approximately 5–10 minutes. Per-task stopwatch timings were not captured.
- There was no manual-source-reading control run, so this session does not prove a time advantage over manual exploration.

T-0907 therefore records partial onboarding value and concrete usability failures. It must not be cited as proof that RepoOnboard already makes every unfamiliar developer faster.

## Reproducible Target

Repository: `joyheros/realworld`

Repository URL: <https://github.com/joyheros/realworld>

Commit: `2d944dbce5e89c6efdf59026f9b38ba014e73288`

Commit URL: <https://github.com/joyheros/realworld/commit/2d944dbce5e89c6efdf59026f9b38ba014e73288>

Source archive SHA-256:

```text
CA35F713331F39F1DA34716C27340137A1E4B9014C099F847048CDB0020E63E2
```

Environment:

```text
OS: Windows 11
Java: 21.0.6
Maven: 3.9.16
RepoOnboard report schema: 1.2
Maven model repository: empty local directory
Target application build/run: not performed
```

The analyzed snapshot produced a `PARTIAL` report with 5 modules, 62 main Java source files, 20 components, 19 endpoints, 1 application entry point, 62 dependencies, and 40 diagnostics. The partial status and diagnostics remained visible during the session.

## Fixed Task Protocol

The participant was asked to use only the RepoOnboard UI and report:

1. The application entry class, its module, and source path.
2. The four child Maven modules and which one contains the application entry.
3. The handler, module, and source path for `POST /api/articles`.
4. Whether `UserApi → UserServiceImpl` is a confirmed component dependency; if not, identify the limitation shown by RepoOnboard.
5. The first three Start Here files and every displayed recommendation reason.

The participant was then asked for total/per-task time, confidence, confusion points, and the most helpful page.

## Answer Key

1. `top.ruilink.realworld.RealworldApplication`, module `app-main`, at `app-main/src/main/java/top/ruilink/realworld/RealworldApplication.java:6`.
2. `app-article`, `app-common`, `app-main`, and `app-permission`; the entry point is in `app-main`.
3. Handler `createArticle`, module `app-article`, at `app-article/src/main/java/top/ruilink/realworld/article/application/ArticleApi.java:39`.
4. The edge is not confirmed. `UserApi` injects the `UserService` interface, but the current conservative resolver does not connect that interface to `UserServiceImpl`; the UI reports an unresolved dependency instead of inventing an edge.
5. `pom.xml` — `Root Maven build file`; `RealworldApplication.java` — `Application entry point` and `Configuration component`; `WebSecurityConfig.java` — `Configuration component`.

## Observations

| Task | Participant outcome | Correctness evidence | Observation |
| --- | --- | --- | --- |
| 1. Application entry | Not confidently completed | No answer was submitted | The participant described the non-API tasks as confusing. |
| 2. Module structure | Not confidently completed | No answer was submitted | The module page did not give the participant an obvious route from the question to the answer. |
| 3. Key API | Reported as the fastest task | Exact handler/module/path were not transcribed, so correctness was not independently scored | API Map offered the clearest search target in this session. |
| 4. Dependency status | Not confidently completed | No answer was submitted | An unresolved interface-to-implementation relationship is conceptually harder than locating a concrete API row. |
| 5. Start Here | First three files found; reasons not found | Reading-order discovery was observed, but reason discovery failed | The card contained an unlabeled primary reason, while the complete `Why this file?` inspector moved below the whole list at the participant's viewport. |

Participant feedback, translated only for this record:

- Overview was dark while the other pages were white; the participant preferred white.
- Task 3 was the fastest to find; the other tasks felt confusing.
- English may have contributed to the confusion, but the participant did not request a full Chinese conversion in M9.
- The first three recommended files were found, but the recommendation reasons were not discoverable.

## Adjustments Made in T-0907

Two directly observed UI defects were corrected without adding new analysis capability:

1. Overview now uses the same light workbench theme as Modules, Architecture, APIs, and Start Here. Its repository top bar shows module and source-file context, and inherited text colors were corrected for light-background contrast.
2. Every Start Here card now labels `Why this file?` and renders all recorded reasons inline. The secondary action is labelled `View evidence` so the below-fold inspector is no longer the only discoverable explanation surface.

The changes retain the original deterministic reason messages, code identifiers, paths, API values, framework terms, and Evidence. Full product localization was not added. The already recorded V0.2 English/中文 Settings candidate remains the appropriate place to evaluate selective UI translation.

## Verification After Adjustment

- Maven 3.9.16 `verify`: passed.
- Frontend unit suite: 18 files, 81 tests passed.
- Java suite: 163 tests passed.
- Frontend production build and packaged-JAR UI verification: passed.
- Browser verification at the participant-sized viewport confirmed a light Overview with readable dark text.
- Browser verification confirmed that the first three Start Here cards directly expose all answer-key reasons without opening or scrolling to the inspector.
- The participant did not perform a second timed session, so the post-adjustment checks establish behavior, not an improved completion-time measurement.

## Product Conclusion

Start Here showed limited but real value: the participant found the recommended reading sequence, but its explanation failed the first-contact discoverability test before the T-0907 adjustment. API Map was the clearest successful surface. The broader onboarding proposition remains unproven because most tasks were not confidently completed, exact task answers were not collected, per-task timings were absent, and no manual baseline was run.

M10 documentation and demos should describe the confirmed capabilities and this mixed validation result accurately. They should not claim a measured speed-up over manual source reading.
