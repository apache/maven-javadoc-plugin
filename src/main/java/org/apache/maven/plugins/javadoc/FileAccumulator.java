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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

class FileAccumulator implements FileVisitor<Path> {

    private Path base;
    private List<PathMatcher> sourceFileIncludes = new ArrayList<>();
    private List<PathMatcher> sourceFileExcludes = new ArrayList<>();
    private List<String> includedFiles = new ArrayList<>();

    FileAccumulator(Path base, List<String> sourceFileIncludes, List<String> sourceFileExcludes) {
        this.base = base;
        FileSystem fileSystem = FileSystems.getDefault();
        for (String glob : sourceFileIncludes) {
            this.sourceFileIncludes.add(fileSystem.getPathMatcher("glob:" + glob));
        }

        if (sourceFileExcludes != null) {
            for (String glob : sourceFileExcludes) {
                this.sourceFileExcludes.add(fileSystem.getPathMatcher("glob:" + glob));
            }
        }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException ex) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes arg1) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes ex) throws IOException {
        for (PathMatcher matcher : sourceFileExcludes) {
            if (matcher.matches(path)) {
                return FileVisitResult.CONTINUE;
            }
        }
        for (PathMatcher matcher : sourceFileIncludes) {
            if (matcher.matches(path)) {
                includedFiles.add(base.relativize(path).toString());
                return FileVisitResult.CONTINUE;
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException ex) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    String[] getIncludedFiles() {
        return includedFiles.toArray(new String[0]);
    }
}
