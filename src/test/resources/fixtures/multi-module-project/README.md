# Multi-module analysis fixture

Read-only Maven input; do not build or execute it.

`workspace` aggregates `api` and `library`. Only `api` inherits the root parent;
`library` has independent coordinates, and `api` declares an exact dependency
on those coordinates so report assembly can verify an internal-module edge.
The API source directory preserves `${code.directory}` and resolves to
`api/code/java` relative to the scan root. `ApiType` references `LibraryType`
through that declared dependency so the regression suite can verify source
ownership and compile-visible cross-module type resolution without building the
fixture.
