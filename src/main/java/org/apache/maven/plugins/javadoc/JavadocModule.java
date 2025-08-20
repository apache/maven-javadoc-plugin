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
 * @author elharo
 */
public class JavadocModule {
    private final String gav;

    private final File artifactFile;

    private final Collection<Path> sourcePaths;

    private final JavaModuleDescriptor moduleDescriptor;

    private final ModuleNameSource moduleNameSource;

    /**
     * <p>Constructor for JavadocModule.</p>
     *
     * @param gav a {@link java.lang.String} object
     * @param artifactFile a {@link java.io.File} object
     * @param sourcePaths a {@link java.util.Collection} object
     */
    public JavadocModule(String gav, File artifactFile, Collection<Path> sourcePaths) {
        this(gav, artifactFile, sourcePaths, null, null);
    }

    /**
     * <p>Constructor for JavadocModule.</p>
     *
     * @param gav a {@link java.lang.String} object
     * @param artifactFile a {@link java.io.File} object
     * @param sourcePaths a {@link java.util.Collection} object
     * @param moduleDescriptor a {@link org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor} object
     * @param moduleNameSource a {@link org.codehaus.plexus.languages.java.jpms.ModuleNameSource} object
     */
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

    /**
     * <p>Getter for the field <code>gav</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getGav() {
        return gav;
    }

    /**
     * <p>Getter for the field <code>sourcePaths</code>.</p>
     *
     * @return a {@link java.util.Collection} object
     */
    public Collection<Path> getSourcePaths() {
        return sourcePaths;
    }

    /**
     * <p>Getter for the field <code>artifactFile</code>.</p>
     *
     * @return a {@link java.io.File} object
     */
    public File getArtifactFile() {
        return artifactFile;
    }

    /**
     * <p>Getter for the field <code>moduleDescriptor</code>.</p>
     *
     * @return a {@link org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor} object
     */
    public JavaModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }

    /**
     * <p>Getter for the field <code>moduleNameSource</code>.</p>
     *
     * @return a {@link org.codehaus.plexus.languages.java.jpms.ModuleNameSource} object
     */
    public ModuleNameSource getModuleNameSource() {
        return moduleNameSource;
    }
}
