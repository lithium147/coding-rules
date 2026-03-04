package com.solubris.enforcer;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Builder(toBuilder = true, setterPrefix = "with")
@RequiredArgsConstructor
@Getter
public class Artifact {
    private final String version;
    private final String artifactId;
    private final String groupId;
    private final String type;      // dependency, plugin, parent, etc.
    private final boolean managed;  // whether the version is from dependencyManagement or pluginManagement
    @With
    private final String profile;
    @With
    private final String effectiveVersion; // version after resolution, may be null if not resolved yet

    public Artifact(Dependency dependency, boolean managed, String profile) {
        this(dependency.getVersion(), dependency.getArtifactId(), dependency.getGroupId(), "Dependency", managed, profile, null);
    }

    public Artifact(Plugin plugin, boolean managed, String profile) {
        this(plugin.getVersion(), plugin.getArtifactId(), plugin.getGroupId(), "Plugin", managed, profile, null);
    }

    public Artifact(ReportPlugin plugin) {
        this(plugin.getVersion(), plugin.getArtifactId(), plugin.getGroupId(), "ReportPlugin", false, null, null);
    }

    public Artifact(ReportPlugin plugin, String profile) {
        this(plugin.getVersion(), plugin.getArtifactId(), plugin.getGroupId(), "ReportPlugin", false, profile, null);
    }

    public Artifact(Extension extension) {
        this(extension.getVersion(), extension.getArtifactId(), extension.getGroupId(), "Extension", false, null, null);
    }

    @Override
    public String toString() {
        String profilePart = profile != null ? String.format(" (profile: %s)", profile) : "";
        return String.format("%s: %s:%s(%s)%s", type, groupId, artifactId, managed ? "managed" : "direct", profilePart);
    }

    public static Artifact direct(Dependency dependency) {
        return new Artifact(dependency, false, null);
    }

    public static Artifact managed(Dependency dependency) {
        return new Artifact(dependency, true, null);
    }

    public String key() {
        // TODO can use versionlessKey()
        return groupId + ":" + artifactId;
    }

    public Artifact resolve(MavenProject project) {
        if (project == null || project.getArtifactMap() == null) return this;
        org.apache.maven.artifact.Artifact artifact = getArtifact(project);
        if (artifact == null) {
            System.out.println("Failed to resolve version for " + key() + ": artifact not found in project, type: " + type);
            return this;
        }
        String resolvedVersion = artifact.getVersion();
        if (resolvedVersion != null && !resolvedVersion.equals(version)) {
            System.out.println("Resolved version for " + key() + ": from " + version + " to " +resolvedVersion);
            return withEffectiveVersion(resolvedVersion);
        } else if (version.startsWith("${") && version.endsWith("}")) {
            System.out.println("Failed to resolve version for " + key() + ": artifact not found in project, type: " + type);
        }
        return this;
    }

    private org.apache.maven.artifact.Artifact getArtifact(MavenProject project) {
        org.apache.maven.artifact.Artifact result = project.getArtifactMap().get(key());
        if (result == null) {
            result = project.getPluginArtifactMap().get(key());
        }
        if (result == null) {
            result = project.getManagedVersionMap().get(key());
        }
        if (result == null) {
            result = project.getExtensionArtifactMap().get(key());
        }
        if (result == null) {
            result = project.getReportArtifactMap().get(key());
        }
        if (result == null) {
            result = project.getReportArtifactMap().get(key());
        }
        if (result == null) {
            result = pluginDependenciesByKey(project.getModel().getBuild().getPlugins()).get(key());
        }
        return result;
    }

    private Map<String, org.apache.maven.artifact.Artifact> pluginDependenciesByKey(List<Plugin> plugins) {
        return plugins.stream()
//                .peek(p -> System.out.println("Processing plugin " + p.getGroupId() + ":" + p.getArtifactId() + " with dependencies: " + p.getDependencies().size()))
                .flatMap(p -> p.getDependencies().stream())
                .map(d -> new org.apache.maven.artifact.DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(), null, type, null, new DefaultArtifactHandler()))
//                .peek(d -> System.out.println("Mapped plugin dependency to artifact: " + d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion()))
                .collect(toMap(ArtifactUtils::versionlessKey, a -> a, (a1, a2) -> a1));
    }
}
