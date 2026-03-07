package com.solubris.enforcer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.solubris.enforcer.ModelStubber.dependencyOf;
import static com.solubris.enforcer.ModelStubber.withAllTypes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VersionPropertyRuleTest {
    private final Model originalModel = new Model();
    private final Model effectiveModel = new Model();
    private final VersionPropertyRule rule = new VersionPropertyRule(originalModel, effectiveModel);

    VersionPropertyRuleTest() {
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    public void singleExplicitVersionsAllowed() {
        originalModel.addDependency(dependencyOf("junit", "junit", "4.13.2"));

        Stream<String> violations = rule.scan();

        assertThat(violations).isEmpty();
    }

    @Test
    public void singleExplicitVersionsAllowedCoveringAllTypes() {
        withAllTypes(originalModel, ModelStubber::randomVersion);

        Stream<String> violations = rule.scan();

        assertThat(violations).isEmpty();
    }

    @Test
    public void singleUseOfPropertyNotAllowed() {
        // XXX property may be defined in parent, so can't assume a property will exist
        originalModel.addProperty("junit.version", "4.13.2");
        originalModel.addDependency(dependencyOf("junit", "junit", "${junit.version}"));

        Stream<String> violations = rule.scan();

        assertThat(violations).hasSize(1);
    }

    @Test
    public void multipleUseOfPropertyAllowed() {
        originalModel.addProperty("junit.version", "4.13.2");
        originalModel.addDependency(dependencyOf("junit", "junit", "${junit.version}"));
//        project.setArtifacts(Set.of(artifactOf(dependencyOf("junit", "junit", "4.13.2"))));
        DependencyManagement depMgmt = new DependencyManagement();
        depMgmt.addDependency(dependencyOf("org.junit.jupiter", "junit-jupiter-api", "${junit.version}"));
        originalModel.setDependencyManagement(depMgmt);
        Artifact artifact = artifactOf(dependencyOf("org.junit.jupiter", "junit-jupiter-api", "4.13.2"));
//        project.setManagedVersionMap(Map.of(versionlessKey(artifact), artifact));

        Stream<String> violations = rule.scan();

        assertThat(violations).isEmpty();
    }

    private static Artifact artifactOf(Dependency dependency) {
        return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), null, "type", null, new DefaultArtifactHandler());
    }

    @Nested
    class RequirePropertiesForDuplicates {
        @Test
        public void multipleExplicitVersionsNotAllowed() {
//            rule.setRequirePropertiesForDuplicates(true);

            originalModel.addDependency(dependencyOf("junit", "junit", "4.13.2"));
            DependencyManagement depMgmt = new DependencyManagement();
            depMgmt.addDependency(dependencyOf("org.junit.jupiter", "junit-jupiter-api", "4.13.2"));
            originalModel.setDependencyManagement(depMgmt);

            Stream<String> violations = rule.scan();

            assertThat(violations).hasSize(1);
        }

        @Test
        public void multipleExplicitVersionsNotAllowedCoveringAllTypes() {
//            rule.setRequirePropertiesForDuplicates(true);

            withAllTypes(originalModel, () -> "4.13.2");

            Stream<String> violations = rule.scan();

            // TODO how to assert the multiple locations for this violation?
            assertThat(violations).hasSize(1);
        }

        @Test
        public void multipleExplicitVersionsAllowedWhenDisabled() {
//            rule.setRequirePropertiesForDuplicates(false);

            originalModel.addDependency(dependencyOf("junit", "junit", "4.13.2"));
            DependencyManagement depMgmt = new DependencyManagement();
            depMgmt.addDependency(dependencyOf("org.junit.jupiter", "junit-jupiter-api", "4.13.2"));
            originalModel.setDependencyManagement(depMgmt);

            Stream<String> violations = rule.scan();

            assertThat(violations).isEmpty();
        }
    }
}
