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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class JavadocJarMojoTest extends AbstractMojoTestCase {

    private JavadocJarMojo lookupMojo(File testPom) throws Exception {
        JavadocJarMojo mojo = (JavadocJarMojo) lookupMojo("jar", testPom);

        Plugin p = new Plugin();
        p.setGroupId("org.apache.maven.plugins");
        p.setArtifactId("maven-javadoc-plugin");
        MojoExecution mojoExecution = new MojoExecution(p, "jar", null);

        setVariableValueToObject(mojo, "mojoExecution", mojoExecution);

        MavenProject currentProject = new MavenProjectStub();
        currentProject.setGroupId("GROUPID");
        currentProject.setArtifactId("ARTIFACTID");

        MavenSession session = newMavenSession(currentProject);
        ((DefaultRepositorySystemSession) session.getRepositorySession())
                .setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                        .newInstance(
                                session.getRepositorySession(), new LocalRepository(new File("target/local-repo"))));
        setVariableValueToObject(mojo, "session", session);

        return mojo;
    }

    /**
     * Test when default configuration is provided
     *
     * @throws Exception if any
     */
    public void testDefaultConfig() throws Exception {
        File testPom = new File(
                getBasedir(), "src/test/resources/unit/javadocjar-default/javadocjar-default-plugin-config.xml");
        JavadocJarMojo mojo = lookupMojo(testPom);
        mojo.execute();

        // check if the javadoc jar file was generated
        File generatedFile =
                new File(getBasedir(), "target/test/unit/javadocjar-default/target/javadocjar-default-javadoc.jar");
        assertThat(generatedFile).exists();

        Set<String> set = new HashSet<>();

        // validate contents of jar file
        try (ZipFile jar = new ZipFile(generatedFile)) {
            for (Enumeration<? extends ZipEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                set.add(entry.getName());
            }
        }

        assertTrue(set.contains("stylesheet.css"));
        JavaVersion javadocVersion = (JavaVersion) getVariableValueFromObject(mojo, "javadocRuntimeVersion");
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
        generatedFile = new File(
                getBasedir(), "target/test/unit/javadocjar-default/target/site/apidocs/javadocjar/def/App.html");
        assertThat(generatedFile).exists();

        generatedFile = new File(
                getBasedir(), "target/test/unit/javadocjar-default/target/site/apidocs/javadocjar/def/AppSample.html");
        assertThat(generatedFile).exists();
    }

    public void testContinueIfFailOnErrorIsFalse() throws Exception {
        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/javadocjar-failonerror/javadocjar-failonerror-plugin-config.xml");
        JavadocJarMojo mojo = lookupMojo(testPom);
        mojo.execute();

        // check if the javadoc jar file was generated
        File generatedFile = new File(
                getBasedir(), "target/test/unit/javadocjar-failonerror/target/javadocjar-failonerror-javadoc.jar");
        assertThat(generatedFile).exists();
    }

    public void testIncludeMavenDescriptorWhenExplicitlyConfigured() throws Exception {
        File testPom = new File(
                getBasedir(), "src/test/resources/unit/javadocjar-archive-config/javadocjar-archive-config.xml");
        JavadocJarMojo mojo = lookupMojo(testPom);
        mojo.execute();

        // check if the javadoc jar file was generated
        File generatedFile = new File(
                getBasedir(),
                "target/test/unit/javadocjar-archive-config/target/javadocjar-archive-config-javadoc.jar");
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

    public void testStale() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/stale-test/stale-test-plugin-config.xml");
        JavadocJarMojo mojo = lookupMojo(testPom);
        BufferingLog log = new BufferingLog();
        mojo.setLog(log);

        Thread.sleep(500);

        new File(getBasedir(), "target/test/unit/stale-test/target/maven-javadoc-plugin-stale-data.txt").delete();
        mojo.execute();
        assertThat(log.getMessages()).contains("[INFO] No previous run data found, generating javadoc.");

        Thread.sleep(500);

        log.getMessages().clear();
        mojo.execute();
        assertThat(log.getMessages()).contains("[INFO] Skipping javadoc generation, everything is up to date.");
    }

    private static class BufferingLog implements Log {
        private final List<String> messages = new ArrayList<>();

        public List<String> getMessages() {
            return messages;
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public void debug(CharSequence charSequence) {
            debug(charSequence, null);
        }

        @Override
        public void debug(CharSequence charSequence, Throwable throwable) {
            message("DEBUG", charSequence, throwable);
        }

        @Override
        public void debug(Throwable throwable) {
            debug(null, throwable);
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(CharSequence charSequence) {
            info(charSequence, null);
        }

        @Override
        public void info(CharSequence charSequence, Throwable throwable) {
            message("INFO", charSequence, throwable);
        }

        @Override
        public void info(Throwable throwable) {
            info(null, throwable);
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(CharSequence charSequence) {
            warn(charSequence, null);
        }

        @Override
        public void warn(CharSequence charSequence, Throwable throwable) {
            message("WARN", charSequence, throwable);
        }

        @Override
        public void warn(Throwable throwable) {
            warn(null, throwable);
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void error(CharSequence charSequence) {
            error(charSequence, null);
        }

        @Override
        public void error(CharSequence charSequence, Throwable throwable) {
            message("ERROR", charSequence, throwable);
        }

        @Override
        public void error(Throwable throwable) {
            error(null, throwable);
        }

        private void message(String level, CharSequence message, Throwable throwable) {
            messages.add("[" + level + "]" + (message != null ? " " + message : "")
                    + (throwable != null ? " " + throwable : ""));
        }
    }
}
