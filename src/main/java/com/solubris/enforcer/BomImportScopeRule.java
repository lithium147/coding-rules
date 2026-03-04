package com.solubris.enforcer;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A custom Maven Enforcer rule that ensures all dependencies declared with
 * {@code type=pom} (i.e. BOMs) also specify {@code scope=import}.
 *
 * <p>This rule inspects both the top-level {@code <dependencies>} section and
 * the {@code <dependencyManagement>} section of the project. Any dependency
 * with {@code type=pom} that does not have {@code scope=import} will cause
 * a build failure.
 *
 * <p>BOMs (Bill of Materials) are intended to be imported via
 * {@code <scope>import</scope>} inside {@code <dependencyManagement>}.
 * Using them without import scope is almost always a mistake and can lead
 * to unexpected transitive dependency resolution.
 */
@Named("bomImportScopeRule")
public class BomImportScopeRule extends AbstractEnforcerRule {

    private static final String POM_TYPE = "pom";
    private static final String IMPORT_SCOPE = "import";

    private final Model model;

    @SuppressWarnings("unused")
    @Inject
    public BomImportScopeRule(MavenSession session) {
        this(modelFrom(session));
    }

    protected BomImportScopeRule(Model model) {
        this.model = model;
    }

    private static Model modelFrom(MavenSession session) {
        MavenProject project = session.getCurrentProject();
        Model originalModel = project.getOriginalModel();
        return originalModel != null ? originalModel : project.getModel();
    }

    @Override
    public void execute() throws EnforcerRuleException {
        List<String> violations = scanAll().collect(Collectors.toList());

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> "  - " + v)
                    .collect(Collectors.joining("\n",
                            "BOM dependencies (type=pom) must use scope=import. "
                                    + "Found " + violations.size() + " violation(s):\n",
                            "\n"));
            throw new EnforcerRuleException(message);
        }

        getLog().info("All BOM dependencies (type=pom) correctly use scope=import.");
    }

    protected Stream<String> scanAll() {
        Stream<String> topLevelViolations = directDependencies()
                .filter(dep -> POM_TYPE.equals(dep.getType()))
                .map(dep -> formatViolation(dep, "dependencies"));

        Stream<String> depMgmtViolations = managedDependencies()
                .filter(dep -> POM_TYPE.equals(dep.getType()))
                .filter(dep -> !IMPORT_SCOPE.equals(dep.getScope()))
                .map(dep -> formatViolation(dep, "dependencyManagement"));

        return Stream.concat(topLevelViolations, depMgmtViolations);
    }

    private Stream<Dependency> directDependencies() {
        return Optional.ofNullable(model.getDependencies())
                .stream()
                .flatMap(Collection::stream);
    }

    private Stream<Dependency> managedDependencies() {
        return Optional.ofNullable(model.getDependencyManagement())
                .map(DependencyManagement::getDependencies)
                .stream()
                .flatMap(Collection::stream);
    }

    private static String formatViolation(Dependency dep, String section) {
        String scope = dep.getScope();
        String scopeInfo = (scope == null || scope.isEmpty()) ? "no scope" : "scope=" + scope;
        return dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion()
                + " in <" + section + "> has type=pom but " + scopeInfo
                + " (expected scope=import)";
    }
}
