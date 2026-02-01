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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.api.plugin.testing.MojoExtension.getTestFile;
import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

/**
 * Test {@link org.apache.maven.plugins.javadoc.JavadocReport} class.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
@MojoTest(realRepositorySession = true)
class JavadocReportTest {

    @Inject
    private MavenSession mavenSession;

    @Inject
    private Log log;

    private static final char LINE_SEPARATOR = ' ';

    private static final String OPTIONS_UMLAUT_ENCODING = "Options Umlaut Encoding ö ä ü ß";

    private File localRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(JavadocReportTest.class);

    @BeforeEach
    void setUp(@TempDir Path tempDirectory) throws Exception {
        localRepo = tempDirectory.resolve(Paths.get("target/local-repo/")).toFile();
        mavenSession.getRequest().setLocalRepositoryPath(localRepo);
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
    @InjectMojo(goal = "javadoc", pom = "default-configuration-plugin-config.xml")
    @MojoParameter(
            name = "detectOfflineLinks",
            value = "false") // before refactoring this parameter was set to false (root cause unknown)
    @Basedir("/unit/default-configuration")
    @Test
    void testDefaultConfiguration(JavadocReport mojo) throws Exception {
        mojo.execute();

        // package level generated javadoc files
        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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
                        assumeThat(connection.getURL().toString()).isEqualTo(url);

                        // https://bugs.openjdk.java.net/browse/JDK-8216497
                        MatcherAssert.assertThat(
                                url + " available, but " + appHtml + " is missing link to java.lang.Object",
                                new String(Files.readAllBytes(generatedFile), StandardCharsets.UTF_8),
                                anyOf(
                                        containsString("/docs/api/java/lang/Object.html"),
                                        containsString("/docs/api/java.base/java/lang/Object.html")));
                    } catch (TestAbortedException e) {
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
        if (JavaVersion.JAVA_VERSION.isAtLeast("23")) {
            assertThat(apidocs.resolve("resource-files/stylesheet.css")).exists();
        } else {

            assertThat(apidocs.resolve("stylesheet.css")).exists();
        }

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
    @InjectMojo(goal = "javadoc", pom = "subpackages-test-plugin-config.xml")
    @Basedir("/unit/subpackages-test")
    @Test
    void testSubpackages(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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

    @InjectMojo(goal = "javadoc", pom = "file-include-exclude-plugin-config.xml")
    @Basedir("/unit/file-include-exclude-test")
    @Test
    void testIncludesExcludes(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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
    @InjectMojo(goal = "javadoc", pom = "docfiles-test-plugin-config.xml")
    @Basedir("/unit/docfiles-test")
    @Test
    @EnabledOnJre(JRE.JAVA_8) // Seems like a bug in Javadoc 9 and above
    void testDocfiles(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs/").toPath();

        // check if the doc-files subdirectories were copied
        assertThat(apidocs.resolve("docfiles/test/doc-files")).exists();
        assertThat(apidocs.resolve("docfiles/test/doc-files/included-dir1/sample-included1.gif"))
                .exists();
        assertThat(apidocs.resolve("docfiles/test/doc-files/included-dir2/sample-included2.gif"))
                .exists();
        assertThat(apidocs.resolve("docfiles/test/doc-files/excluded-dir1")).doesNotExist();
        assertThat(apidocs.resolve("docfiles/test/doc-files/excluded-dir2")).doesNotExist();
    }

    @InjectMojo(goal = "javadoc", pom = "docfiles-with-java-test-plugin-config.xml")
    @Basedir("/unit/docfiles-with-java-test")
    @Test
    @EnabledOnJre(JRE.JAVA_8)
    void testDocfilesWithJava(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs/").toPath();

        // check if the doc-files subdirectories were copied
        assertThat(apidocs.resolve("test/doc-files")).exists();
        assertThat(apidocs.resolve("test/doc-files/App.java")).exists();
        assertThat(apidocs.resolve("test/App.html")).exists();
    }

    /**
     * Test javadoc plugin using custom configuration. noindex, notree and nodeprecated parameters
     * were set to true.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "custom-configuration-plugin-config.xml")
    @Basedir("/unit/custom-configuration")
    @Test
    void testCustomConfiguration(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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
        assertFalse(str.toLowerCase(Locale.ENGLISH).contains("author"));

        // bottom
        assertTrue(str.toUpperCase(Locale.ENGLISH).contains("SAMPLE BOTTOM CONTENT"));

        // offlineLinks
        if (JavaVersion.JAVA_VERSION.isBefore("11.0.2")) {
            // some java 8 jdks produce a link to oracle
            assertThat(str)
                    .containsAnyOf(
                            "href=\"http://java.sun.com/j2se/1.4.2/docs/api/java/lang/string.html",
                            "href=\"https://docs.oracle.com/javase/8/docs/api/java/lang/String.html");

        } else {
            assertTrue(str.toLowerCase(Locale.ENGLISH)
                    .contains("href=\"http://java.sun.com/j2se/1.4.2/docs/api/java.base/java/lang/string.html"));
        }

        // header
        assertTrue(str.toUpperCase(Locale.ENGLISH).contains("MAVEN JAVADOC PLUGIN TEST"));

        // footer
        if (JavaVersion.JAVA_VERSION.isBefore("16-ea")
                && !System.getProperty("java.vm.name").contains("OpenJ9")) {
            assertTrue(str.toUpperCase(Locale.ENGLISH).contains("MAVEN JAVADOC PLUGIN TEST FOOTER"));
        }

        // nohelp == true
        assertFalse(str.toUpperCase(Locale.ENGLISH).contains("/HELP-DOC.HTML"));

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
    @InjectMojo(goal = "javadoc", pom = "doclet-test-plugin-config.xml")
    @Basedir("/unit/doclet-test")
    @Test
    @EnabledForJreRange(max = JRE.JAVA_12) // As of JDK 13, the com.sun.javadoc API is no longer supported.
    void testDoclets(JavadocReport mojo) throws Exception {

        // ----------------------------------------------------------------------
        // doclet-test: check if the file generated by UmlGraph exists and if
        // doclet path contains the UmlGraph artifact
        // ----------------------------------------------------------------------

        File sourceDir = getTestFile("artifact-doclet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir, localRepo);

        mojo.execute();

        Path generatedFile = new File(getBasedir(), "/target/site/apidocs/graph.dot").toPath();
        assertThat(generatedFile).exists();

        Path optionsFile = new File(mojo.getPluginReportOutputDirectory(), "options").toPath();
        assertThat(optionsFile).exists();
        String options = readFile(optionsFile);
        assertThat(options).contains("/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar");
    }

    @InjectMojo(goal = "javadoc", pom = "doclet-path-test-plugin-config.xml")
    @Basedir("/unit/doclet-path-test")
    @Test
    @EnabledForJreRange(max = JRE.JAVA_12) // As of JDK 13, the com.sun.javadoc API is no longer supported.
    void testDocletsPath(JavadocReport mojo) throws Exception {

        ////     ----------------------------------------------------------------------
        ////     doclet-path: check if the file generated by UmlGraph exists and if
        ////     doclet path contains the twice UmlGraph artifacts
        ////     ----------------------------------------------------------------------

        File sourceDir = getTestFile("artifact-doclet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir, localRepo);

        sourceDir = getTestFile("../doclet-test/artifact-doclet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir, localRepo);

        mojo.execute();

        Path generatedFile = new File(getBasedir(), "/target/site/apidocs/graph.dot").toPath();
        assertThat(generatedFile).exists();

        Path optionsFile = new File(mojo.getPluginReportOutputDirectory(), "options").toPath();
        assertThat(optionsFile).exists();
        String options = readFile(optionsFile);
        assertThat(options)
                .contains("/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar")
                .contains("/target/local-repo/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.jar");
    }

    /**
     * Method to test when the path to the project sources has an apostrophe (')
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "quotedpath-test-plugin-config.xml")
    @Basedir("/unit/quotedpath'test")
    @Test
    void testQuotedPath(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

        // package level generated javadoc files
        assertThat(apidocs.resolve("quotedpath/test/App.html")).exists();
        assertThat(apidocs.resolve("quotedpath/test/AppSample.html")).exists();

        // project level generated javadoc files
        assertThat(apidocs.resolve("index-all.html")).exists();
        assertThat(apidocs.resolve("index.html")).exists();
        assertThat(apidocs.resolve("overview-tree.html")).exists();
        if (JavaVersion.JAVA_VERSION.isAtLeast("23")) {
            assertThat(apidocs.resolve("resource-files/stylesheet.css")).exists();
        } else {
            assertThat(apidocs.resolve("stylesheet.css")).exists();
        }

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
    @InjectMojo(goal = "javadoc", pom = "optionsumlautencoding-test-plugin-config.xml")
    @Basedir("/unit/optionsumlautencoding-test")
    @Test
    void testOptionsUmlautEncoding(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path optionsFile = new File(mojo.getPluginReportOutputDirectory(), "options").toPath();
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

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

        // package level generated javadoc files
        assertThat(apidocs.resolve("optionsumlautencoding/test/App.html")).exists();
        assertThat(apidocs.resolve("optionsumlautencoding/test/AppSample.html")).exists();

        // project level generated javadoc files
        assertThat(apidocs.resolve("index-all.html")).exists();
        assertThat(apidocs.resolve("index.html")).exists();
        assertThat(apidocs.resolve("overview-tree.html")).exists();
        if (JavaVersion.JAVA_VERSION.isAtLeast("23")) {
            assertThat(apidocs.resolve("resource-files/stylesheet.css")).exists();
        } else {
            assertThat(apidocs.resolve("stylesheet.css")).exists();
        }

        if (JavaVersion.JAVA_VERSION.isBefore("10")) {
            assertThat(apidocs.resolve("package-list")).exists();
        } else {
            assertThat(apidocs.resolve("element-list")).exists();
        }
    }

    /**
     * Method to test the taglet artifact configuration
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "taglet-test-plugin-config.xml")
    @Basedir("/unit/taglet-test")
    @Test
    @EnabledForJreRange(max = JRE.JAVA_9)
    void testTaglets(JavadocReport mojo) throws Exception {
        // ----------------------------------------------------------------------
        // taglet-test: check if a taglet is used
        // ----------------------------------------------------------------------
        // com.sun.tools.doclets.Taglet not supported by Java9 anymore
        // Should be refactored with jdk.javadoc.doclet.Taglet

        File sourceDir = getTestFile("artifact-taglet");
        assertThat(sourceDir).exists();
        copyDirectory(sourceDir, localRepo);

        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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
    @InjectMojo(goal = "javadoc", pom = "jdk5-test-plugin-config.xml")
    @Basedir("/unit/jdk5-test")
    @Test
    @EnabledForJreRange(max = JRE.JAVA_8)
    void testJdk5(JavadocReport mojo) throws Exception {
        // Java 5 not supported by Java9 anymore

        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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
    @InjectMojo(goal = "javadoc", pom = "javaHome-test-plugin-config.xml")
    @Basedir("/unit/javaHome-test")
    @Test
    void testToFindJavadoc(JavadocReport mojo) throws Exception {
        String oldJreHome = System.getProperty("java.home");
        System.setProperty("java.home", "foo/bar");

        mojo.execute();

        System.setProperty("java.home", oldJreHome);
    }

    /**
     * Test the javadoc resources.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "resources-test-plugin-config.xml")
    @Basedir("/unit/resources-test")
    @Test
    void testJavadocResources(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs/").toPath();

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
    }

    /**
     * Test the javadoc resources.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "resources-with-excludes-test-plugin-config.xml")
    @Basedir("/unit/resources-with-excludes-test")
    @Test
    void testJavadocResourcesWithExcludes(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

        Path app = apidocs.resolve("resources/test/App.html");
        assertThat(app).exists();

        assertThat(apidocs.resolve("resources/test/doc-files/maven-feather.png"))
                .exists();

        assertThat(apidocs.resolve("resources/test2/doc-files/maven-feather.png"))
                .exists();

        Path app2 = apidocs.resolve("resources/test2/App2.html");
        assertThat(app2).exists();
        String content = readFile(app2);
        assertThat(content).contains("<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">");
        assertThat(apidocs.resolve("resources/test2/doc-files/maven-feather.png"))
                .exists();
    }

    /**
     * Test the javadoc for a POM project.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "pom-test-plugin-config.xml")
    @Basedir("/unit/pom-test")
    @Test
    void testPom(JavadocReport mojo) throws Exception {

        mojo.getProject().setPackaging("pom");

        mojo.execute();

        assertThat(new File(getBasedir(), "/target/site")).doesNotExist();

        verify(log).info(contains("Skipping org.apache.maven.plugins:maven-javadoc-plugin"));
    }

    /**
     * Test the javadoc with tag.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "tag-test-plugin-config.xml")
    @Basedir("/unit/tag-test")
    @Test
    void testTag(JavadocReport mojo) throws Exception {
        mojo.execute();

        Path app = new File(getBasedir(), "/target/site/apidocs/tag/test/App.html").toPath();
        assertThat(app).exists();
        String readed = readFile(app);
        assertThat(readed).contains(">To do something:</").contains(">Generator Class:</");

        // In javadoc-options-javadoc-resources.xml tag 'version' has only a name,
        // which is not enough for Java 11 anymore
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("11")) {
            assertThat(readed).contains(">Version:</");
            assertTrue(readed.toLowerCase(Locale.ENGLISH).contains("</dt>" + LINE_SEPARATOR + "  <dd>1.0</dd>")
                    || readed.toLowerCase(Locale.ENGLISH)
                            .contains("</dt>" + LINE_SEPARATOR + "<dd>1.0</dd>" /* JDK 8 */));
        }
    }

    /**
     * Test newline in the header/footer parameter
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "header-footer-test-plugin-config.xml")
    @Basedir("/unit/header-footer-test")
    @Test
    void testHeaderFooter(JavadocReport mojo) throws Exception {
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail("Doesnt handle correctly newline for header or footer parameter");
        }
    }

    /**
     * Test newline in various string parameters
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "newline-test-plugin-config.xml")
    @Basedir("/unit/newline-test")
    @Test
    void testNewline(JavadocReport mojo) throws Exception {
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail("Doesn't handle correctly newline for string parameters. See options and packages files.");
        }
    }

    /**
     * Method to test the jdk6 javadoc
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "jdk6-test-plugin-config.xml")
    @Basedir("/unit/jdk6-test")
    @EnabledForJreRange(max = JRE.JAVA_11)
    @Test
    void testJdk6(JavadocReport mojo) throws Exception {
        // Java 6 not supported by Java 12 anymore
        mojo.execute();

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();
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
    @InjectMojo(goal = "javadoc", pom = "proxy-test-plugin-config.xml")
    @Basedir("/unit/proxy-test")
    @Test
    void testProxyWithDummy(JavadocReport mojo) throws Exception {

        mojo.getProject().setDependencyArtifacts(Collections.emptySet());

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

        setVariableValueToObject(mojo, "settings", settings);

        mojo.execute();

        Path commandLine = new File(
                        getBasedir(), "/target/site/apidocs/javadoc." + (SystemUtils.IS_OS_WINDOWS ? "bat" : "sh"))
                .toPath();
        assertThat(commandLine).exists();
        String readed = readFile(commandLine);
        assertThat(readed).contains("-J-Dhttp.proxyHost=127.0.0.1").contains("-J-Dhttp.proxyPort=80");
        if (SystemUtils.IS_OS_WINDOWS) {
            assertThat(readed).contains(" -J-Dhttp.nonProxyHosts=\"www.google.com^|*.somewhere.com\" ");
        } else {
            assertThat(readed).contains(" \"-J-Dhttp.nonProxyHosts=\\\"www.google.com^|*.somewhere.com\\\"\" ");
        }

        Path options = new File(getBasedir(), "/target/site/apidocs/options").toPath();
        assertThat(options).exists();
        String optionsContent = readFile(options);
        // NO -link expected
        assertThat(optionsContent).doesNotContain("-link");
    }

    /**
     * Method to test proxy support in the javadoc
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "proxy-test-plugin-config.xml")
    @MojoParameter(
            name = "detectOfflineLinks",
            value = "false") // before refactoring this parameter was set to false (root cause unknown)
    @Basedir("/unit/proxy-test")
    @Test
    void testRealProxy(JavadocReport mojo) throws Exception {
        mojo.getProject().setDependencyArtifacts(Collections.emptySet());

        // real proxy
        ProxyServer proxyServer = null;
        ProxyServer.AuthAsyncProxyServlet proxyServlet;
        try {
            proxyServlet = new ProxyServer.AuthAsyncProxyServlet();
            proxyServer = new ProxyServer(proxyServlet);
            proxyServer.start();

            Settings settings = new Settings();
            Proxy proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            settings.addProxy(proxy);

            setVariableValueToObject(mojo, "settings", settings);

            mojo.execute();
            Path commandLine = new File(
                            getBasedir(), "/target/site/apidocs/javadoc." + (SystemUtils.IS_OS_WINDOWS ? "bat" : "sh"))
                    .toPath();
            String readed = readFile(commandLine);
            assertTrue(readed.contains("-J-Dhttp.proxyHost=" + proxyServer.getHostName()));
            assertTrue(readed.contains("-J-Dhttp.proxyPort=" + proxyServer.getPort()));

            Path options = new File(getBasedir(), "/target/site/apidocs/options").toPath();
            assertThat(options).exists();
            //            String optionsContent = readFile(options);
            // -link expected
            // TODO: This got disabled for now!
            // This test fails since the last commit but I actually think it only ever worked by accident.
            // It did rely on a commons-logging-1.0.4.pom which got resolved by a test which did run
            //         previously.
            // But after updating to commons-logging.1.1.1 there is no pre-resolved artifact available in
            // target/local-repo anymore, thus the javadoc link info cannot get built and the test fails
            // I'll for now just disable this line of code, because the test as far as I can see _never_
            // did go upstream. The remoteRepository list used is always empty!.
            //
            //            assertTrue( optionsContent.contains( "-link
            //         'http://commons.apache.org/logging/apidocs'"
            //             ) );
        } finally {
            if (proxyServer != null) {
                proxyServer.stop();
            }
        }
    }

    /**
     * Method to test proxy support in the javadoc
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "proxy-test-plugin-config.xml")
    @Basedir("/unit/proxy-test")
    @Test
    void testAuthProxy(JavadocReport mojo) throws Exception {
        mojo.getProject().setDependencyArtifacts(Collections.emptySet());

        // auth proxy
        Map<String, String> authentications = new HashMap<>();
        authentications.put("foo", "bar");
        ProxyServer proxyServer = null;
        ProxyServer.AuthAsyncProxyServlet proxyServlet;
        try {
            proxyServlet = new ProxyServer.AuthAsyncProxyServlet(authentications);
            proxyServer = new ProxyServer(proxyServlet);
            proxyServer.start();

            Settings settings = new Settings();
            Proxy proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            proxy.setUsername("foo");
            proxy.setPassword("bar");
            settings.addProxy(proxy);

            setVariableValueToObject(mojo, "settings", settings);

            mojo.execute();
            Path commandLine = new File(
                            getBasedir(), "/target/site/apidocs/javadoc." + (SystemUtils.IS_OS_WINDOWS ? "bat" : "sh"))
                    .toPath();
            String readed = readFile(commandLine);
            assertTrue(readed.contains("-J-Dhttp.proxyHost=" + proxyServer.getHostName()));
            assertTrue(readed.contains("-J-Dhttp.proxyPort=" + proxyServer.getPort()));

            Path options = new File(getBasedir(), "/target/site/apidocs/options").toPath();
            assertThat(options).exists();
            //            String optionsContent = readFile(options);
            // -link expected
            // TODO: This got disabled for now!
            // This test fails since the last commit but I actually think it only ever worked by accident.
            // It did rely on a commons-logging-1.0.4.pom which got resolved by a test which did run
            //         previously.
            // But after updating to commons-logging.1.1.1 there is no pre-resolved artifact available in
            // target/local-repo anymore, thus the javadoc link info cannot get built and the test fails
            // I'll for now just disable this line of code, because the test as far as I can see _never_
            // did go upstream. The remoteRepository list used is always empty!.
            //
            //            assertTrue( optionsContent.contains( "-link
            //         'http://commons.apache.org/logging/apidocs'"
            //             ) );
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
    @InjectMojo(goal = "javadoc", pom = "wrong-encoding-test-plugin-config.xml")
    @Basedir("/unit/validate-options-test")
    @Test
    void testValidateOptionsWrongEncoding(JavadocReport mojo) throws Exception {
        // encoding
        try {
            mojo.execute();
            fail("No wrong encoding catch");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("Unsupported option <encoding/>"), "No wrong encoding catch");
        }
    }
    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "wrong-docencoding-test-plugin-config.xml")
    @Basedir("/unit/validate-options-test")
    @Test
    void testValidateOptionsWrongDocencoding(JavadocReport mojo) throws Exception {
        try {
            mojo.execute();
            fail("No wrong docencoding catch");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("Unsupported option <docencoding/>"), "No wrong docencoding catch");
        }
    }

    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "wrong-charset-test-plugin-config.xml")
    @Basedir("/unit/validate-options-test")
    @Test
    void testValidateOptionsWrongCharset(JavadocReport mojo) throws Exception {
        try {
            mojo.execute();
            fail("No wrong charset catch");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("Unsupported option <charset/>"), "No wrong charset catch");
        }
    }

    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "wrong-locale-with-variant-test-plugin-config.xml")
    @Basedir("/unit/validate-options-test")
    @Test
    void testValidateOptionsWrongLocale(JavadocReport mojo) throws Exception {
        mojo.execute();
        assertTrue(true, "No wrong locale catch");
    }

    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "conflict-options-test-plugin-config.xml")
    @Basedir("/unit/validate-options-test")
    @Test
    void testValidateOptionsConflicts(JavadocReport mojo) throws Exception {
        try {
            mojo.execute();
            fail("No conflict catch");
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("Option <nohelp/> conflicts with <helpfile/>"), "No conflict catch");
        }
    }

    /**
     * Method to test the <code>&lt;tagletArtifacts/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "tagletArtifacts-test-plugin-config.xml")
    @Basedir("/unit/tagletArtifacts-test")
    @Test
    @EnabledForJreRange(max = JRE.JAVA_9)
    void testTagletArtifacts(JavadocReport mojo) throws Exception {

        mojo.execute();

        Path optionsFile = new File(mojo.getPluginReportOutputDirectory(), "options").toPath();
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
    @InjectMojo(goal = "javadoc", pom = "pom.xml")
    @Basedir("/unit/stylesheetfile-test")
    @Test
    void testStylesheetfile(JavadocReport mojo) throws Exception {

        // bug in testing framework - wrong resource dir name
        mojo.getProject()
                .getBuild()
                .getResources()
                .get(0)
                .setDirectory(getTestFile("src/main/resources").getAbsolutePath());

        // add dependency to find the stylesheetfile in a plugin dependency
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-javadoc-plugin");
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache.maven.plugins.maven-javadoc-plugin.unit");
        dependency.setArtifactId("stylesheetfile-test");
        dependency.setVersion("1.0-SNAPSHOT");
        plugin.addDependency(dependency);

        mojo.getProject().getBuild().setPlugins(Collections.singletonList(plugin));

        File testArtifactDir = getTestFile("/artifact-stylesheetfile");
        assertThat(testArtifactDir).exists();
        copyDirectory(testArtifactDir, localRepo);

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

        Path stylesheetfile = apidocs.resolve("stylesheet.css");
        if (JavaVersion.JAVA_VERSION.isAtLeast("23")) {
            stylesheetfile = apidocs.resolve("resource-files/stylesheet.css");
        }
        Path options = apidocs.resolve("options");

        // stylesheet == maven OR java
        setVariableValueToObject(mojo, "stylesheet", "javamaven");

        try {
            mojo.execute();
            fail();
        } catch (MojoExecutionException | MojoFailureException e) {
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
        setVariableValueToObject(mojo, "stylesheetfile", "/com/mycompany/app/javadoc/css/stylesheet.css");
        mojo.execute();

        content = readFile(stylesheetfile);
        assertTrue(content.contains("/* Custom Javadoc style sheet in project */"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-stylesheetfile"));
        File stylesheetResource = getTestFile("/src/main/resources/com/mycompany/app/javadoc/css/stylesheet.css");
        assertTrue(optionsContent.contains(
                "'" + stylesheetResource.getAbsolutePath().replaceAll("\\\\", "/") + "'"));

        // stylesheetfile defined in a javadoc plugin dependency
        setVariableValueToObject(mojo, "stylesheetfile", "/com/mycompany/app/javadoc/css2/stylesheet.css");
        mojo.execute();

        content = readFile(stylesheetfile);
        assertTrue(content.contains("/* Custom Javadoc style sheet in artefact */"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-stylesheetfile"));

        assertThat(optionsContent)
                .contains("'" + stylesheetfile.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'");

        // stylesheetfile defined as file
        File css = getTestFile("/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css");
        setVariableValueToObject(mojo, "stylesheetfile", css.getAbsolutePath());
        mojo.execute();

        content = readFile(stylesheetfile);
        assertTrue(content.contains("/* Custom Javadoc style sheet as file */"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-stylesheetfile"));
        stylesheetResource = getTestFile("/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css");
        assertTrue(optionsContent.contains(
                "'" + stylesheetResource.getAbsolutePath().replaceAll("\\\\", "/") + "'"));
    }

    /**
     * Method to test the <code>&lt;helpfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "javadoc", pom = "pom.xml")
    @Basedir("/unit/helpfile-test")
    @Test
    void testHelpfile(JavadocReport mojo) throws Exception {

        // bug in testing framework - wrong resource dir name
        mojo.getProject()
                .getBuild()
                .getResources()
                .get(0)
                .setDirectory(getTestFile("src/main/resources").getAbsolutePath());

        // add dependency to find the helpfile in a plugin dependency
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-javadoc-plugin");
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache.maven.plugins.maven-javadoc-plugin.unit");
        dependency.setArtifactId("helpfile-test");
        dependency.setVersion("1.0-SNAPSHOT");
        plugin.addDependency(dependency);

        mojo.getProject().getBuild().setPlugins(Collections.singletonList(plugin));

        File testArtifactDir = getTestFile("/artifact-helpfile");
        assertThat(testArtifactDir).exists();
        copyDirectory(testArtifactDir, localRepo);

        Path apidocs = new File(getBasedir(), "/target/site/apidocs").toPath();

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
        help = getTestFile("/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html")
                .toPath();
        assertTrue(optionsContent.contains("'" + help.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));

        // helpfile defined as file
        help = getTestFile("/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html")
                .toPath();
        setVariableValueToObject(mojo, "helpfile", help.toFile().getAbsolutePath());
        mojo.execute();

        content = readFile(helpfile);
        assertTrue(content.contains("<!--  Help file from file -->"));

        optionsContent = readFile(options);
        assertTrue(optionsContent.contains("-helpfile"));
        assertTrue(optionsContent.contains("'" + help.toFile().getAbsolutePath().replaceAll("\\\\", "/") + "'"));
    }
}
