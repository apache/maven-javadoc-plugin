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
package org.apache.maven.plugins.javadoc;

import javax.inject.Inject;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.javadoc.resolver.ResourceResolver;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.RepositorySystem;

/**
 * Generates documentation for the <code>Java code</code> in a <b>NON aggregator</b> project using the standard
 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html">Javadoc Tool</a>. Note that this
 * goal does require generation of sources before site generation, e.g. by invoking {@code mvn clean deploy site}.
 *
 * @author <a href="mailto:mfriedenhagen@apache.org">Mirko Friedenhagen</a>
 * @since 2.10
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html">Javadoc Tool</a>
 * @see <a href="http://docs.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#options">Javadoc Options</a>
 */
@Mojo(name = "javadoc-no-fork", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
@Execute(phase = LifecyclePhase.NONE)
public class JavadocNoForkReport extends JavadocReport {

    @Inject
    public JavadocNoForkReport(
            SiteTool siteTool,
            ArchiverManager archiverManager,
            ResourceResolver resourceResolver,
            RepositorySystem repoSystem,
            ArtifactHandlerManager artifactHandlerManager,
            ProjectBuilder mavenProjectBuilder,
            ToolchainManager toolchainManager) {
        super(
                siteTool,
                archiverManager,
                resourceResolver,
                repoSystem,
                artifactHandlerManager,
                mavenProjectBuilder,
                toolchainManager);
    }
}
