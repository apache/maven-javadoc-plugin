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
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class TagletArtifactsMavenProjectStub extends MavenProjectStub {
    /**
     * Default constructor.
     */
    public TagletArtifactsMavenProjectStub() {
        readModel(new File(getBasedir(), "tagletArtifacts-test-plugin-config.xml"));

        setGroupId(getModel().getGroupId());
        setArtifactId(getModel().getArtifactId());
        setVersion(getModel().getVersion());
        setName(getModel().getName());
        setUrl(getModel().getUrl());
        setPackaging(getModel().getPackaging());

        Build build = new Build();
        build.setFinalName(getModel().getArtifactId());
        build.setSourceDirectory(getBasedir() + "/src/main/java");
        build.setDirectory(super.getBasedir() + "/target/test/unit/tagletArtifacts-test/target");
        setBuild(build);

        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add(getBasedir() + "/src/main/java");
        setCompileSourceRoots(compileSourceRoots);
    }

    /*
     * Allow to retrieve some dependencies from Maven Central
     */
    @Override
    public List<RemoteRepository> getRemoteProjectRepositories() {
        RemoteRepository.Builder builder =
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2");
        return Collections.singletonList(builder.build());
    }

    /** {@inheritDoc} */
    @Override
    public File getBasedir() {
        return new File(super.getBasedir() + "/src/test/resources/unit/tagletArtifacts-test");
    }
}
