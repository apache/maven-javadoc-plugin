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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest(realRepositorySession = true)
public class AggregatorJavadocReportTest {

    @Inject
    private MavenSession mavenSession;

    private static final char LINE_SEPARATOR = ' ';

    private File localRepo;

    /** {@inheritDoc} */
    @BeforeEach
    public void setUp() {
        localRepo = new File(getBasedir(), "target/local-repo/");
        mavenSession.getRequest().setLocalRepositoryPath(localRepo);
    }

    /**
     * Convenience method that reads the contents of the specified file object into a string with a <code>space</code>
     * as line separator.
     *
     * @see #LINE_SEPARATOR
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile(File file) throws IOException {
        StringBuilder str = new StringBuilder((int) file.length());

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {

            for (String strTmp; (strTmp = in.readLine()) != null; ) {
                str.append(LINE_SEPARATOR);
                str.append(strTmp);
            }
        }

        return str.toString();
    }

    /**
     * Method to test the aggregate parameter
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "aggregate", pom = "aggregate-test/aggregate-test-plugin-config.xml")
    @Basedir("/unit")
    @Test
    void testAggregate(JavadocReport mojo) throws Exception {
        mojo.execute();

        File apidocs = new File(getBasedir(), "aggregate-test/target/site/apidocs/");

        // check if project1 api files exist
        assertTrue(new File(apidocs, "aggregate/test/project1/Project1App.html").exists());
        assertTrue(new File(apidocs, "aggregate/test/project1/Project1AppSample.html").exists());
        assertTrue(new File(apidocs, "aggregate/test/project1/Project1Sample.html").exists());
        assertTrue(new File(apidocs, "aggregate/test/project1/Project1Test.html").exists());

        // check if project2 api files exist
        assertTrue(new File(apidocs, "aggregate/test/project2/Project2App.html").exists());
        assertTrue(new File(apidocs, "aggregate/test/project2/Project2AppSample.html").exists());
        assertTrue(new File(apidocs, "aggregate/test/project2/Project2Sample.html").exists());
        assertTrue(new File(apidocs, "aggregate/test/project2/Project2Test.html").exists());
    }

    /**
     * Test the javadoc resources in the aggregation case.
     *
     * @throws Exception if any
     */
    @InjectMojo(goal = "aggregate", pom = "aggregate-resources-test/aggregate-resources-test-plugin-config.xml")
    @Basedir("/unit")
    @Test
    void testAggregateJavadocResources(JavadocReport mojo) throws Exception {
        mojo.execute();

        File apidocs = new File(getBasedir(), "aggregate-resources-test/target/site/apidocs");

        // Test overview
        File overviewSummary = getOverviewSummary(apidocs);

        assertTrue(overviewSummary.exists());
        String overview = readFile(overviewSummary).toLowerCase(Locale.ENGLISH);
        assertTrue(overview.contains("<a href=\"resources/test/package-summary.html\">resources.test</a>"));
        assertTrue(overview.contains(">blabla</"));
        assertTrue(overview.contains("<a href=\"resources/test2/package-summary.html\">resources.test2</a>"));
        assertTrue(overview.contains("<a href=\"resources2/test/package-summary.html\">resources2.test</a>"));
        assertTrue(overview.contains("<a href=\"resources2/test2/package-summary.html\">resources2.test2</a>"));

        // Test doc-files
        File app = new File(apidocs, "resources/test/App.html");
        assertTrue(app.exists());
        overview = readFile(app);
        assertTrue(overview.contains("<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">"));
        assertTrue(new File(apidocs, "resources/test/doc-files/maven-feather.png").exists());
    }

    @InjectMojo(goal = "aggregate", pom = "aggregate-modules-not-in-subdirectories-test/all/pom.xml")
    @Basedir("/unit")
    @Test
    void testAggregateWithModulsNotInSubDirectories(JavadocReport mojo) throws Exception {
        mojo.execute();

        File apidocs = new File(getBasedir(), "aggregate-modules-not-in-subdirectories-test/target/site/apidocs");
        assertTrue(apidocs.isDirectory());
        assertTrue(getOverviewSummary(apidocs).isFile());
    }

    private static File getOverviewSummary(File apidocs) {
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("11")) {
            return new File(apidocs, "overview-summary.html");
        }
        return new File(apidocs, "index.html");
    }
}
