/**
 * Maven project and build-metadata analysis.
 *
 * <p>This boundary produces Maven facts and diagnostics without interpreting
 * Java source or Spring component semantics. Current parsing preserves raw and
 * resolved field values, source locations, resolution state, and structured
 * diagnostics. Spring Boot build detection in this package is based only on
 * traceable Maven parent, BOM, and dependency evidence; source-level Spring
 * semantics belong to the Spring analyzer.</p>
 */
package io.github.zhancm.repoonboard.analyzer.maven;
