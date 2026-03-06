package com.solubris.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Build;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Reporting;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.solubris.enforcer.ModelStubber.dependencyOf;
import static com.solubris.enforcer.ModelStubber.extensionOf;
import static com.solubris.enforcer.ModelStubber.pluginOf;
import static com.solubris.enforcer.ModelStubber.reportPluginOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Test cases for UnusedPropertyRule enforcer rule.
 */
class UnusedPropertyRuleTest {

    @Test
    void noPropertiesPasses() {
        Model originalModel = new Model();
        Model effectiveModel = new Model();

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        assertThatNoException().isThrownBy(rule::execute);
    }

    @Test
    void versionPropertyUsedByDirectDependencyPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("junit.version", "5.9.3");
        originalModel.addDependency(dependencyOf("org.junit", "junit", "${junit.version}"));

        Model effectiveModel = new Model();
        effectiveModel.addDependency(dependencyOf("org.junit", "junit", "5.9.3"));

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void versionPropertyUsedByManagedDependencyPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("junit.version", "5.9.3");
        DependencyManagement originalDepMgmt = new DependencyManagement();
        originalDepMgmt.addDependency(dependencyOf("org.junit", "junit", "${junit.version}"));
        originalModel.setDependencyManagement(originalDepMgmt);

        Model effectiveModel = new Model();
        DependencyManagement effectiveDepMgmt = new DependencyManagement();
        effectiveDepMgmt.addDependency(dependencyOf("org.junit", "junit", "5.9.3"));
        effectiveModel.setDependencyManagement(effectiveDepMgmt);

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void versionPropertyUsedByPluginPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("compiler.version", "3.13.0");
        Build originalBuild = new Build();
        originalBuild.addPlugin(pluginOf("org.apache.maven.plugins", "maven-compiler-plugin", "${compiler.version}"));
        originalModel.setBuild(originalBuild);

        Model effectiveModel = new Model();
        Build effectiveBuild = new Build();
        effectiveBuild.addPlugin(pluginOf("org.apache.maven.plugins", "maven-compiler-plugin", "3.13.0"));
        effectiveModel.setBuild(effectiveBuild);

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void versionPropertyUsedByManagedPluginPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("compiler.version", "3.13.0");
        Build originalBuild = new Build();
        PluginManagement originalPluginMgmt = new PluginManagement();
        originalPluginMgmt.addPlugin(pluginOf("org.apache.maven.plugins", "maven-compiler-plugin", "${compiler.version}"));
        originalBuild.setPluginManagement(originalPluginMgmt);
        originalModel.setBuild(originalBuild);

        Model effectiveModel = new Model();
        Build effectiveBuild = new Build();
        PluginManagement effectivePluginMgmt = new PluginManagement();
        effectivePluginMgmt.addPlugin(pluginOf("org.apache.maven.plugins", "maven-compiler-plugin", "3.13.0"));
        effectiveBuild.setPluginManagement(effectivePluginMgmt);
        effectiveModel.setBuild(effectiveBuild);

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void versionPropertyUsedByExtensionPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("ext.version", "1.0.0");
        Build originalBuild = new Build();
        originalBuild.addExtension(extensionOf("org.example", "my-extension", "${ext.version}"));
        originalModel.setBuild(originalBuild);

        Model effectiveModel = new Model();
        Build effectiveBuild = new Build();
        effectiveBuild.addExtension(extensionOf("org.example", "my-extension", "1.0.0"));
        effectiveModel.setBuild(effectiveBuild);

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void versionPropertyUsedByReportPluginPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("site.version", "4.0.0");
        Reporting originalReporting = new Reporting();
        originalReporting.addPlugin(reportPluginOf("org.apache.maven.plugins", "maven-site-plugin", "${site.version}"));
        originalModel.setReporting(originalReporting);

        Model effectiveModel = new Model();
        Reporting effectiveReporting = new Reporting();
        effectiveReporting.addPlugin(reportPluginOf("org.apache.maven.plugins", "maven-site-plugin", "4.0.0"));
        effectiveModel.setReporting(effectiveReporting);

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void unusedVersionPropertyFails() {
        Model originalModel = new Model();
        originalModel.addProperty("old-lib.version", "2.0.0");

        Model effectiveModel = new Model();

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations)
                .hasSize(1)
                .first()
                .asString()
                .contains("old-lib.version")
                .contains("2.0.0")
                .contains("unused");
    }

    @Test
    void nonVersionPropertyIsIgnored() {
        Model originalModel = new Model();
        originalModel.addProperty("project.build.sourceEncoding", "UTF-8");

        Model effectiveModel = new Model();

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    @Test
    void multiplePropertiesMixedUsage() {
        Model originalModel = new Model();
        originalModel.addProperty("junit.version", "5.9.3");
        originalModel.addProperty("old-lib.version", "2.0.0");
        originalModel.addDependency(dependencyOf("org.junit", "junit", "${junit.version}"));

        Model effectiveModel = new Model();
        effectiveModel.addDependency(dependencyOf("org.junit", "junit", "5.9.3"));

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations)
                .hasSize(1)
                .allMatch(v -> v.contains("old-lib.version"));
    }

    @Test
    void multipleUnusedPropertiesReported() {
        Model originalModel = new Model();
        originalModel.addProperty("lib-a.version", "1.0.0");
        originalModel.addProperty("lib-b.version", "2.0.0");

        Model effectiveModel = new Model();

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations)
                .hasSize(2)
                .anyMatch(v -> v.contains("lib-a.version"))
                .anyMatch(v -> v.contains("lib-b.version"));
    }

    @Test
    void executeThrowsOnUnusedProperty() {
        Model originalModel = new Model();
        originalModel.addProperty("orphaned.version", "3.0.0");

        Model effectiveModel = new Model();

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        assertThatThrownBy(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("orphaned.version")
                .hasMessageContaining("3.0.0");
    }

    @Test
    void executePassesWhenAllPropertiesUsed() {
        Model originalModel = new Model();
        originalModel.addProperty("junit.version", "5.9.3");
        originalModel.addDependency(dependencyOf("org.junit", "junit", "${junit.version}"));

        Model effectiveModel = new Model();
        effectiveModel.addDependency(dependencyOf("org.junit", "junit", "5.9.3"));

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        assertThatNoException().isThrownBy(rule::execute);
    }

    @Test
    void versionPropertyUsedByPluginDependencyPasses() {
        Model originalModel = new Model();
        originalModel.addProperty("api.version", "3.0.0");
        Build originalBuild = new Build();
        var plugin = pluginOf("org.apache.maven.plugins", "maven-compiler-plugin", "3.13.0");
        plugin.addDependency(dependencyOf("org.apache.maven", "maven-plugin-api", "${api.version}"));
        originalBuild.addPlugin(plugin);
        originalModel.setBuild(originalBuild);

        Model effectiveModel = new Model();
        Build effectiveBuild = new Build();
        var effectivePlugin = pluginOf("org.apache.maven.plugins", "maven-compiler-plugin", "3.13.0");
        effectivePlugin.addDependency(dependencyOf("org.apache.maven", "maven-plugin-api", "3.0.0"));
        effectiveBuild.addPlugin(effectivePlugin);
        effectiveModel.setBuild(effectiveBuild);

        UnusedPropertyRule rule = createRule(originalModel, effectiveModel);

        Stream<String> violations = rule.scanProperties();

        assertThat(violations).isEmpty();
    }

    private static UnusedPropertyRule createRule(Model originalModel, Model effectiveModel) {
        UnusedPropertyRule rule = new UnusedPropertyRule(originalModel, effectiveModel);
        rule.setLog(mock(EnforcerLogger.class));
        return rule;
    }
}
