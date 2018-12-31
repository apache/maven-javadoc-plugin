package org.apache.maven.plugins.javadoc.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class ProxyTestMavenProjectStub
    extends MavenProjectStub
{
    private Set<Artifact> dependencyArtifacts = new HashSet<>();

    public ProxyTestMavenProjectStub()
    {
        readModel( new File( getBasedir(), "proxy-test-plugin-config.xml" ) );

        setGroupId( getModel().getGroupId() );
        setArtifactId( getModel().getArtifactId() );
        setVersion( getModel().getVersion() );
        setName( getModel().getName() );
        setUrl( getModel().getUrl() );
        setPackaging( getModel().getPackaging() );

        Build build = new Build();
        build.setFinalName( getModel().getArtifactId() );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/proxy-test/target" );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        
        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId()).thenReturn( "commons-logging" );
        when( artifact.getArtifactId()).thenReturn( "commons-logging" );
        when( artifact.getVersion()).thenReturn( "1.1.1" );
        when( artifact.getScope()).thenReturn( Artifact.SCOPE_RUNTIME );
        when( artifact.getType()).thenReturn( "jar" );
        when( artifact.getFile()).thenReturn( getBasedir() );
        DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub();
        artifactHandler.setExtension( "jar" );
        when( artifact.getArtifactHandler()).thenReturn( artifactHandler );

        dependencyArtifacts.add( artifact );
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/proxy-test" );
    }

    @Override
    public Set<Artifact> getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }
}
