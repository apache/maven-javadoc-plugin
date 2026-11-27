---
title: Frequently Asked Questions
---

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

<a id="top"></a>

# Frequently Asked Questions

1. [What are the Javadoc options supported by the Maven Javadoc Plugin?](#What_are_the_Javadoc_options_supported_by_the_Maven_Javadoc_Plugin)
2. [Where in the pom.xml do I configure the Javadoc Plugin?](#Where_in_the_pom.xml_do_I_configure_the_Javadoc_Plugin)
3. [Where do I put Javadoc resources like HTML files or images?](#Where_do_I_put_javadoc_resources_like_HTML_files_or_images)
4. [How to know exactly the Javadoc command line?](#How_to_know_exactly_the_Javadoc_command_line)
5. [How to add additional Javadoc parameters?](#How_to_add_additional_Javadoc_parameters)
6. [How to add additional Javadoc options?](#How_to_add_additional_Javadoc_options)
7. [How to increase Javadoc heap size?](#How_to_increase_Javadoc_heap_size)
8. [How to add proxy support?](#How_to_add_proxy_support)
9. [How to have less output?](#How_to_have_less_output)
10. [How to remove test Javadocs report?](#How_to_remove_test_Javadocs_report)
11. [How to deploy Javadoc jar file?](#How_to_deploy_Javadoc_jar_file)
12. [How to include additional source code directories in aggregate mode?](#How_to_include_additional_source_code_directories_in_aggregate_mode)
13. [How to use `<links/>` option in Standard Doclet?](#How_to_use_links_option_in_Standard_Doclet)
14. [How to add cross reference link to internal-external projects?](#How_to_add_cross_reference_link_to_internal-external_projects)
15. [What are the values of the `<encoding/>`, `<docencoding/>`, and `<charset/>` parameters?](#What_are_the_values_of_the_encoding.2C_docencoding.2C_and_charset_parameters)
16. [Why do I get errors when using links under Java 8?](#Why_do_I_get_errors_when_using_links_under_Java_8)

<a id="What_are_the_Javadoc_options_supported_by_the_Maven_Javadoc_Plugin"></a>

### What are the Javadoc options supported by the Maven Javadoc Plugin?

Most options of the [Javadoc tool](https://docs.oracle.com/en/java/javase/25/docs/specs/man/javadoc.html) and
its Standard Doclet have a plugin parameter of the same name; the
[Javadoc Plugin Documentation](./javadoc-mojo.html) lists them. Pass any other option with the
[*&lt;additionalOptions/&gt;*](./javadoc-mojo.html#additionalOptions) parameter.

<a id="Where_in_the_pom.xml_do_I_configure_the_Javadoc_Plugin"></a>

### Where in the pom.xml do I configure the Javadoc Plugin?

Like all other reporting plugins, the Javadoc Plugin goes in the *&lt;reporting/&gt;* section of your pom.xml.
In this case, you will need to call `mvn site` to run reports.

You could also configure it in the *&lt;plugins/&gt;* or *&lt;pluginsManagement/&gt;* in *&lt;build/&gt;* tag
of your pom.xml. In this case, you will need to call `mvn javadoc:javadoc` to run the main report.

**IMPORTANT NOTE**: using *&lt;reporting/&gt;* or *&lt;build/&gt;* elements have not the same behavior, refer
to [Using the &lt;reporting/&gt; Tag VS &lt;build/&gt; Tag](https://maven.apache.org/guides/mini/guide-configuring-plugins.html#Using_the_reporting_Tag_VS_build_Tag)
part for more information.

<a id="Where_do_I_put_javadoc_resources_like_HTML_files_or_images"></a>

### Where do I put Javadoc resources like HTML files or images?

All javadoc resources like HTML files, images could be put in the *${basedir}/src/main/javadoc* directory.

See [Using Javadoc Resources](examples/javadoc-resources.html) for more information.

<a id="How_to_know_exactly_the_Javadoc_command_line"></a>

### How to know exactly the Javadoc command line?

The Javadoc Plugin calls the Javadoc tool with
[argument files](https://docs.oracle.com/en/java/javase/25/docs/specs/man/javac.html#command-line-argument-files),
i.e. files called 'options', 'packages' and 'argfile':

```
javadoc @options @packages | @argfile
```

These argument files are generated at runtime depending the Javadoc Plugin configuration and are deleted when
the Javadoc Plugin ended.

To preserve them, add &lt;debug&gt;true&lt;/debug&gt; to your Javadoc Plugin configuration or call
`mvn javadoc:javadoc -Ddebug=true`. The plugin then also writes a `javadoc.sh` (`javadoc.bat` on Windows) script
with the complete command line to the output directory, `target/reports/apidocs` for `javadoc:javadoc`.
`mvn javadoc:javadoc -X` logs the command line but deletes the files.

<a id="How_to_add_additional_Javadoc_parameters"></a>

### How to add additional Javadoc parameters?

You could need to add more Javadoc parameters to be process by the Javadoc Tool (i.e. for doclet).

For this, you should use the *&lt;additionalOptions/&gt;* parameter in your Javadoc Plugin configuration.

<a id="How_to_add_additional_Javadoc_options"></a>

### How to add additional Javadoc options?

You could need to add more J options (i.e. runtime system java options that runs Javadoc tool like -J-Xss) to
be processed by the Javadoc Tool. For this, you should use the *&lt;additionalJOption/&gt;* parameter in your
Javadoc Plugin configuration.

The Javadoc Plugin calls the Javadoc tool with J options, e.g.:

```
javadoc \
    -J-Xss128m \
    @options \
    @packages | @argfile
```

<a id="How_to_increase_Javadoc_heap_size"></a>

### How to increase Javadoc heap size?

If you need to increase the Javadoc heap size, you should use the *&lt;minmemory/&gt;* and
*&lt;maxmemory/&gt;* parameters in your Javadoc Plugin configuration. For instance:

```xml
<project>
  ...
  <reporting>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-javadoc-plugin</artifactId>
        <configuration>
          ...
          <minmemory>128m</minmemory>
          <maxmemory>1g</maxmemory>
          ...
        </configuration>
      </plugin>
    </plugins>
    ...
  </reporting>
  ...
</project>
```

**Note:** The memory unit depends on the JVM used. The units supported could be: `k`, `kb`, `m`, `mb`, `g`,
`gb`, `t`, `tb`. If no unit specified, the default unit is `m`.

<a id="How_to_add_proxy_support"></a>

### How to add proxy support?

To specify a proxy in the Javadoc tool, you need to configure an active proxy in your
*${user.home}/.m2/settings.xml*, similar to:

```xml
<settings>
  ...
  <proxies>
   <proxy>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy.somewhere.com</host>
      <port>3128</port>
      <username>foo</username>
      <password>bar</password>
      <nonProxyHosts>localhost|*.somewhere.com</nonProxyHosts>
    </proxy>
  </proxies>
  ...
</settings>
```

With this, the Javadoc tool will be called with networking J options, e.g.:

```
javadoc \
    -J-Dhttp.proxyHost=proxy.somewhere.com \
    -J-Dhttp.proxyPort=3128 \
    -J-Dhttps.proxyHost=proxy.somewhere.com \
    -J-Dhttps.proxyPort=3128 \
    "-J-Dhttp.nonProxyHosts=\"localhost^|*.somewhere.com\"" \
    @options \
    @packages | @argfile
```

The proxy user name and password are not passed to the Javadoc tool.

**Note**: If your proxy needs more JVM
[networking properties](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/doc-files/net-properties.html) (like
NTLM), you could always add JVM options using the *&lt;additionalJOption/&gt;* parameter in your Javadoc
Plugin configuration, e.g.:

```xml
<configuration>
  <additionalJOption>-J-Dhttp.auth.ntlm.domain=MYDOMAIN</additionalJOption>
  ...
</configuration>
```

<a id="How_to_have_less_output"></a>

### How to have less output?

Just set the *&lt;quiet/&gt;* parameter to *true* in your Javadoc Plugin configuration.

<a id="How_to_remove_test_Javadocs_report"></a>

### How to remove test Javadocs report?

You need to configure the *&lt;reportSets/&gt;* parameter. Read the
[Selective Javadocs Reports](examples/selective-javadocs-report.html) part for more information.

<a id="How_to_deploy_Javadoc_jar_file"></a>

### How to deploy Javadoc jar file?

Basically, you need to call *mvn clean javadoc:jar deploy*. If you want to include the javadoc jar in a
release process, you need to attach it in the release profile, for instance:

```xml
<project>
  ...
  <profiles>
    <profile>
      <id>release</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <executions>
              <execution>
                <id>attach-javadocs</id>
                <goals>
                  <goal>jar</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
    ...
  </profiles>
  ...
</project>
```

To deploy the Javadoc jar on a given Maven repository, you could call:

```
mvn deploy:deploy-file \
    -DgroupId=<group-id> \
    -DartifactId=<artifact-id> \
    -Dversion=<version> \
    -Dfile=<path-to-file> \
    -Dpackaging=jar \
    -DrepositoryId=<repository-id> \
    -Durl=https://repository.example.com/releases \
    -Dclassifier=javadoc
```

<a id="How_to_include_additional_source_code_directories_in_aggregate_mode"></a>

### How to include additional source code directories in aggregate mode?

Use the `javadoc:aggregate` goal. It forks the `compile` phase of every module, so it includes source
directories added with the
[build-helper:add-source](https://www.mojohaus.org/build-helper-maven-plugin/add-source-mojo.html) goal. The
`javadoc:javadoc` goal has no `aggregate` parameter any more. Read the
[Aggregating Javadocs for Multi-Projects](examples/aggregate.html) part for more information.

<a id="How_to_use_links_option_in_Standard_Doclet"></a>

### How to use `<links/>` option in Standard Doclet?

You need to configure the [*&lt;links/&gt;*](./javadoc-mojo.html#links) parameter. Also, you should correctly
write references in your Javadoc. That is,

- `@see MyMojo` or `{@link MyMojo}` will **NOT work**.
- `@see com.mycompany.plugin.myplugin.MyMojo` or `{@link com.mycompany.myplugin.MyMojo}` will **work**.

<a id="How_to_add_cross_reference_link_to_internal-external_projects"></a>

### How to add cross reference link to internal-external projects?

Please refer to [Links configuration page](./examples/links-configuration.html).

<a id="What_are_the_values_of_the_encoding.2C_docencoding.2C_and_charset_parameters"></a>

### What are the values of the `<encoding/>`, `<docencoding/>`, and `<charset/>` parameters?

By default, these parameters have the following values:

`<encoding/>`
: Value of `${project.build.sourceEncoding}` property or the value of the `file.encoding` system property if
  not specified.

`<docencoding/>`
: Value of `${project.reporting.outputEncoding}` property or `UTF-8` if not specified.

`<charset/>`
: Value of `docencoding` parameter if not specified.

<a id="Why_do_I_get_errors_when_using_links_under_Java_8"></a>

### Why do I get errors when using links under Java 8?

Due to [a bug in JDK 8](https://bugs.openjdk.org/browse/JDK-8040771) you need at least Java 8u20 for this
to work. See [MJAVADOC-393](https://issues.apache.org/jira/browse/MJAVADOC-393) for more info.
