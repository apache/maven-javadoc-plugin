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

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.api.plugin.testing.MojoExtension.getVariableValueFromObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
@MojoTest
class JavadocJarMojoTest {

    @Inject
    private MavenProject project;

    @Inject
    private Log log;

    @BeforeEach
    void setup() {
        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler("jar");
        artifactHandler.setLanguage("java");

        DefaultArtifact artifact =
                new DefaultArtifact("GROUPID", "ARTIFACTID", "1.0-SNAPSHOT", "compile", "jar", null, artifactHandler);
        project.setArtifact(artifact);
    }
    /**
     * Test when default configuration is provided
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "jar", pom = "javadocjar-default-plugin-config.xml")
    @Basedir("/unit/javadocjar-default")
    void testDefaultConfig(JavadocJarMojo mojo) throws Exception {
        mojo.execute();

        // check if the javadoc jar file was generated
        File generatedFile = new File(getBasedir(), "/target/javadocjar-default-javadoc.jar");
        assertThat(generatedFile).exists();

        Set<String> set = new HashSet<>();

        // validate contents of jar file
        try (ZipFile jar = new ZipFile(generatedFile)) {
            for (Enumeration<? extends ZipEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                set.add(entry.getName());
            }
        }
        if (JavaVersion.JAVA_VERSION.isAtLeast("23")) {
            assertTrue(set.contains("resource-files/stylesheet.css"));
        } else {
            assertTrue(set.contains("stylesheet.css"));
        }
        JavaVersion javadocVersion = getVariableValueFromObject(mojo, "javadocRuntimeVersion");
        if (javadocVersion.isBefore("1.7")) {
            assertTrue(set.contains("resources/inherit.gif"));
        } else if (javadocVersion.isBefore("1.8")) {
            assertTrue(set.contains("resources/background.gif") /* JDK7 */);
        } else {
            // JDK8 has no resources anymore
            assertFalse(set.contains("resources"));
        }

        assertTrue(set.contains("javadocjar/def/package-use.html"));
        assertTrue(set.contains("javadocjar/def/package-tree.html"));
        assertTrue(set.contains("javadocjar/def/package-summary.html"));
        // package frame not generated anymore since Java 11
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("11")) {
            assertTrue(set.contains("javadocjar/def/package-frame.html"));
        }
        assertTrue(set.contains("javadocjar/def/class-use/AppSample.html"));
        assertTrue(set.contains("index.html"));
        assertTrue(set.contains("javadocjar/def/App.html"));
        assertTrue(set.contains("javadocjar/def/AppSample.html"));
        assertTrue(set.contains("javadocjar/def/class-use/App.html"));

        assertFalse(set.contains(AbstractJavadocMojo.ARGFILE_FILE_NAME));
        assertFalse(set.contains(AbstractJavadocMojo.FILES_FILE_NAME));
        assertFalse(set.contains(AbstractJavadocMojo.OPTIONS_FILE_NAME));
        assertFalse(set.contains(AbstractJavadocMojo.PACKAGES_FILE_NAME));

        // check if the javadoc files were created
        generatedFile = new File(getBasedir(), "/target/site/apidocs/javadocjar/def/App.html");
        assertThat(generatedFile).exists();

        generatedFile = new File(getBasedir(), "/target/site/apidocs/javadocjar/def/AppSample.html");
        assertThat(generatedFile).exists();
    }

    @Test
    @InjectMojo(goal = "jar", pom = "javadocjar-failonerror-plugin-config.xml")
    @Basedir("/unit/javadocjar-failonerror")
    void testContinueIfFailOnErrorIsFalse(JavadocJarMojo mojo) throws Exception {
        mojo.execute();

        // check if the javadoc jar file was generated
        File generatedFile = new File(getBasedir(), "/target/javadocjar-failonerror-javadoc.jar");
        assertThat(generatedFile).exists();
    }

    @Test
    @InjectMojo(goal = "jar", pom = "javadocjar-archive-config.xml")
    @Basedir("/unit/javadocjar-archive-config")
    void testIncludeMavenDescriptorWhenExplicitlyConfigured(JavadocJarMojo mojo) throws Exception {

        project.setGroupId("org.apache.maven.plugins.maven-javadoc-plugin.unit");
        project.setArtifactId("javadocjar-archive-config");

        mojo.execute();

        // check if the javadoc jar file was generated
        File generatedFile = new File(getBasedir(), "/target/javadocjar-archive-config-javadoc.jar");
        assertThat(generatedFile).exists();

        // validate contents of jar file
        ZipFile jar = new ZipFile(generatedFile);
        Set<String> set = new HashSet<>();
        for (Enumeration<? extends ZipEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = entries.nextElement();
            set.add(entry.getName());
        }
        jar.close();

        assertThat(set)
                .contains(
                        "META-INF/",
                        "META-INF/maven/",
                        "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/",
                        "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/javadocjar-archive-config/",
                        "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/javadocjar-archive-config/pom.xml",
                        "META-INF/maven/org.apache.maven.plugins.maven-javadoc-plugin.unit/javadocjar-archive-config/pom.properties");
    }

    @Test
    @InjectMojo(goal = "jar", pom = "stale-test-plugin-config.xml")
    @Basedir("/unit/stale-test")
    void testStale(JavadocJarMojo mojo) throws Exception {

        new File(getBasedir(), "/target/maven-javadoc-plugin-stale-data.txt").delete();

        mojo.execute();
        verify(log).debug("No previous run data found, generating javadoc.");

        clearInvocations(log);

        mojo.execute();
        verify(log).debug("Skipping javadoc generation, everything is up to date.");
    }
}
