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
import java.nio.file.Path;
import java.util.Collection;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;

/**
 * Represents a unit of Javadoc referring to the binary and java source paths
 *
 * @since 3.3.0
 */
public class JavadocModule {
    private final String gav;

    private final File artifactFile;

    private final Collection<Path> sourcePaths;

    private final JavaModuleDescriptor moduleDescriptor;

    private final ModuleNameSource moduleNameSource;

    public JavadocModule(String gav, File artifactFile, Collection<Path> sourcePaths) {
        this(gav, artifactFile, sourcePaths, null, null);
    }

    public JavadocModule(
            String gav,
            File artifactFile,
            Collection<Path> sourcePaths,
            JavaModuleDescriptor moduleDescriptor,
            ModuleNameSource moduleNameSource) {
        this.gav = gav;
        this.artifactFile = artifactFile;
        this.sourcePaths = sourcePaths;
        this.moduleDescriptor = moduleDescriptor;
        this.moduleNameSource = moduleNameSource;
    }

    public String getGav() {
        return gav;
    }

    public Collection<Path> getSourcePaths() {
        return sourcePaths;
    }

    public File getArtifactFile() {
        return artifactFile;
    }

    public JavaModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }

    public ModuleNameSource getModuleNameSource() {
        return moduleNameSource;
    }
}
