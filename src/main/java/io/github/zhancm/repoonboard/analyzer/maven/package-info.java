/**
 * Maven project and build-metadata analysis.
 *
 * <p>This boundary produces Maven facts and diagnostics without interpreting
 * Java source or Spring component semantics. Project detection checks only for
 * a root-level {@code pom.xml}; parsing is a separate analysis step.</p>
 */
package io.github.zhancm.repoonboard.analyzer.maven;
