/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.javadoc.resolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugins.javadoc.AbstractJavadocMojo;
import org.apache.maven.plugins.javadoc.JavadocModule;
import org.apache.maven.plugins.javadoc.JavadocUtil;
import org.apache.maven.plugins.javadoc.ResourcesBundleMojo;
import org.apache.maven.plugins.javadoc.options.JavadocOptions;
import org.apache.maven.plugins.javadoc.options.io.xpp3.JavadocOptionsXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.resolve.transform.ArtifactIncludeFilterTransformer;
import org.apache.maven.shared.artifact.filter.resolve.transform.EclipseAetherFilterTransformer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 *
 */
@Named
@Singleton
public final class ResourceResolver extends AbstractLogEnabled {
    @Inject
    private RepositorySystem repoSystem;

    @Inject
    private ArchiverManager archiverManager;

    /**
     * The classifier for sources.
     */
    public static final String SOURCES_CLASSIFIER = "sources";

    /**
     * The classifier for test sources
     */
    public static final String TEST_SOURCES_CLASSIFIER = "test-sources";

    private static final List<String> SOURCE_VALID_CLASSIFIERS =
            Arrays.asList(SOURCES_CLASSIFIER, TEST_SOURCES_CLASSIFIER);

    private static final List<String> RESOURCE_VALID_CLASSIFIERS = Arrays.asList(
            AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER,
            AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER);

    /**
     * @param config {@link SourceResolverConfig}
     * @return list of {@link JavadocBundle}.
     * @throws IOException {@link IOException}
     */
    public List<JavadocBundle> resolveDependencyJavadocBundles(final SourceResolverConfig config) throws IOException {
        final List<JavadocBundle> bundles = new ArrayList<>();

        final Map<String, MavenProject> projectMap = new HashMap<>();
        if (config.reactorProjects() != null) {
            for (final MavenProject p : config.reactorProjects()) {
                projectMap.put(key(p.getGroupId(), p.getArtifactId()), p);
            }
        }

        final List<Artifact> artifacts = config.project().getTestArtifacts();

        final List<Artifact> forResourceResolution = new ArrayList<>(artifacts.size());
        for (final Artifact artifact : artifacts) {
            final String key = key(artifact.getGroupId(), artifact.getArtifactId());
            final MavenProject p = projectMap.get(key);
            if (p != null) {
                bundles.addAll(resolveBundleFromProject(config, p, artifact));
            } else {
                forResourceResolution.add(artifact);
            }
        }

        bundles.addAll(resolveBundlesFromArtifacts(config, forResourceResolution));

        return bundles;
    }

    /**
     * @param config {@link SourceResolverConfig}
     * @return The list of resolved dependencies.
     * @throws ArtifactResolutionException {@link ArtifactResolutionException}
     * @throws ArtifactNotFoundException {@link ArtifactNotFoundException}
     */
    public Collection<JavadocModule> resolveDependencySourcePaths(final SourceResolverConfig config)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        final Collection<JavadocModule> mappedDirs = new ArrayList<>();

        final Map<String, MavenProject> projectMap = new HashMap<>();
        if (config.reactorProjects() != null) {
            for (final MavenProject p : config.reactorProjects()) {
                projectMap.put(key(p.getGroupId(), p.getArtifactId()), p);
            }
        }

        final List<Artifact> artifacts = config.project().getTestArtifacts();

        for (final Artifact artifact : artifacts) {
            final String key = key(artifact.getGroupId(), artifact.getArtifactId());
            final MavenProject p = projectMap.get(key);
            if (p != null) {
                mappedDirs.add(new JavadocModule(key, artifact.getFile(), resolveFromProject(config, p, artifact)));
            } else {
                JavadocModule m = resolveFromArtifact(config, artifact);
                if (m != null) {
                    mappedDirs.add(m);
                }
            }
        }

        return mappedDirs;
    }

    private static List<JavadocBundle> resolveBundleFromProject(
            SourceResolverConfig config, MavenProject project, Artifact artifact) throws IOException {
        List<JavadocBundle> bundles = new ArrayList<>();

        List<String> classifiers = new ArrayList<>();
        if (config.includeCompileSources()) {
            classifiers.add(AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER);
        }

        if (config.includeTestSources()) {
            classifiers.add(AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER);
        }

        for (String classifier : classifiers) {
            File optionsFile = new File(
                    project.getBuild().getDirectory(), "javadoc-bundle-options/javadoc-options-" + classifier + ".xml");
            if (!optionsFile.exists()) {
                continue;
            }

            try (FileInputStream stream = new FileInputStream(optionsFile)) {
                JavadocOptions options = new JavadocOptionsXpp3Reader().read(stream);
                bundles.add(new JavadocBundle(
                        options, new File(project.getBasedir(), options.getJavadocResourcesDirectory())));
            } catch (XmlPullParserException e) {
                IOException error = new IOException(
                        "Failed to read javadoc options from: " + optionsFile + "\nReason: " + e.getMessage(), e);
                throw error;
            }
        }

        return bundles;
    }

    private List<JavadocBundle> resolveBundlesFromArtifacts(
            final SourceResolverConfig config, final List<Artifact> artifacts) throws IOException {
        final List<org.eclipse.aether.artifact.Artifact> toResolve = new ArrayList<>(artifacts.size());

        for (final Artifact artifact : artifacts) {
            if (config.filter() != null
                    && !new ArtifactIncludeFilterTransformer()
                            .transform(config.filter())
                            .include(artifact)) {
                continue;
            }

            if (config.includeCompileSources()) {
                toResolve.add(createResourceArtifact(
                        artifact, AbstractJavadocMojo.JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER, config));
            }

            if (config.includeTestSources()) {
                toResolve.add(createResourceArtifact(
                        artifact, AbstractJavadocMojo.TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER, config));
            }
        }

        Collection<Path> dirs = new ArrayList<>(toResolve.size());
        try {
            dirs = resolveAndUnpack(toResolve, config, RESOURCE_VALID_CLASSIFIERS, false);
        } catch (ArtifactResolutionException | ArtifactNotFoundException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(e.getMessage(), e);
            }
        }

        List<JavadocBundle> result = new ArrayList<>();

        for (Path d : dirs) {
            File dir = d.toFile();
            File resources = new File(dir, ResourcesBundleMojo.RESOURCES_DIR_PATH);
            JavadocOptions options = null;

            File javadocOptions = new File(dir, ResourcesBundleMojo.BUNDLE_OPTIONS_PATH);
            if (javadocOptions.exists()) {
                try (FileInputStream reader = new FileInputStream(javadocOptions)) {
                    options = new JavadocOptionsXpp3Reader().read(reader);
                } catch (XmlPullParserException e) {
                    IOException error = new IOException("Failed to parse javadoc options: " + e.getMessage(), e);
                    throw error;
                }
            }

            result.add(new JavadocBundle(options, resources));
        }

        return result;
    }

    private JavadocModule resolveFromArtifact(final SourceResolverConfig config, final Artifact artifact)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        final List<org.eclipse.aether.artifact.Artifact> toResolve = new ArrayList<>(2);

        if (config.filter() != null
                && !new ArtifactIncludeFilterTransformer()
                        .transform(config.filter())
                        .include(artifact)) {
            return null;
        }

        if (config.includeCompileSources()) {
            toResolve.add(createResourceArtifact(artifact, SOURCES_CLASSIFIER, config));
        }

        if (config.includeTestSources()) {
            toResolve.add(createResourceArtifact(artifact, TEST_SOURCES_CLASSIFIER, config));
        }

        Collection<Path> sourcePaths = resolveAndUnpack(toResolve, config, SOURCE_VALID_CLASSIFIERS, true);

        return new JavadocModule(key(artifact.getGroupId(), artifact.getArtifactId()), artifact.getFile(), sourcePaths);
    }

    private org.eclipse.aether.artifact.Artifact createResourceArtifact(
            final Artifact artifact, final String classifier, final SourceResolverConfig config) {
        return new org.eclipse.aether.artifact.DefaultArtifact(
                artifact.getGroupId(), artifact.getArtifactId(), classifier, "jar", artifact.getVersion());
    }

    /**
     *
     * @param artifacts the artifacts to resolve
     * @param config the configuration
     * @param validClassifiers
     * @param propagateErrors
     * @return list of <dependencyConflictId, absolutePath>
     * @throws ArtifactResolutionException if an exception occurs
     * @throws ArtifactNotFoundException if an exception occurs
     */
    private Collection<Path> resolveAndUnpack(
            final List<org.eclipse.aether.artifact.Artifact> artifacts,
            final SourceResolverConfig config,
            final List<String> validClassifiers,
            final boolean propagateErrors)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        // NOTE: Since these are '-sources' and '-test-sources' artifacts, they won't actually
        // resolve transitively...this is just used to aggregate resolution failures into a single
        // exception.
        final Set<org.eclipse.aether.artifact.Artifact> artifactSet = new LinkedHashSet<>(artifacts);

        final DependencyFilter filter;
        if (config.filter() != null) {
            filter = new EclipseAetherFilterTransformer().transform(config.filter());
        } else {
            filter = null;
        }

        final List<Path> result = new ArrayList<>(artifacts.size());
        for (final org.eclipse.aether.artifact.Artifact a : artifactSet) {
            if (!validClassifiers.contains(a.getClassifier())
                    || (filter != null && !filter.accept(new DefaultDependencyNode(a), Collections.emptyList()))) {
                continue;
            }

            Artifact resolvedArtifact;
            ArtifactRequest req = new ArtifactRequest(a, config.project().getRemoteProjectRepositories(), null);
            try {
                RepositorySystemSession repoSession =
                        config.getBuildingRequest().getRepositorySession();
                ArtifactResult resolutionResult = repoSystem.resolveArtifact(repoSession, req);
                resolvedArtifact = RepositoryUtils.toArtifact(resolutionResult.getArtifact());
            } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
                continue;
            }
            final File d = new File(
                    config.outputBasedir(), a.getArtifactId() + "-" + a.getVersion() + "-" + a.getClassifier());

            if (!d.exists()) {
                d.mkdirs();
            }

            try {
                final UnArchiver unArchiver = archiverManager.getUnArchiver(a.getExtension());

                unArchiver.setDestDirectory(d);
                unArchiver.setSourceFile(resolvedArtifact.getFile());

                unArchiver.extract();

                result.add(d.toPath().toAbsolutePath());
            } catch (final NoSuchArchiverException e) {
                if (propagateErrors) {
                    throw new ArtifactResolutionException(
                            "Failed to retrieve valid un-archiver component: " + a.getExtension(),
                            RepositoryUtils.toArtifact(a),
                            e);
                }
            } catch (final ArchiverException e) {
                if (propagateErrors) {
                    throw new ArtifactResolutionException("Failed to unpack: " + a, RepositoryUtils.toArtifact(a), e);
                }
            }
        }

        return result;
    }

    private static Collection<Path> resolveFromProject(
            final SourceResolverConfig config, final MavenProject reactorProject, final Artifact artifact) {
        final List<String> dirs = new ArrayList<>();

        if (config.filter() == null
                || new ArtifactIncludeFilterTransformer()
                        .transform(config.filter())
                        .include(artifact)) {
            if (config.includeCompileSources()) {
                final List<String> srcRoots = reactorProject.getCompileSourceRoots();
                dirs.addAll(srcRoots);
            }

            if (config.includeTestSources()) {
                final List<String> srcRoots = reactorProject.getTestCompileSourceRoots();
                dirs.addAll(srcRoots);
            }
        }

        return JavadocUtil.pruneDirs(reactorProject, dirs);
    }

    private static String key(final String gid, final String aid) {
        return gid + ":" + aid;
    }
}
