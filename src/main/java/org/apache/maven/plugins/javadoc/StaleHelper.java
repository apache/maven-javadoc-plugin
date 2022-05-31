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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Helper class to compute and write data used to detect a
 * stale javadoc.
 */
public class StaleHelper {

    /**
     * Compute the data used to detect a stale javadoc
     *
     * @param cmd the command line
     * @return the stale data
     * @throws MavenReportException if an error occurs
     */
    public static List<String> getStaleData(Commandline cmd) throws MavenReportException {
        try {
            List<String> ignored = new ArrayList<>();
            List<String> options = new ArrayList<>();
            Path dir = cmd.getWorkingDirectory().toPath().toAbsolutePath().normalize();
            String[] args = cmd.getArguments();
            Collections.addAll(options, args);

            final Charset cs;
            if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("9")
                    && JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("12")) {
                cs = StandardCharsets.UTF_8;
            } else {
                cs = Charset.defaultCharset();
            }

            for (String arg : args) {
                if (arg.startsWith("@")) {
                    String name = arg.substring(1);
                    options.addAll(Files.readAllLines(dir.resolve(name), cs));
                    ignored.add(name);
                }
            }
            List<String> state = new ArrayList<>(options);
            boolean cp = false;
            boolean sp = false;
            for (String arg : options) {
                if (cp) {
                    String s = unquote(arg);
                    for (String ps : s.split(File.pathSeparator)) {
                        Path p = dir.resolve(ps);
                        state.add(p + " = " + lastmod(p));
                    }
                } else if (sp) {
                    String s = unquote(arg);
                    for (String ps : s.split(File.pathSeparator)) {
                        Path p = dir.resolve(ps);
                        for (Path c : walk(p)) {
                            if (Files.isRegularFile(c)) {
                                state.add(c + " = " + lastmod(c));
                            }
                        }
                        state.add(p + " = " + lastmod(p));
                    }
                }
                cp = "-classpath".equals(arg);
                sp = "-sourcepath".equals(arg);
            }
            for (Path p : walk(dir)) {
                if (Files.isRegularFile(p) && !ignored.contains(p.getFileName().toString())) {
                    state.add(p + " = " + lastmod(p));
                }
            }
            return state;
        } catch (Exception e) {
            throw new MavenReportException("Unable to compute stale date", e);
        }
    }

    /**
     * Write the data used to detect a stale javadoc
     *
     * @param cmd the command line
     * @param path the stale data path
     * @throws MavenReportException if an error occurs
     */
    public static void writeStaleData(Commandline cmd, Path path) throws MavenReportException {
        try {
            List<String> curdata = getStaleData(cmd);
            Files.createDirectories(path.getParent());
            Files.write(path, curdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MavenReportException("Error checking stale data", e);
        }
    }

    private static Collection<Path> walk(Path dir) {
        Collection<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path p : directoryStream) {
                paths.add(p);
            }
            return paths;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String unquote(String s) {
        if (s.startsWith("'") && s.endsWith("'")) {
            return s.substring(1, s.length() - 1).replaceAll("\\\\'", "'");
        } else {
            return s;
        }
    }

    private static long lastmod(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }
}
