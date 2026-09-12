# Partial multi-module analysis fixture

Read-only Maven input; do not build or execute it.

The root aggregates two children. One declares an unavailable external parent
with local relative lookup disabled; the other imports an unavailable external
BOM. An empty configured local repository must leave both children available as
raw module facts while the overall analysis reports `PARTIAL` with located
diagnostics.
