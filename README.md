<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# Apache Maven Javadoc Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-javadoc-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-javadoc-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/plugins/maven-javadoc-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-javadoc-plugin/README.md)
[![Verify](https://github.com/apache/maven-javadoc-plugin/actions/workflows/maven.yml/badge.svg)](https://github.com/apache/maven-javadoc-plugin/actions/workflows/maven.yml)

Generates API documentation for a Maven project with the Javadoc tool of the
JDK, packages it as the `-javadoc.jar` that Maven Central expects next to a
release, and adds it to the project site as a report.

```shell
mvn javadoc:javadoc        # one module, into target/reports/apidocs
mvn javadoc:aggregate      # all modules of a multi-module build
mvn javadoc:jar            # the -javadoc.jar
```

The plugin needs Maven 3.9.0 or later and Java 8 or later.

- Documentation: <https://maven.apache.org/plugins/maven-javadoc-plugin/>
- Issues: <https://github.com/apache/maven-javadoc-plugin/issues>
- Security reports: see [SECURITY.md](SECURITY.md); never a public issue

## Build

Build the plugin and install it into your local repository:

```shell
mvn install
```

The integration tests under `src/it/projects` run with the `run-its`
profile. To run all of them, or one by its directory name:

```shell
mvn -Prun-its verify
mvn -Prun-its verify -Dinvoker.test=MJAVADOC-137_jar
```

Before you push, check formatting and license headers:

```shell
mvn spotless:check apache-rat:check
```

`mvn spotless:apply` fixes the formatting. Run it with JDK 17 or 21.

## Contributing

Open a pull request against `master` on GitHub. Discuss larger changes on the
[developer mailing list](https://maven.apache.org/mailing-lists.html) or in an
issue first. Add a unit test or an integration test for each fix, keep
formatting changes out of functional pull requests, and see the
[guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html)
for more. Contributors who send more than the occasional patch are asked to
file an [Apache Individual Contributor License Agreement](https://www.apache.org/licenses/#clas).
