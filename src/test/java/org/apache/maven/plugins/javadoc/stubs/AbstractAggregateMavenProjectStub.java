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
 * @author <a href="mailto:reto.weiss@axonivy.com">Reto Weiss</a>
 */
public class AbstractAggregateMavenProjectStub extends MavenProjectStub {
    private final String baseDir;
    private final String[] projects;

    public AbstractAggregateMavenProjectStub(
            String baseDir, String pomFileName, String targetDirectory, String... projects) {
        this.baseDir = baseDir;
        this.projects = projects;
        readModel(new File(getBasedir(), pomFileName));

        setGroupId(getModel().getGroupId());
        setArtifactId(getModel().getArtifactId());
        setVersion(getModel().getVersion());
        setName(getModel().getName());
        setUrl(getModel().getUrl());
        setPackaging(getModel().getPackaging());

        setExecutionRoot(true);

        Build build = new Build();
        build.setFinalName(getModel().getArtifactId());
        build.setSourceDirectory(getBasedir() + "/src/main/java");
        build.setDirectory(super.getBasedir() + targetDirectory);
        setBuild(build);

        List<String> compileSourceRoots = new ArrayList<>();
        setCompileSourceRoots(compileSourceRoots);
    }

    @Override
    public File getBasedir() {
        return new File(super.getBasedir() + baseDir);
    }

    @Override
    public MavenProject getExecutionProject() {
        return this;
    }

    @Override
    public List<String> getModules() {
        return Arrays.asList(projects);
    }

    @Override
    public Set<Artifact> getDependencyArtifacts() {
        return Collections.emptySet();
    }
}
