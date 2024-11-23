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

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.siterenderer.DocumentRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.javadoc.resolver.ResourceResolver;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.RepositorySystem;

/**
 * Generates documentation for the <code>Java code</code> in a <b>NON aggregator</b> project using the standard
 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html">Javadoc Tool</a>.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.0
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html">Javadoc Tool</a>
 */
@Mojo(name = "javadoc", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public class JavadocReport extends AbstractJavadocMojo implements MavenMultiPageReport {
    // ----------------------------------------------------------------------
    // Report Mojo Parameters
    // ----------------------------------------------------------------------

    /** The current shared report output directory to use */
    private File reportOutputDirectory;

    /**
     * The name of the Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @since 2.1
     */
    @Parameter(property = "name")
    private String name;

    /**
     * The description of the Javadoc report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     *
     * @since 2.1
     */
    @Parameter(property = "description")
    private String description;

    @Inject
    public JavadocReport(
            SiteTool siteTool,
            ArchiverManager archiverManager,
            ResourceResolver resourceResolver,
            RepositorySystem repoSystem,
            ArtifactHandlerManager artifactHandlerManager,
            ProjectBuilder mavenProjectBuilder,
            ToolchainManager toolchainManager) {
        super(
                siteTool,
                archiverManager,
                resourceResolver,
                repoSystem,
                artifactHandlerManager,
                mavenProjectBuilder,
                toolchainManager);
    }

    // ----------------------------------------------------------------------
    // Report public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getName(Locale locale) {
        if (name == null || name.isEmpty()) {
            return getBundle(locale).getString("report.javadoc.name");
        }

        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription(Locale locale) {
        if (description == null || description.isEmpty()) {
            return getBundle(locale).getString("report.javadoc.description");
        }

        return description;
    }

    /** {@inheritDoc} */
    @Override
    public void generate(Sink sink, Locale locale) throws MavenReportException {
        generate(sink, null, locale);
    }

    /** {@inheritDoc} */
    @Override
    public void generate(Sink sink, SinkFactory sinkFactory, Locale locale) throws MavenReportException {
        try {
            executeReport(locale);
        } catch (MavenReportException | RuntimeException e) {
            if (failOnError) {
                throw e;
            }
            getLog().error("Error while creating javadoc report: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getOutputName() {
        return (isTest() ? "test" : "") + "apidocs" + "/index";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExternalReport() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <br>
     * The logic is the following:
     * <table><caption>Can-generate-report Matrix</caption>
     *   <tbody>
     *     <tr>
     *       <th> isAggregator </th>
     *       <th> hasSourceFiles </th>
     *       <th> isRootProject </th>
     *       <th> Generate Report </th>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>True</td>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>False</td>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>True</td>
     *       <td>False</td>
     *     </tr>
     *     <tr>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>False</td>
     *       <td>False</td>
     *     </tr>
     *     <tr>
     *       <td>False</td>
     *       <td>True</td>
     *       <td>True</td>
     *       <td>True</td>
     *     </tr>
     *     <tr>
     *       <td>False</td>
     *       <td>True</td>
     *       <td>False</td>
     *       <td>True</td>
     *     </tr>
     *     <tr>
     *        <td>False</td>
     *        <td>False</td>
     *        <td>True</td>
     *        <td>False</td>
     *      </tr>
     *      <tr>
     *        <td>False</td>
     *        <td>False</td>
     *        <td>False</td>
     *        <td>False</td>
     *      </tr>
     *    </tbody>
     *  </table>
     */
    @Override
    public boolean canGenerateReport() throws MavenReportException {
        if (skip) {
            return false;
        }

        Collection<JavadocModule> sourcePaths = getSourcePaths();

        Collection<Path> collectedSourcePaths =
                sourcePaths.stream().flatMap(e -> e.getSourcePaths().stream()).collect(Collectors.toList());

        Map<Path, Collection<String>> files = getFiles(collectedSourcePaths);

        return canGenerateReport(files);
    }

    /** {@inheritDoc} */
    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    /** {@inheritDoc} */
    @Override
    public File getReportOutputDirectory() {
        if (reportOutputDirectory == null) {
            reportOutputDirectory = new File(getOutputDirectory());
        }

        return reportOutputDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public void setReportOutputDirectory(File reportOutputDirectory) {
        this.reportOutputDirectory = reportOutputDirectory;
        this.outputDirectory = reportOutputDirectory;
    }

    /** {@inheritDoc} */
    @Override
    protected String getPluginReportOutputDirectory() {
        return getReportOutputDirectory().getAbsolutePath() + "/" + (isTest() ? "test" : "") + "apidocs";
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!canGenerateReport()) {
                String reportMojoInfo = mojoExecution.getPlugin().getId() + ":" + mojoExecution.getGoal();
                getLog().info("Skipping " + reportMojoInfo + " report goal");
                return;
            }
        } catch (MavenReportException e) {
            throw new MojoExecutionException("Failed to determine whether report can be generated", e);
        }

        File outputDirectory = new File(getOutputDirectory());

        String filename = getOutputName() + ".html";

        Locale locale = SiteTool.DEFAULT_LOCALE;

        try {
            String reportMojoInfo = mojoExecution.getPlugin().getId() + ":" + mojoExecution.getGoal();
            DocumentRenderingContext docRenderingContext =
                    new DocumentRenderingContext(outputDirectory, filename, reportMojoInfo);

            SiteRendererSink sink = new SiteRendererSink(docRenderingContext);

            generate(sink, null, locale);
        } catch (MavenReportException | RuntimeException e) {
            failOnError("An error has occurred in " + getName(Locale.ENGLISH) + " report generation", e);
        }
    }

    /**
     * Gets the resource bundle for the specified locale.
     *
     * @param locale the locale of the currently generated report
     * @return the resource bundle for the requested locale
     */
    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("javadoc-report", locale, getClass().getClassLoader());
    }
}
