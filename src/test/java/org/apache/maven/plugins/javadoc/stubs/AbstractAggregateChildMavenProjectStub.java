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
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:reto.weiss@axonivy.com">Reto Weiss</a>
 */
public class AbstractAggregateChildMavenProjectStub extends MavenProjectStub {
    private String baseDir;

    public AbstractAggregateChildMavenProjectStub(String baseDir, String pomFileName, String targetDirectory) {
        this.baseDir = baseDir;
        readModel(new File(getBasedir(), pomFileName));

        setGroupId(
                Objects.toString(getModel().getGroupId(), getModel().getParent().getGroupId()));
        setArtifactId(getModel().getArtifactId());
        setVersion(
                Objects.toString(getModel().getVersion(), getModel().getParent().getVersion()));
        setName(getModel().getName());
        setUrl(getModel().getUrl());
        setPackaging(getModel().getPackaging());

        setExecutionRoot(true);

        Artifact artifact = new JavadocPluginArtifactStub(getGroupId(), getArtifactId(), getVersion(), getPackaging());
        artifact.setArtifactHandler(new DefaultArtifactHandlerStub());
        setArtifact(artifact);

        Build build = new Build();
        build.setFinalName(getModel().getArtifactId());
        build.setSourceDirectory(getBasedir() + "/src/main/java");
        build.setDirectory(super.getBasedir() + targetDirectory);
        setBuild(build);

        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add(getBasedir().getAbsolutePath() + "/src/main/java");
        setCompileSourceRoots(compileSourceRoots);
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir() {
        return new File(super.getBasedir() + baseDir);
    }

    /** {@inheritDoc} */
    @Override
    public MavenProject getExecutionProject() {
        return this;
    }
}
