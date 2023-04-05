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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.thoughtworks.qdox.model.JavaClass;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * Fix Javadoc documentation and tags for the <code>Test Java code</code> for the project.
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/javadoc/doc-comment-spec.html#where-tags-can-be-used">Where Tags Can
 * Be Used</a>.
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.6
 */
@Mojo(name = "test-fix", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class TestFixJavadocMojo extends AbstractFixJavadocMojo {
    /** {@inheritDoc} */
    @Override
    protected List<String> getProjectSourceRoots(MavenProject p) {
        return (p.getTestCompileSourceRoots() == null
                ? Collections.<String>emptyList()
                : new LinkedList<>(p.getTestCompileSourceRoots()));
    }

    /** {@inheritDoc} */
    @Override
    protected List<String> getCompileClasspathElements(MavenProject p) throws DependencyResolutionRequiredException {
        return (p.getTestClasspathElements() == null
                ? Collections.<String>emptyList()
                : new LinkedList<>(p.getTestClasspathElements()));
    }

    /** {@inheritDoc} */
    @Override
    protected String getArtifactType(MavenProject p) {
        return "test-jar";
    }

    /** {@inheritDoc} */

    // Refactor : Push down from AbstractFixJavadocMojo class's execute method.
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // clirr doesn't analyze test code, so ignore it
        ignoreClirr = true;

        if (!fixClassComment && !fixFieldComment && !fixMethodComment) {
            getLog().info("Specified to NOT fix classes, fields and methods. Nothing to do.");
            return;
        }

        // verify goal params
        init();

        if (fixTagsSplitted.length == 0) {
            getLog().info("No fix tag specified. Nothing to do.");
            return;
        }

        // add warranty msg
        if (!preCheck()) {
            return;
        }

        // run clirr
        // Refactored Method
        try {
            runClirrCheck();
        } catch (MavenInvocationException e) {
            if (getLog().isDebugEnabled()) {
                getLog().error("MavenInvocationException: " + e.getMessage(), e);
            } else {
                getLog().error("MavenInvocationException: " + e.getMessage());
            }
            getLog().info("Clirr is ignored.");
        }

        // run qdox and process
        try {
            Collection<JavaClass> javaClasses = getQdoxClasses();

            if (javaClasses != null) {
                for (JavaClass javaClass : javaClasses) {
                    processFix(javaClass);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("IOException: " + e.getMessage(), e);
        }

    }

}
