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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class HelpFileMavenProjectStub extends MavenProjectStub
{
    public HelpFileMavenProjectStub()
    {
        readModel( new File( getBasedir(), "pom.xml" ) );

        setGroupId( getModel().getGroupId() );
        setArtifactId( getModel().getArtifactId() );
        setVersion( getModel().getVersion() );
        setName( getModel().getName() );
        setUrl( getModel().getUrl() );
        setPackaging( getModel().getPackaging() );

        Build build = new Build();
        build.setFinalName( getModel().getArtifactId() );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/helpfile-test/target" );
        Resource resource = new Resource();
        resource.setDirectory( getBasedir() + "/src/main/resources" );
        build.addResource( resource );

        build.setPlugins( getModel().getBuild().getPlugins() );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/helpfile-test" );
    }

    /** {@inheritDoc} */
    @Override
    public List<ArtifactRepository> getRemoteArtifactRepositories()
    {
        ArtifactRepository repository =
            new DefaultArtifactRepository( "central", "http://repo.maven.apache.org/maven2",
                                           new DefaultRepositoryLayout() );

        return Collections.singletonList( repository );
    }
}
