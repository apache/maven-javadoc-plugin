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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class TestJavadocReportTest
    extends AbstractMojoTestCase
{
    /**
     * Test the test-javadoc configuration for the plugin
     *
     * @throws Exception if any
     */
    public void testTestJavadoc()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/test-javadoc-test/test-javadoc-test-plugin-config.xml" );
        TestJavadocReport mojo = (TestJavadocReport) lookupMojo( "test-javadoc", testPom );
        
        MojoExecution mojoExec = new MojoExecution( new Plugin(), "test-javadoc", null );

        setVariableValueToObject( mojo, "mojo", mojoExec );

        MavenProject currentProject = new MavenProjectStub();
        currentProject.setGroupId( "GROUPID" );
        currentProject.setArtifactId( "ARTIFACTID" );
        
        setVariableValueToObject( mojo, "session", newMavenSession( currentProject ) );

        mojo.execute();

        File generatedFile =
            new File( getBasedir(), "target/test/unit/test-javadoc-test/target/site/apidocs/maven/AppTest.html" );
        assertThat( generatedFile ).exists();
        
        File options = new File( getBasedir(), "target/test/unit/test-javadoc-test/target/site/apidocs/options");
        assertThat( FileUtils.fileRead( options ) ).contains( "junit-3.8.1.jar" );
    }
}
