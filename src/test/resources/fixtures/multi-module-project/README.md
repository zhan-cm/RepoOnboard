# Multi-module analysis fixture

Read-only Maven input; do not build or execute it.

`workspace` aggregates `api` and `library`. Only `api` inherits the root parent;
`library` has independent coordinates. The API source directory preserves
`${code.directory}` and resolves to `api/code/java` relative to the scan root.
Source directories intentionally need not exist during metadata analysis.
