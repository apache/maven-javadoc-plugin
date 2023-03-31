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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.plugins.javadoc.ProxyServer.AuthAsyncProxyServlet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.hamcrest.MatcherAssert;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test {@link org.apache.maven.plugins.javadoc.JavadocReport} class.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class JavadocReportTest extends AbstractMojoTestCase {

    private static final char LINE_SEPARATOR = ' ';

    public static final String OPTIONS_UMLAUT_ENCODING = "Options Umlaut Encoding ö ä ü ß";

    /** flag to copy repo only one time */
    private static boolean TEST_REPO_CREATED = false;

    private Path unit;

    private File localRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(JavadocReportTest.class);

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        unit = new File(getBasedir(), "src/test/resources/unit").toPath();

        localRepo = new File(getBasedir(), "target/local-repo/");

        createTestRepo();
    }

    private JavadocReport lookupMojo(Path testPom) throws Exception {
        JavadocReport mojo = (JavadocReport) lookupMojo("javadoc", testPom.toFile());

        MojoExecution mojoExec = new MojoExecution(new Plugin(), "javadoc", null);

        setVariableValueToObject(mojo, "mojo", mojoExec);

        MavenProject currentProject = new MavenProjectStub();
        currentProject.setGroupId("GROUPID");
        currentProject.setArtifactId("ARTIFACTID");

        MavenSession session = newMavenSession(currentProject);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", session.getRepositorySession());
        return mojo;
    }

    /**
     * Create test repository in target directory.
     *
     * @throws IOException if any
     */
    private void createTestRepo() throws IOException {
        if (TEST_REPO_CREATED) {
            return;
        }

        localRepo.mkdirs();

        // ----------------------------------------------------------------------
        // UMLGraph
        // ----------------------------------------------------------------------

        Path sourceDir = unit.resolve("doclet-test/artifact-doclet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir.toFile(), localRepo);

        // ----------------------------------------------------------------------
        // UMLGraph-bis
        // ----------------------------------------------------------------------

        sourceDir = unit.resolve("doclet-path-test/artifact-doclet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir.toFile(), localRepo);

        // ----------------------------------------------------------------------
        // commons-attributes-compiler
        // http://www.tullmann.org/pat/taglets/
        // ----------------------------------------------------------------------

        sourceDir = unit.resolve("taglet-test/artifact-taglet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir.toFile(), localRepo);

        // ----------------------------------------------------------------------
        // stylesheetfile-test
        // ----------------------------------------------------------------------

        sourceDir = unit.resolve("stylesheetfile-test/artifact-stylesheetfile");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir.toFile(), localRepo);

        // ----------------------------------------------------------------------
        // helpfile-test
        // ----------------------------------------------------------------------

        sourceDir = unit.resolve("helpfile-test/artifact-helpfile");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir.toFile(), localRepo);

        // Remove SCM files
        List<String> files = FileUtils.getFileAndDirectoryNames(
                localRepo, FileUtils.getDefaultExcludesAsString(), null, true, true, true, true);
        for (String filename : files) {
            File file = new File(filename);

            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }

        TEST_REPO_CREATED = true;
    }

    /**
     * Convenience method that reads the contents of the specified file object into a string with a
     * <code>space</code> as line separator.
     *
     * @see #LINE_SEPARATOR
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile(Path file) throws IOException {
        return readFile(file, StandardCharsets.UTF_8);
    }

    /**
     * Convenience method that reads the contents of the specified file object into a string with a
     * <code>space</code> as line separator.
     *
     * @see #LINE_SEPARATOR
     * @param file the file to be read
     * @param cs charset to use
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile(Path file, Charset cs) throws IOException {
        StringBuilder str = new StringBuilder((int) Files.size(file));

        for (String strTmp : Files.readAllLines(file, cs)) {
            str.append(LINE_SEPARATOR);
            str.append(strTmp);
        }

        return str.toString();
    }

    /**
     * Test when default configuration is provided for the plugin
     *
     * @throws Exception if any
     */
    public void testDefaultConfiguration() throws Exception {
        Path testPom = unit.resolve("default-configuration/default-configuration-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        // package level generated javadoc files
        Path apidocs = new File(getBasedir(), "target/test/unit/default-configuration/target/site/apidocs").toPath();

        String appHtml = "def/configuration/App.html";
        Path generatedFile = apidocs.resolve(appHtml);
        assertThat(generatedFile).exists();

        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("16")) {
            String url = Objects.requireNonNull(mojo.getDefaultJavadocApiLink()).getUrl();
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            try {
                // only test when URL can be reached
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try {
                        assumeThat(connection.getURL().toString(), is(url));

                        // https://bugs.openjdk.java.net/browse/JDK-8216497
                        MatcherAssert.assertThat(
                                url + " available, but " + appHtml + " is missing link to java.lang.Object",
                                new String(Files.readAllBytes(generatedFile), StandardCharsets.UTF_8),
                                anyOf(
                                        containsString("/docs/api/java/lang/Object.html"),
                                        containsString("/docs/api/java.base/java/lang/Object.html")));
                    } catch (AssumptionViolatedException e) {
                        LOGGER.warn("ignoring defaultAPI check: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("error connecting to javadoc URL: {}", url);
                throw e;
            }
        } else {
            MatcherAssert.assertThat(
                    new String(Files.readAllBytes(generatedFile), StandardCharsets.UTF_8),
                    containsString("/docs/api/java.base/java/lang/Object.html"));
        }

        assertThat(apidocs.resolve("def/configuration/AppSample.html")).exists();
        assertThat(apidocs.resolve("def/configuration/package-summary.html")).exists();
        assertThat(apidocs.resolve("def/configuration/package-tree.html")).exists();
        assertThat(apidocs.resolve("def/configuration/package-use.html")).exists();

        // package-frame and allclasses-(no)frame not generated anymore since Java 11
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("11")) {
            assertThat(apidocs.resolve("def/configuration/package-frame.html")).exists();
            assertThat(apidocs.resolve("allclasses-frame.html"))
                    .exists()
                    .content()
                    .containsOnlyOnce("def/configuration/App.html")
                    .containsOnlyOnce("def/configuration/AppSample.html");
            assertThat(apidocs.resolve("allclasses-noframe.html"))
                    .exists()
                    .content()
                    .containsOnlyOnce("def/configuration/App.html")
                    .containsOnlyOnce("def/configuration/AppSample.html");
        }

        // class level generated javadoc files
        assertThat(apidocs.resolve("def/configuration/class-use/App.html")).exists();
        assertThat(apidocs.resolve("def/configuration/class-use/AppSample.html"))
                .exists();

        // project level generated javadoc files
        assertThat(apidocs.resolve("constant-values.html")).exists();
        assertThat(apidocs.resolve("deprecated-list.html")).exists();
        assertThat(apidocs.resolve("help-doc.html")).exists();
        assertThat(apidocs.resolve("index-all.html")).exists();
        assertThat(apidocs.resolve("index.html")).exists();
        assertThat(apidocs.resolve("overview-tree.html")).exists();
        assertThat(apidocs.resolve("stylesheet.css")).exists();

        if (JavaVersion.JAVA_VERSION.isAtLeast("10")) {
            assertThat(apidocs.resolve("element-list")).exists();
        } else {
            assertThat(apidocs.resolve("package-list")).exists();
        }
    }

    /**
     * Method for testing the subpackages and excludePackageNames parameter
     *
     * @throws Exception if any
     */
    public void testSubpackages() throws Exception {
        Path testPom = unit.resolve("subpackages-test/subpackages-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/subpackages-test/target/site/apidocs").toPath();

        // check the excluded packages
        assertThat(apidocs.resolve("subpackages/test/excluded")).doesNotExist();
        assertThat(apidocs.resolve("subpackages/test/included/exclude")).doesNotExist();

        // check if the classes in the specified subpackages were included
        assertThat(apidocs.resolve("subpackages/test/App.html")).exists();
        assertThat(apidocs.resolve("subpackages/test/AppSample.html")).exists();
        assertThat(apidocs.resolve("subpackages/test/included/IncludedApp.html"))
                .exists();
        assertThat(apidocs.resolve("subpackages/test/included/IncludedAppSample.html"))
                .exists();
    }

    public void testIncludesExcludes() throws Exception {
        Path testPom = unit.resolve("file-include-exclude-test/file-include-exclude-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs =
                new File(getBasedir(), "target/test/unit/file-include-exclude-test/target/site/apidocs").toPath();

        // check if the classes in the specified subpackages were included
        assertThat(apidocs.resolve("subpackages/test/App.html")).exists();
        assertThat(apidocs.resolve("subpackages/test/AppSample.html")).exists();
        assertThat(apidocs.resolve("subpackages/test/included/IncludedApp.html"))
                .exists();
        assertThat(apidocs.resolve("subpackages/test/included/IncludedAppSample.html"))
                .exists();
        assertThat(apidocs.resolve("subpackages/test/PariahApp.html")).doesNotExist();
    }

    /**
     * Test the recursion and exclusion of the doc-files subdirectories.
     *
     * @throws Exception if any
     */
    public void testDocfiles() throws Exception {
        // Should be an assumption, but not supported by TestCase
        // Seems like a bug in Javadoc 9 and above
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("9")) {
            return;
        }

        Path testPom = unit.resolve("docfiles-test/docfiles-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/docfiles-test/target/site/apidocs/").toPath();

        // check if the doc-files subdirectories were copied
        assertThat(apidocs.resolve("docfiles/test/doc-files")).exists();
        assertThat(apidocs.resolve("docfiles/test/doc-files/included-dir1/sample-included1.gif"))
                .exists();
        assertThat(apidocs.resolve("docfiles/test/doc-files/included-dir2/sample-included2.gif"))
                .exists();
        assertThat(apidocs.resolve("docfiles/test/doc-files/excluded-dir1")).doesNotExist();
        assertThat(apidocs.resolve("docfiles/test/doc-files/excluded-dir2")).doesNotExist();

        testPom = unit.resolve("docfiles-with-java-test/docfiles-with-java-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        mojo.execute();
    }

    /**
     * Test javadoc plugin using custom configuration. noindex, notree and nodeprecated parameters
     * were set to true.
     *
     * @throws Exception if any
     */
    public void testCustomConfiguration() throws Exception {
        Path testPom = unit.resolve("custom-configuration/custom-configuration-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/custom-configuration/target/site/apidocs").toPath();

        // check if there is a tree page generated (notree == true)
        assertThat(apidocs.resolve("overview-tree.html")).doesNotExist();
        assertThat(apidocs.resolve("custom/configuration/package-tree.html")).doesNotExist();

        // check if the main index page was generated (noindex == true)
        assertThat(apidocs.resolve("index-all.html")).doesNotExist();

        // check if the deprecated list and the deprecated api were generated (nodeprecated == true)
        // @todo Fix: the class-use of the deprecated api is still created eventhough the deprecated api of that class
        // is no longer generated
        assertThat(apidocs.resolve("deprecated-list.html")).doesNotExist();
        assertThat(apidocs.resolve("custom/configuration/App.html")).doesNotExist();

        // read the contents of the html files based on some of the parameter values
        // author == false
        String str = readFile(apidocs.resolve("custom/configuration/AppSample.html"));
        assertFalse(str.toLowerCase().contains("author"));

        // bottom
        assertTrue(str.toUpperCase().contains("SAMPLE BOTTOM CONTENT"));

        // offlineLinks
        if (JavaVersion.JAVA_VERSION.isBefore("11.0.2")) {
            assertThat(str)
                    .containsIgnoringCase("href=\"http://java.sun.com/j2se/1.4.2/docs/api/java/lang/string.html");
        } else {
            assertTrue(str.toLowerCase()
                    .contains("href=\"http://java.sun.com/j2se/1.4.2/docs/api/java.base/java/lang/string.html"));
        }

        // header
        assertTrue(str.toUpperCase().contains("MAVEN JAVADOC PLUGIN TEST"));

        // footer
        if (JavaVersion.JAVA_VERSION.isBefore("16-ea")
                && !System.getProperty("java.vm.name").contains("OpenJ9")) {
            assertTrue(str.toUpperCase().contains("MAVEN JAVADOC PLUGIN TEST FOOTER"));
        }

        // nohelp == true
        assertFalse(str.toUpperCase().contains("/HELP-DOC.HTML"));

        // check the wildcard (*) package exclusions -- excludePackageNames parameter
        assertThat(apidocs.resolve("custom/configuration/exclude1/Exclude1App.html"))
                .exists();
        assertThat(apidocs.resolve("custom/configuration/exclude1/subexclude/SubexcludeApp.html"))
                .doesNotExist();
        assertThat(apidocs.resolve("custom/configuration/exclude2/Exclude2App.html"))
                .doesNotExist();

        assertThat(apidocs.resolve("options")).isRegularFile();

        String contentOptions = new String(Files.readAllBytes(apidocs.resolve("options")), StandardCharsets.UTF_8);

        assertNotNull(contentOptions);
        assertThat(contentOptions).contains("-link").contains("http://java.sun.com/j2se/");
    }

    /**
     * Method to test the doclet artifact configuration
     *
     * @throws Exception if any
     */
    public void testDoclets() throws Exception {
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("13")) {
            // As of JDK 13, the com.sun.javadoc API is no longer supported.
            return;
        }

        // ----------------------------------------------------------------------
        // doclet-test: check if the file generated by UmlGraph exists and if
        // doclet path contains the UmlGraph artifact
        // ----------------------------------------------------------------------

        Path testPom = unit.resolve("doclet-test/doclet-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);

        MavenSession session = spy(newMavenSession(mojo.project));
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getRemoteRepositories()).thenReturn(mojo.project.getRemoteArtifactRepositories());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(localRepo)));
        when(buildingRequest.getRepositorySession()).thenReturn(repositorySession);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(session);

        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);
        mojo.execute();

        Path generatedFile =
                new File(getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot").toPath();
        assertThat(generatedFile).exists();

        Path optionsFile = new File(mojo.getOutputDirectory(), "options").toPath();
        assertThat(optionsFile).exists();
        String options = readFile(optionsFile);
        assertThat(options).contains("/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar");

        // ----------------------------------------------------------------------
        // doclet-path: check if the file generated by UmlGraph exists and if
        // doclet path contains the twice UmlGraph artifacts
        // ----------------------------------------------------------------------

        testPom = unit.resolve("doclet-path-test/doclet-path-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);
        mojo.execute();

        generatedFile = new File(getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot").toPath();
        assertThat(generatedFile).exists();

        optionsFile = new File(mojo.getOutputDirectory(), "options").toPath();
        assertThat(optionsFile).exists();
        options = readFile(optionsFile);
        assertThat(options)
                .contains("/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar")
                .contains("/target/local-repo/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.jar");
    }

    /**
     * Method to test when the path to the project sources has an apostrophe (')
     *
     * @throws Exception if any
     */
    public void testQuotedPath() throws Exception {
        Path testPom = unit.resolve("quotedpath'test/quotedpath-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs").toPath();

        // package level generated javadoc files
        assertThat(apidocs.resolve("quotedpath/test/App.html")).exists();
        assertThat(apidocs.resolve("quotedpath/test/AppSample.html")).exists();

        // project level generated javadoc files
        assertThat(apidocs.resolve("index-all.html")).exists();
        assertThat(apidocs.resolve("index.html")).exists();
        assertThat(apidocs.resolve("overview-tree.html")).exists();
        assertThat(apidocs.resolve("stylesheet.css")).exists();

        if (JavaVersion.JAVA_VERSION.isBefore("10")) {
            assertThat(apidocs.resolve("package-list")).exists();
        } else {
            assertThat(apidocs.resolve("element-list")).exists();
        }
    }

    /**
     * Method to test when the options file has umlauts.
     *
     * @throws Exception if any
     */
    public void testOptionsUmlautEncoding() throws Exception {
        Path testPom = unit.resolve("optionsumlautencoding-test/optionsumlautencoding-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path optionsFile = new File(mojo.getOutputDirectory(), "options").toPath();
        assertThat(optionsFile).exists();

        // check for a part of the window title
        String content;
        String expected;
        if (JavaVersion.JAVA_VERSION.isAtLeast("9") && JavaVersion.JAVA_VERSION.isBefore("12")) {
            content = readFile(optionsFile, StandardCharsets.UTF_8);
            expected = OPTIONS_UMLAUT_ENCODING;
        } else {
            content = readFile(optionsFile, Charset.defaultCharset());
            expected = new String(OPTIONS_UMLAUT_ENCODING.getBytes(Charset.defaultCharset()));
        }

        assertThat(content).contains(expected);

        Path apidocs =
                new File(getBasedir(), "target/test/unit/optionsumlautencoding-test/target/site/apidocs").toPath();

        // package level generated javadoc files
        assertThat(apidocs.resolve("optionsumlautencoding/test/App.html")).exists();
        assertThat(apidocs.resolve("optionsumlautencoding/test/AppSample.html")).exists();

        // project level generated javadoc files
        assertThat(apidocs.resolve("index-all.html")).exists();
        assertThat(apidocs.resolve("index.html")).exists();
        assertThat(apidocs.resolve("overview-tree.html")).exists();
        assertThat(apidocs.resolve("stylesheet.css")).exists();

        if (JavaVersion.JAVA_VERSION.isBefore("10")) {
            assertThat(apidocs.resolve("package-list")).exists();
        } else {
            assertThat(apidocs.resolve("element-list")).exists();
        }
    }

    /**
     * @throws Exception if any
     */
    public void testExceptions() throws Exception {
        try {
            Path testPom = unit.resolve("default-configuration/exception-test-plugin-config.xml");
            JavadocReport mojo = lookupMojo(testPom);
            mojo.execute();

            fail("Must throw exception.");
        } catch (Exception e) {
            assertTrue(true);

            try {
                deleteDirectory(new File(getBasedir(), "exception"));
            } catch (IOException ie) {
                // nop
            }
        }
    }

    /**
     * Method to test the taglet artifact configuration
     *
     * @throws Exception if any
     */
    public void testTaglets() throws Exception {
        // ----------------------------------------------------------------------
        // taglet-test: check if a taglet is used
        // ----------------------------------------------------------------------

        // Should be an assumption, but not supported by TestCase
        // com.sun.tools.doclets.Taglet not supported by Java9 anymore
        // Should be refactored with jdk.javadoc.doclet.Taglet
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("10")) {
            return;
        }

        Path testPom = unit.resolve("taglet-test/taglet-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);

        MavenSession session = spy(newMavenSession(mojo.project));
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getRemoteRepositories()).thenReturn(mojo.project.getRemoteArtifactRepositories());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(localRepo)));
        when(buildingRequest.getRepositorySession()).thenReturn(repositorySession);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(session);

        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);

        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/taglet-test/target/site/apidocs").toPath();

        assertThat(apidocs.resolve("index.html")).exists();

        Path appFile = apidocs.resolve("taglet/test/App.html");
        assertThat(appFile).exists();
        String appString = readFile(appFile);
        assertThat(appString).contains("<b>To Do:</b>");
    }

    /**
     * Method to test the jdk5 javadoc
     *
     * @throws Exception if any
     */
    public void testJdk5() throws Exception {
        // Should be an assumption, but not supported by TestCase
        // Java 5 not supported by Java9 anymore
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("9")) {
            return;
        }

        Path testPom = unit.resolve("jdk5-test/jdk5-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/jdk5-test/target/site/apidocs").toPath();

        assertThat(apidocs.resolve("index.html")).exists();

        Path overviewSummary = apidocs.resolve("overview-summary.html");
        assertThat(overviewSummary).exists();
        String content = readFile(overviewSummary);
        assertThat(content).contains("<b>Test the package-info</b>");

        Path packageSummary = apidocs.resolve("jdk5/test/package-summary.html");
        assertThat(packageSummary).exists();
        content = readFile(packageSummary);
        assertThat(content).contains("<b>Test the package-info</b>");
    }

    /**
     * Test to find the javadoc executable when <code>java.home</code> is not in the JDK_HOME. In this case, try to
     * use the <code>JAVA_HOME</code> environment variable.
     *
     * @throws Exception if any
     */
    public void testToFindJavadoc() throws Exception {
        String oldJreHome = System.getProperty("java.home");
        System.setProperty("java.home", "foo/bar");

        Path testPom = unit.resolve("javaHome-test/javaHome-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        System.setProperty("java.home", oldJreHome);
    }

    /**
     * Test the javadoc resources.
     *
     * @throws Exception if any
     */
    public void testJavadocResources() throws Exception {
        Path testPom = unit.resolve("resources-test/resources-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/resources-test/target/site/apidocs/").toPath();

        Path app = apidocs.resolve("resources/test/App.html");
        assertThat(app).exists();
        String content = readFile(app);
        assertThat(content).contains("<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">");
        assertThat(apidocs.resolve("resources/test/doc-files/maven-feather.png"))
                .exists();

        Path app2 = apidocs.resolve("resources/test2/App2.html");
        assertThat(app2).exists();
        content = readFile(app2);
        assertThat(content).contains("<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">");
        assertThat(apidocs.resolve("resources/test2/doc-files/maven-feather.png"))
                .doesNotExist();

        // with excludes
        testPom = unit.resolve("resources-with-excludes-test/resources-with-excludes-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        mojo.execute();

        apidocs = new File(getBasedir(), "target/test/unit/resources-with-excludes-test/target/site/apidocs").toPath();

        app = apidocs.resolve("resources/test/App.html");
        assertThat(app).exists();
        content = readFile(app);
        assertThat(content).contains("<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">");

        JavaVersion javadocVersion = (JavaVersion) getVariableValueFromObject(mojo, "javadocRuntimeVersion");
        if (javadocVersion.isAtLeast("1.8") /* && javadocVersion.isBefore( "14" ) */) {
            // https://bugs.openjdk.java.net/browse/JDK-8032205
            assertThat(apidocs.resolve("resources/test/doc-files/maven-feather.png"))
                    .as("Javadoc runtime version: " + javadocVersion
                            + "\nThis bug appeared in JDK8 and was planned to be fixed in JDK9, see JDK-8032205")
                    .exists();
        } else {
            assertThat(apidocs.resolve("resources/test/doc-files/maven-feather.png"))
                    .doesNotExist();
        }
        assertThat(apidocs.resolve("resources/test2/doc-files/maven-feather.png"))
                .exists();

        app2 = apidocs.resolve("resources/test2/App2.html");
        assertThat(app2).exists();
        content = readFile(app2);
        assertThat(content).contains("<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">");
        assertThat(apidocs.resolve("resources/test2/doc-files/maven-feather.png"))
                .exists();
    }

    /**
     * Test the javadoc for a POM project.
     *
     * @throws Exception if any
     */
    public void testPom() throws Exception {
        Path testPom = unit.resolve("pom-test/pom-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        assertThat(new File(getBasedir(), "target/test/unit/pom-test/target/site"))
                .doesNotExist();
    }

    /**
     * Test the javadoc with tag.
     *
     * @throws Exception if any
     */
    public void testTag() throws Exception {
        Path testPom = unit.resolve("tag-test/tag-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path app = new File(getBasedir(), "target/test/unit/tag-test/target/site/apidocs/tag/test/App.html").toPath();
        assertThat(app).exists();
        String readed = readFile(app);
        assertThat(readed).contains(">To do something:</").contains(">Generator Class:</");

        // In javadoc-options-javadoc-resources.xml tag 'version' has only a name,
        // which is not enough for Java 11 anymore
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("11")) {
            assertThat(readed).contains(">Version:</");
            assertTrue(readed.toLowerCase().contains("</dt>" + LINE_SEPARATOR + "  <dd>1.0</dd>")
                    || readed.toLowerCase().contains("</dt>" + LINE_SEPARATOR + "<dd>1.0</dd>" /* JDK 8 */));
        }
    }

    /**
     * Test newline in the header/footer parameter
     *
     * @throws Exception if any
     */
    public void testHeaderFooter() throws Exception {
        Path testPom = unit.resolve("header-footer-test/header-footer-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail("Doesnt handle correctly newline for header or footer parameter");
        }

        assertTrue(true);
    }

    /**
     * Test newline in various string parameters
     *
     * @throws Exception if any
     */
    public void testNewline() throws Exception {
        Path testPom = unit.resolve("newline-test/newline-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail("Doesn't handle correctly newline for string parameters. See options and packages files.");
        }

        assertTrue(true);
    }

    /**
     * Method to test the jdk6 javadoc
     *
     * @throws Exception if any
     */
    public void testJdk6() throws Exception {
        // Should be an assumption, but not supported by TestCase
        // Java 6 not supported by Java 12 anymore
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("12")) {
            return;
        }

        Path testPom = unit.resolve("jdk6-test/jdk6-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        mojo.execute();

        Path apidocs = new File(getBasedir(), "target/test/unit/jdk6-test/target/site/apidocs").toPath();
        assertThat(apidocs.resolve("index.html")).exists();

        Path overview;
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("11")) {
            overview = apidocs.resolve("overview-summary.html");
        } else {
            overview = apidocs.resolve("index.html");
        }

        assertThat(overview).exists();
        String content = readFile(overview);
        assertThat(content)
                .contains("Top - Copyright &#169; All rights reserved.")
                .contains("Header - Copyright &#169; All rights reserved.");
        // IBM dist of adopt-openj9 does not support the footer param
        if (!System.getProperty("java.vm.name").contains("OpenJ9")) {
            assertThat(content).contains("Footer - Copyright &#169; All rights reserved.");
        }

        Path packageSummary = apidocs.resolve("jdk6/test/package-summary.html");
        assertThat(packageSummary).exists();
        content = readFile(packageSummary);
        assertThat(content)
                .contains("Top - Copyright &#169; All rights reserved.")
                .contains("Header - Copyright &#169; All rights reserved.");

        // IBM dist of adopt-openj9 does not support the footer param
        if (!System.getProperty("java.vm.name").contains("OpenJ9")) {
            assertThat(content).contains("Footer - Copyright &#169; All rights reserved.");
        }
    }

    /**
     * Method to test proxy support in the javadoc
     *
     * @throws Exception if any
     */
    public void testProxy() throws Exception {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();

        // dummy proxy
        proxy.setActive(true);
        proxy.setHost("127.0.0.1");
        proxy.setPort(80);
        proxy.setProtocol("http");
        proxy.setUsername("toto");
        proxy.setPassword("toto");
        proxy.setNonProxyHosts("www.google.com|*.somewhere.com");
        settings.addProxy(proxy);

        Path testPom =
                new File(getBasedir(), "src/test/resources/unit/proxy-test/proxy-test-plugin-config.xml").toPath();
        JavadocReport mojo = lookupMojo(testPom);

        MavenSession session = spy(newMavenSession(mojo.project));
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getRemoteRepositories()).thenReturn(mojo.project.getRemoteArtifactRepositories());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(localRepo)));
        when(buildingRequest.getRepositorySession()).thenReturn(repositorySession);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(session);

        setVariableValueToObject(mojo, "settings", settings);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);
        mojo.execute();

        Path commandLine = new File(
                        getBasedir(),
                        "target/test/unit/proxy-test/target/site/apidocs/javadoc."
                                + (SystemUtils.IS_OS_WINDOWS ? "bat" : "sh"))
                .toPath();
        assertThat(commandLine).exists();
        String readed = readFile(commandLine);
        assertThat(readed).contains("-J-Dhttp.proxyHost=127.0.0.1").contains("-J-Dhttp.proxyPort=80");
        if (SystemUtils.IS_OS_WINDOWS) {
            assertThat(readed).contains(" -J-Dhttp.nonProxyHosts=\"www.google.com^|*.somewhere.com\" ");
        } else {
            assertThat(readed).contains(" \"-J-Dhttp.nonProxyHosts=\\\"www.google.com^|*.somewhere.com\\\"\" ");
        }

        Path options = new File(getBasedir(), "target/test/unit/proxy-test/target/site/apidocs/options").toPath();
        assertThat(options).exists();
        String optionsContent = readFile(options);
        // NO -link expected
        assertThat(optionsContent).doesNotContain("-link");

        // real proxy
        ProxyServer proxyServer = null;
        AuthAsyncProxyServlet proxyServlet;
        try {
            proxyServlet = new AuthAsyncProxyServlet();
            proxyServer = new ProxyServer(proxyServlet);
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            settings.addProxy(proxy);

            mojo = lookupMojo(testPom);
            setVariableValueToObject(mojo, "settings", settings);
            setVariableValueToObject(mojo, "session", session);
            setVariableValueToObject(mojo, "repoSession", repositorySession);
            mojo.execute();
            readed = readFile(commandLine);
            assertTrue(readed.contains("-J-Dhttp.proxyHost=" + proxyServer.getHostName()));
            assertTrue(readed.contains("-J-Dhttp.proxyPort=" + proxyServer.getPort()));

            optionsContent = readFile(options);
            // -link expected
            // TODO: This got disabled for now!
            // This test fails since the last commit but I actually think it only ever worked by accident.
            // It did rely on a commons-logging-1.0.4.pom which got resolved by a test which did run previously.
            // But after updating to commons-logging.1.1.1 there is no pre-resolved artifact available in
            // target/local-repo anymore, thus the javadoc link info cannot get built and the test fails
            // I'll for now just disable this line of code, because the test as far as I can see _never_
            // did go upstream. The remoteRepository list used is always empty!.
            //
            //            assertTrue( optionsContent.contains( "-link 'http://commons.apache.org/logging/apidocs'" ) );
        } finally {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        }

        // auth proxy
        Map<String, String> authentications = new HashMap<>();
        authentications.put("foo", "bar");
        try {
            proxyServlet = new AuthAsyncProxyServlet(authentications);
            proxyServer = new ProxyServer(proxyServlet);
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            proxy.setUsername("foo");
            proxy.setPassword("bar");
            settings.addProxy(proxy);

            mojo = lookupMojo(testPom);
            setVariableValueToObject(mojo, "settings", settings);
            setVariableValueToObject(mojo, "session", session);
            setVariableValueToObject(mojo, "repoSession", repositorySession);
            mojo.execute();
            readed = readFile(commandLine);
            assertThat(readed)
                    .contains("-J-Dhttp.proxyHost=" + proxyServer.getHostName())
                    .contains("-J-Dhttp.proxyPort=" + proxyServer.getPort());

            optionsContent = readFile(options);
            // -link expected
            // see comment above (line 829)
            //             assertTrue( optionsContent.contains( "-link 'http://commons.apache.org/logging/apidocs'" ) );
        } finally {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        }
    }

    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    public void testValidateOptions() throws Exception {
        // encoding
        Path testPom = unit.resolve("validate-options-test/wrong-encoding-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);
        try {
            mojo.execute();
            fail("No wrong encoding catch");
        } catch (MojoExecutionException e) {
            assertTrue("No wrong encoding catch", e.getMessage().contains("Unsupported option <encoding/>"));
        }
        testPom = unit.resolve("validate-options-test/wrong-docencoding-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        try {
            mojo.execute();
            fail("No wrong docencoding catch");
        } catch (MojoExecutionException e) {
            assertTrue("No wrong docencoding catch", e.getMessage().contains("Unsupported option <docencoding/>"));
        }
        testPom = unit.resolve("validate-options-test/wrong-charset-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        try {
            mojo.execute();
            fail("No wrong charset catch");
        } catch (MojoExecutionException e) {
            assertTrue("No wrong charset catch", e.getMessage().contains("Unsupported option <charset/>"));
        }

        // locale
        testPom = unit.resolve("validate-options-test/wrong-locale-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        try {
            mojo.execute();
            fail("No wrong locale catch");
        } catch (MojoExecutionException e) {
            assertTrue("No wrong locale catch", e.getMessage().contains("Unsupported option <locale/>"));
        }
        testPom = unit.resolve("validate-options-test/wrong-locale-with-variant-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        mojo.execute();
        assertTrue("No wrong locale catch", true);

        // conflict options
        testPom = unit.resolve("validate-options-test/conflict-options-test-plugin-config.xml");
        mojo = lookupMojo(testPom);
        try {
            mojo.execute();
            fail("No conflict catch");
        } catch (MojoExecutionException e) {
            assertTrue("No conflict catch", e.getMessage().contains("Option <nohelp/> conflicts with <helpfile/>"));
        }
    }

    /**
     * Method to test the <code>&lt;tagletArtifacts/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testTagletArtifacts() throws Exception {
        // Should be an assumption, but not supported by TestCase
        // com.sun.tools.doclets.Taglet not supported by Java 10 anymore
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("10")) {
            return;
        }

        Path testPom = unit.resolve("tagletArtifacts-test/tagletArtifacts-test-plugin-config.xml");
        JavadocReport mojo = lookupMojo(testPom);

        MavenSession session = spy(newMavenSession(mojo.project));
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getRemoteRepositories()).thenReturn(mojo.project.getRemoteArtifactRepositories());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(localRepo)));
        when(buildingRequest.getRepositorySession()).thenReturn(repositorySession);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(session);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);
        mojo.execute();

        Path optionsFile = new File(mojo.getOutputDirectory(), "options").toPath();
        assertThat(optionsFile).exists();
        String options = readFile(optionsFile);
        // count -taglet
        assertThat(StringUtils.countMatches(options, LINE_SEPARATOR + "-taglet" + LINE_SEPARATOR))
                .isEqualTo(3);
        assertThat(options)
                .contains("org.codehaus.plexus.javadoc.PlexusConfigurationTaglet")
                .contains("org.codehaus.plexus.javadoc.PlexusRequirementTaglet")
                .contains("org.codehaus.plexus.javadoc.PlexusComponentTaglet");
    }

    /**
     * Method to test the <code>&lt;stylesheetfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testStylesheetfile() throws Exception {
        Path testPom = unit.resolve("stylesheetfile-test/pom.xml");

        JavadocReport mojo = lookupMojo(testPom);
        assertNotNull(mojo);

        MavenSession session = spy(newMavenSession(mojo.project));
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getRemoteRepositories()).thenReturn(mojo.project.getRemoteArtifactRepositories());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(localRepo)));
        when(buildingRequest.getRepositorySession()).thenReturn(repositorySession);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(session);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);

        Path apidocs = new File(getBasedir(), "target/test/unit/stylesheetfile-test/target/site/apidocs").toPath();

        Path stylesheetfile = apidocs.resolve("stylesheet.css");
        Path options = apidocs.resolve("options");

        // stylesheet == maven OR java
        setVariableValueToObject(mojo, "stylesheet", "javamaven");

        try {
            mojo.execute();
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }

        // stylesheet == java
        setVariableValueToObject(mojo, "stylesheet", "java");
        mojo.execute();

        String content = readFile(stylesheetfile);
        if (JavaVersion.JAVA_VERSION.isAtLeast("13-ea")) {
            assertTrue(content.contains("/*" + LINE_SEPARATOR + " * Javadoc style sheet" + LINE_SEPARATOR + " */"));
        } else if (JavaVersion.JAVA_VERSION.isAtLeast("10")) {
            assertTrue(content.contains("/* " + LINE_SEPARATOR + " * Javadoc style sheet" + LINE_SEPARATOR + " */"));
        } else {
            assertTrue(content.contains("/* Javadoc style sheet */"));
        }

        String optionsContent = readFile(options);
        assertFalse(optionsContent.contains("-stylesheetfile"));

        // stylesheetfile defined as a project resource
        setVariableValueToObject(mojo, "stylesheet", null);
        setVariableValueToObject(mojo, "stylesheetfile", "com/mycompany/app/javadoc/css/stylesheet.css");
        mojo.execute();

        content = readFile(stylesheetfile);
        assertTrue(content.contains("/* Custom Javadoc style sheet in project */"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-stylesheetfile"));
        Path stylesheetResource =
                unit.resolve("stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css/stylesheet.css");
        assertTrue(optionsContent.contains(
                "'" + stylesheetResource.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));

        // stylesheetfile defined in a javadoc plugin dependency
        setVariableValueToObject(mojo, "stylesheetfile", "com/mycompany/app/javadoc/css2/stylesheet.css");
        mojo.execute();

        content = readFile(stylesheetfile);
        assertTrue(content.contains("/* Custom Javadoc style sheet in artefact */"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-stylesheetfile"));
        assertTrue(optionsContent.contains(
                "'" + stylesheetfile.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));

        // stylesheetfile defined as file
        Path css = unit.resolve("stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css");
        setVariableValueToObject(mojo, "stylesheetfile", css.toFile().getAbsolutePath());
        mojo.execute();

        content = readFile(stylesheetfile);
        assertTrue(content.contains("/* Custom Javadoc style sheet as file */"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-stylesheetfile"));
        stylesheetResource =
                unit.resolve("stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css");
        assertTrue(optionsContent.contains(
                "'" + stylesheetResource.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));
    }

    /**
     * Method to test the <code>&lt;helpfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testHelpfile() throws Exception {
        Path testPom = unit.resolve("helpfile-test/pom.xml");

        JavadocReport mojo = lookupMojo(testPom);
        assertNotNull(mojo);

        MavenSession session = spy(newMavenSession(mojo.project));
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getRemoteRepositories()).thenReturn(mojo.project.getRemoteArtifactRepositories());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(localRepo)));
        when(buildingRequest.getRepositorySession()).thenReturn(repositorySession);
        when(session.getRepositorySession()).thenReturn(repositorySession);
        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(session);
        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);

        Path apidocs = new File(getBasedir(), "target/test/unit/helpfile-test/target/site/apidocs").toPath();

        Path helpfile = apidocs.resolve("help-doc.html");
        Path options = apidocs.resolve("options");

        // helpfile by default
        mojo.execute();

        String content = readFile(helpfile);
        assertTrue(content.contains("<!-- Generated by javadoc"));

        String optionsContent = readFile(options);
        assertFalse(optionsContent.contains("-helpfile"));

        // helpfile defined in a javadoc plugin dependency
        setVariableValueToObject(mojo, "helpfile", "com/mycompany/app/javadoc/helpfile/help-doc.html");

        setVariableValueToObject(mojo, "session", session);
        setVariableValueToObject(mojo, "repoSession", repositorySession);

        mojo.execute();

        content = readFile(helpfile);
        assertTrue(content.contains("<!--  Help file from artefact -->"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-helpfile"));
        Path help = apidocs.resolve("help-doc.html");
        assertTrue(optionsContent.contains("'" + help.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));

        // helpfile defined as a project resource
        setVariableValueToObject(mojo, "helpfile", "com/mycompany/app/javadoc/helpfile2/help-doc.html");
        mojo.execute();

        content = readFile(helpfile);
        assertTrue(content.contains("<!--  Help file from file -->"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-helpfile"));
        help = unit.resolve("helpfile-test/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html");
        assertTrue(optionsContent.contains("'" + help.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));

        // helpfile defined as file
        help = unit.resolve("helpfile-test/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html");
        setVariableValueToObject(mojo, "helpfile", help.toFile().getAbsolutePath());
        mojo.execute();

        content = readFile(helpfile);
        assertTrue(content.contains("<!--  Help file from file -->"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-helpfile"));
        assertTrue(optionsContent.contains("'" + help.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));
    }
}
