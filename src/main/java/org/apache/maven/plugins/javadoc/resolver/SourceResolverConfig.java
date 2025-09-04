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

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.AndFilter;

/**
 * <p>SourceResolverConfig class.</p>
 *
 * @author elharo
 */
public class SourceResolverConfig {
    private ProjectBuildingRequest buildingRequest;

    private final MavenProject project;

    private AndFilter filter;

    private List<MavenProject> reactorProjects;

    private final File outputBasedir;

    private boolean compileSourceIncluded;

    private boolean testSourceIncluded;

    /**
     * <p>Constructor for SourceResolverConfig.</p>
     *
     * @param project {@link org.apache.maven.project.MavenProject}
     * @param buildingRequest {@link org.apache.maven.project.ProjectBuildingRequest}
     * @param outputBasedir The output base directory.
     */
    public SourceResolverConfig(
            final MavenProject project, final ProjectBuildingRequest buildingRequest, final File outputBasedir) {
        this.project = project;
        this.buildingRequest = buildingRequest;
        this.outputBasedir = outputBasedir;
    }

    /**
     * <p>withFilter.</p>
     *
     * @param filter {@link org.apache.maven.shared.artifact.filter.resolve.AndFilter}
     * @return {@link org.apache.maven.plugins.javadoc.resolver.SourceResolverConfig}
     */
    public SourceResolverConfig withFilter(final AndFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     * <p>withReactorProjects.</p>
     *
     * @param reactorProjects The list of reactor projects.
     * @return {@link org.apache.maven.plugins.javadoc.resolver.SourceResolverConfig}
     */
    public SourceResolverConfig withReactorProjects(final List<MavenProject> reactorProjects) {
        this.reactorProjects = reactorProjects;
        return this;
    }

    /**
     * <p>withCompileSources.</p>
     *
     * @return {@link org.apache.maven.plugins.javadoc.resolver.SourceResolverConfig}
     */
    public SourceResolverConfig withCompileSources() {
        compileSourceIncluded = true;
        return this;
    }

    /**
     * <p>withoutCompileSources.</p>
     *
     * @return {@link org.apache.maven.plugins.javadoc.resolver.SourceResolverConfig}
     */
    public SourceResolverConfig withoutCompileSources() {
        compileSourceIncluded = false;
        return this;
    }

    /**
     * <p>withTestSources.</p>
     *
     * @return {@link org.apache.maven.plugins.javadoc.resolver.SourceResolverConfig}
     */
    public SourceResolverConfig withTestSources() {
        testSourceIncluded = true;
        return this;
    }

    /**
     * <p>withoutTestSources.</p>
     *
     * @return {@link org.apache.maven.plugins.javadoc.resolver.SourceResolverConfig}
     */
    public SourceResolverConfig withoutTestSources() {
        testSourceIncluded = false;
        return this;
    }

    /**
     * <p>project.</p>
     *
     * @return {@link org.apache.maven.project.MavenProject}
     */
    public MavenProject project() {
        return project;
    }

    /**
     * <p>Getter for the field <code>buildingRequest</code>.</p>
     *
     * @return {@link org.apache.maven.project.ProjectBuildingRequest}
     */
    public ProjectBuildingRequest getBuildingRequest() {
        return buildingRequest;
    }

    /**
     * <p>filter.</p>
     *
     * @return {@link org.apache.maven.shared.artifact.filter.resolve.AndFilter}
     */
    public AndFilter filter() {
        return filter;
    }

    /**
     * <p>reactorProjects.</p>
     *
     * @return list of {@link org.apache.maven.project.MavenProject}
     */
    public List<MavenProject> reactorProjects() {
        return reactorProjects;
    }

    /**
     * <p>outputBasedir.</p>
     *
     * @return {@link #outputBasedir}
     */
    public File outputBasedir() {
        return outputBasedir;
    }

    /**
     * <p>includeCompileSources.</p>
     *
     * @return {@link #compileSourceIncluded}
     */
    public boolean includeCompileSources() {
        return compileSourceIncluded;
    }

    /**
     * <p>includeTestSources.</p>
     *
     * @return {@link #testSourceIncluded}
     */
    public boolean includeTestSources() {
        return testSourceIncluded;
    }
}
