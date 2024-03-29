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
package org.apache.maven.plugins.javadoc.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class AggregateTestMavenProjectStub extends MavenProjectStub {
    private Build build;

    public AggregateTestMavenProjectStub() {
        readModel(new File(getBasedir(), "aggregate-test-plugin-config.xml"));

        setGroupId(getModel().getGroupId());
        setArtifactId(getModel().getArtifactId());
        setVersion(getModel().getVersion());
        setName(getModel().getName());
        setUrl(getModel().getUrl());
        setPackaging(getModel().getPackaging());
        setExecutionRoot(true);

        build = new Build();
        build.setFinalName(getModel().getArtifactId());
        build.setDirectory(super.getBasedir() + "/target/test/unit/aggregate-test/target");

        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add(getBasedir() + "/src/main/java");
        setCompileSourceRoots(compileSourceRoots);
    }

    @Override
    public Build getBuild() {
        return build;
    }

    @Override
    public void setBuild(Build build) {
        this.build = build;
    }

    @Override
    public File getBasedir() {
        return new File(super.getBasedir() + "/src/test/resources/unit/aggregate-test");
    }

    @Override
    public MavenProject getExecutionProject() {
        return this;
    }

    @Override
    public List<String> getModules() {
        return Arrays.asList("project1", "project2");
    }

    @Override
    public Set<Artifact> getDependencyArtifacts() {
        return Collections.emptySet();
    }
}
