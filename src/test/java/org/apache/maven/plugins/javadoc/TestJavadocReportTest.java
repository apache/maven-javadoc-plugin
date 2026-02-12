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
import java.util.Collections;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
@MojoTest
public class TestJavadocReportTest {
    /**
     * Test the test-javadoc configuration for the plugin
     *
     * @throws Exception if any
     */
    @Test
    @InjectMojo(goal = "test-javadoc", pom = "test-javadoc-test-plugin-config.xml")
    @Basedir("/unit/test-javadoc-test")
    public void testTestJavadoc(TestJavadocReport mojo) throws Exception {

        Artifact testArtifact = new DefaultArtifact(
                "groupId", "artifactId", "1.2.3", "test", "jar", null, new DefaultArtifactHandler("jar"));
        testArtifact.setFile(new File("test/test-dependency-1.2.3.jar"));

        mojo.getProject().setArtifacts(Collections.singleton(testArtifact));

        mojo.execute();

        File generatedFile = new File(getBasedir(), "/target/site/testapidocs/maven/AppTest.html");
        assertThat(generatedFile).exists();

        // -classpath
        // '/Users/slawomir.jaranowski/repos/apache/maven-javadoc-plugin/target/test/unit/test-javadoc-test/target/classes:/Users/slawomir.jaranowski/repos/apache/maven-javadoc-plugin/target/test/unit/test-javadoc-test/target/test-classes:/Users/slawomir.jaranowski/repos/apache/maven-javadoc-plugin/src/test/resources/unit/test-javadoc-test/junit/junit/3.8.1/junit-3.8.1.jar'

        File options = new File(getBasedir(), "/target/site/testapidocs/options");
        assertThat(options).content().contains("test-dependency-1.2.3.jar");
    }
}
