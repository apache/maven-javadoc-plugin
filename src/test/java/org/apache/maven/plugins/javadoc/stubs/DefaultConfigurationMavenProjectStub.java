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

import org.apache.maven.model.Build;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DefaultConfigurationMavenProjectStub
    extends MavenProjectStub
{
    private Scm scm;

    public DefaultConfigurationMavenProjectStub()
    {
        readModel( new File( getBasedir(), "default-configuration-plugin-config.xml" ) );

        setGroupId( "org.apache.maven.plugins.maven-javadoc-plugin.unit" );
        setArtifactId( "default-configuration" );
        setVersion( "1.0-SNAPSHOT" );
        setName( "Maven Javadoc Plugin Default configuration Test" );
        setUrl( "http://maven.apache.org" );
        setPackaging( "jar" );

        Scm scm = new Scm();
        scm.setConnection( "scm:svn:http://svn.apache.org/maven/sample/trunk" );
        setScm( scm );

        Build build = new Build();
        build.setFinalName( "default-configuration" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/default-configuration/target" );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add( getBasedir().getAbsolutePath() );
        setCompileSourceRoots( compileSourceRoots );
    }

    /** {@inheritDoc} */
    @Override
    public Scm getScm()
    {
        return scm;
    }

    /** {@inheritDoc} */
    @Override
    public void setScm( Scm scm )
    {
        this.scm = scm;
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/default-configuration/" );
    }
}
