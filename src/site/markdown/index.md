<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Maven Javadoc Plugin
The Javadoc Plugin uses the Javadoc tool to generate javadocs for the specified project. For more information about the standard Javadoc tool, please refer to [Reference Guide](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html).

The Javadoc Plugin gets the parameter values that will be used from the plugin configuration specified in the pom. To hold all javadoc arguments, packages or files, the Javadoc Plugin generates [argument files](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/javadoc.html#argumentfiles) and calls the Javadoc tool as follow:

```unknown
javadoc.exe(or .sh) @options @packages | @argfile
```

When no configuration values are set, the plugin sets default values instead and then executes the Javadoc tool.

You can also use the plugin to package the generated javadocs into a jar file for distribution.

## Goals Overview

The Javadoc Plugin has 16 goals:

- [javadoc:javadoc](./javadoc-mojo.html) generates the Javadoc files for the project. It executes the standard Javadoc tool and supports the parameters used by the tool.
- [javadoc:test-javadoc](./test-javadoc-mojo.html) generates the test Javadoc files for the project. It executes the standard Javadoc tool and supports the parameters used by the tool.
- [javadoc:javadoc-no-fork](./javadoc-no-fork-mojo.html) generates the Javadoc files for the project. It executes the standard Javadoc tool and supports the parameters used by the tool without forking the `generate-sources` phase again. Note that this goal does require generation of sources before site generation, e.g. by invoking `mvn clean deploy site`.
- [javadoc:test-javadoc-no-fork](./test-javadoc-no-fork-mojo.html) generates the test Javadoc files for the project. It executes the standard Javadoc tool and supports the parameters used by the tool without forking the `generate-test-sources` phase again. Note that this goal does require generation of test sources before site generation, e.g. by invoking `mvn clean deploy site`.
- [javadoc:aggregate](./aggregate-mojo.html) generates the Javadoc files for an aggregator project. It executes the standard Javadoc tool and supports the parameters used by the tool.
- [javadoc:test-aggregate](./test-aggregate-mojo.html) generates the test Javadoc files for an aggregator project. It executes the standard Javadoc tool and supports the parameters used by the tool.
- [javadoc:aggregate-no-fork](./aggregate-no-fork-mojo.html) generates the Javadoc files for an aggregator project. It executes the standard Javadoc tool and supports the parameters used by the tool without forking the **compile**&gt; phase again. Note that this goal does require generation of class files before site generation, e.g. by invoking `mvn compile` or `mvn install`.
- [javadoc:test-aggregate-no-fork](./test-aggregate-no-fork-mojo.html) generates the test Javadoc files for an aggregator project. It executes the standard Javadoc tool and supports the parameters used by the tool without forking the `compile` phase again. Note that this goal does require generation of test class files before site generation, e.g. by invoking `mvn test-compile` or `mvn install`.
- [javadoc:jar](./jar-mojo.html) creates an archive file of the generated Javadocs. It is used during the release process to create the Javadoc artifact for the project's release. This artifact is uploaded to the remote repository along with the project's compiled binary and source archive.
- [javadoc:test-jar](./test-jar-mojo.html) creates an archive file of the generated Test Javadocs.
- [javadoc:aggregate-jar](./aggregate-jar-mojo.html) creates an archive file of the generated Javadocs for an aggregator project.
- [javadoc:test-aggregate-jar](./test-aggregate-jar-mojo.html) creates an archive file of the generated Test Javadocs for an aggregator project.
- [javadoc:resource-bundle](./resource-bundle-mojo.html) bundles the `javadocDirectory` along with Javadoc configuration options such as taglet, doclet, and link information into a deployable artifact.
- [javadoc:test-resource-bundle](./test-resource-bundle-mojo.html) bundles the `testJavadocDirectory` along with Javadoc configuration options such as taglet, doclet, and link information into a deployable artifact.
## Usage

General instructions on how to use the Javadoc Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](http://maven.apache.org/guides/development/guide-helping.html).

## Examples

The following examples show how to use the Javadoc Plugin in more advanced use cases:

- [Aggregating Javadocs for Multi-Projects](./examples/aggregate.html)
- [Aggregating Dependency Javadocs](./examples/aggregate-dependency-sources.html)
- [Excluding Packages](./examples/exclude-package-names.html)
- [Grouping Packages](./examples/group-configuration.html)
- [Using Alternate Doclet](./examples/alternate-doclet.html)
- [Using Alternate Javadoc Tool](./examples/alternate-javadoc-tool.html)
- [Using Javadoc Resources](./examples/javadoc-resources.html)
- [Configuring Stylesheets](./examples/stylesheet-configuration.html)
- [Configuring Helpfile](./examples/help-configuration.html)
- [Configuring Custom Javadoc Tags](./examples/tag-configuration.html)
- [Configuring Custom Javadoc Taglet](./examples/taglet-configuration.html)
- [Configuring Links](./examples/links-configuration.html)
- [Generating test Javadocs](./examples/test-javadocs.html)
- [Selective Javadocs Reports](./examples/selective-javadocs-report.html)
- [Generate Javadoc without duplicate execution of phase generate-sources](./examples/javadoc-nofork.html)
- [Generate aggregate Javadocs without execution of phase compile](./examples/aggregate-nofork.html)
