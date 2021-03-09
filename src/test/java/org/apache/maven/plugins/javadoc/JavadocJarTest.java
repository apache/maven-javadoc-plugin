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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.languages.java.version.JavaVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class JavadocJarTest
    extends AbstractMojoTestCase
{
    
    private JavadocJar lookupMojo( File testPom )
                    throws Exception
    {
        JavadocJar mojo = (JavadocJar) lookupMojo( "jar", testPom );

        MojoExecution mojoExec = new MojoExecution( new Plugin(), "javadoc", null );

        setVariableValueToObject( mojo, "mojo", mojoExec );
        
        MavenProject currentProject = new MavenProjectStub();
        currentProject.setGroupId( "GROUPID" );
        currentProject.setArtifactId( "ARTIFACTID" );
        
        setVariableValueToObject( mojo, "session", newMavenSession( currentProject ) );
        
        return mojo;
    }


    /**
     * Test when default configuration is provided
     *
     * @throws Exception if any
     */
    public void testDefaultConfig()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/javadocjar-default/javadocjar-default-plugin-config.xml" );
        JavadocJar mojo = lookupMojo( testPom );
        mojo.execute();

        //check if the javadoc jar file was generated
        File generatedFile =
            new File( getBasedir(), "target/test/unit/javadocjar-default/target/javadocjar-default-javadoc.jar" );
        assertThat( generatedFile ).exists();

        Set<String> set = new HashSet<>();

        //validate contents of jar file
        try ( ZipFile jar = new ZipFile( generatedFile ) )
        {
            for( Enumeration<? extends ZipEntry> entries = jar.entries(); entries.hasMoreElements(); )
            {
                ZipEntry entry = entries.nextElement();
                set.add( entry.getName() );
            }
        }

        assertTrue( set.contains( "stylesheet.css" ) );
        JavaVersion javadocVersion = (JavaVersion) getVariableValueFromObject( mojo, "javadocRuntimeVersion" );
        if ( javadocVersion.isBefore( "1.7" ) )
        {
            assertTrue( set.contains( "resources/inherit.gif" ) );
        }
        else if ( javadocVersion.isBefore( "1.8" ) )
        {
            assertTrue( set.contains( "resources/background.gif" ) /* JDK7 */);
        }
        else
        {
            // JDK8 has no resources anymore
            assertFalse( set.contains( "resources" ) );
        }

        assertTrue( set.contains( "javadocjar/def/package-use.html" ) );
        assertTrue( set.contains( "javadocjar/def/package-tree.html" ) );
        assertTrue( set.contains( "javadocjar/def/package-summary.html" ) );
        // package frame not generated anymore since Java 11
        if ( JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore( "11" ) )
        {
            assertTrue( set.contains( "javadocjar/def/package-frame.html" ) );
        }
        assertTrue( set.contains( "javadocjar/def/class-use/AppSample.html" ) );
        assertTrue( set.contains( "index.html" ) );
        assertTrue( set.contains( "javadocjar/def/App.html" ) );
        assertTrue( set.contains( "javadocjar/def/AppSample.html" ) );
        assertTrue( set.contains( "javadocjar/def/class-use/App.html" ) );

        assertFalse( set.contains( AbstractJavadocMojo.ARGFILE_FILE_NAME ) );
        assertFalse( set.contains( AbstractJavadocMojo.FILES_FILE_NAME ) );
        assertFalse( set.contains( AbstractJavadocMojo.OPTIONS_FILE_NAME ) );
        assertFalse( set.contains( AbstractJavadocMojo.PACKAGES_FILE_NAME ) );

        //check if the javadoc files were created
        generatedFile =
            new File( getBasedir(), "target/test/unit/javadocjar-default/target/site/apidocs/javadocjar/def/App.html" );
        assertThat(generatedFile).exists();

        generatedFile = new File( getBasedir(),
                                  "target/test/unit/javadocjar-default/target/site/apidocs/javadocjar/def/AppSample.html" );
        assertThat( generatedFile ).exists();
    }

    /**
     * Test when the specified destDir parameter has an invalid value
     *
     * @throws Exception if any
     */
    public void testInvalidDestdir()
        throws Exception
    {
        File testPom = new File( getBasedir(),
                                 "src/test/resources/unit/javadocjar-invalid-destdir/javadocjar-invalid-destdir-plugin-config.xml" );
        JavadocJar mojo = lookupMojo( testPom );
        mojo.execute();

        //check if the javadoc jar file was generated
        File generatedFile = new File( getBasedir(),
                                       "target/test/unit/javadocjar-invalid-destdir/target/javadocjar-invalid-destdir-javadoc.jar" );
        assertThat( generatedFile ).doesNotExist();
    }

    public void testContinueIfFailOnErrorIsFalse() throws Exception
    {
        File testPom =
                new File( getBasedir(), "src/test/resources/unit/javadocjar-failonerror/javadocjar-failonerror-plugin-config.xml" );
        JavadocJar mojo = lookupMojo( testPom );
        mojo.execute();

        //check if the javadoc jar file was generated
        File generatedFile =
                new File( getBasedir(), "target/test/unit/javadocjar-failonerror/target/javadocjar-failonerror-javadoc.jar" );
        assertThat( generatedFile ).exists();
    }

    public void testIncludeMavenDescriptorWhenExplicitlyConfigured() throws Exception
    {
        File testPom =
                new File( getBasedir(), "src/test/resources/unit/javadocjar-archive-config/javadocjar-archive-config.xml" );
        JavadocJar mojo = lookupMojo( testPom );
        mojo.execute();

        //check if the javadoc jar file was generated
        File generatedFile =
                new File( getBasedir(), "target/test/unit/javadocjar-archive-config/target/javadocjar-archive-config-javadoc.jar" );
        assertThat( generatedFile ).exists();

        //validate contents of jar file
        ZipFile jar = new ZipFile( generatedFile );
        Set<String> set = new HashSet<>();
        for (Enumeration<? extends ZipEntry> entries = jar.entries(); entries.hasMoreElements(); )
        {
            ZipEntry entry = entries.nextElement();
            set.add( entry.getName() );
        }
        jar.close();

        assertThat( set ).contains(
                "META-INF/",
                "META-INF/maven/",
                "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/",
                "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/javadocjar-archive-config/",
                "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/javadocjar-archive-config/pom.xml",
                "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/javadocjar-archive-config/pom.properties" );
    }
}
