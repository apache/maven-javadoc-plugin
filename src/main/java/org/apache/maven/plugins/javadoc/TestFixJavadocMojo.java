package org.apache.maven.plugins.javadoc;

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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Fix Javadoc documentation and tags for the <code>Test Java code</code> for the project.
 * See <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#wheretags">Where Tags Can
 * Be Used</a>.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.6
 */
@Mojo( name = "test-fix", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
@Execute( phase = LifecyclePhase.TEST_COMPILE )
public class TestFixJavadocMojo
    extends AbstractFixJavadocMojo
{
    /** {@inheritDoc} */
    @Override
    protected List<String> getProjectSourceRoots( MavenProject p )
    {
        return ( p.getTestCompileSourceRoots() == null ? Collections.<String>emptyList()
                        : new LinkedList<>( p.getTestCompileSourceRoots() ) );
    }

    /** {@inheritDoc} */
    @Override
    protected List<String> getCompileClasspathElements( MavenProject p )
        throws DependencyResolutionRequiredException
    {
        return ( p.getTestClasspathElements() == null ? Collections.<String>emptyList()
                        : new LinkedList<>( p.getTestClasspathElements() ) );
    }

    /** {@inheritDoc} */
    @Override
    protected String getArtifactType( MavenProject p )
    {
        return "test-jar";
    }

    /** {@inheritDoc} */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // clirr doesn't analyze test code, so ignore it
        ignoreClirr = true;

        super.execute();
    }
}
