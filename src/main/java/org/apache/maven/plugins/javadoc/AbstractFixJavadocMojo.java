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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.library.ClassLibraryBuilder;
import com.thoughtworks.qdox.library.OrderedClassLibraryBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaConstructor;
import com.thoughtworks.qdox.model.JavaExecutable;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaGenericDeclaration;
import com.thoughtworks.qdox.model.JavaMember;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.model.JavaTypeVariable;
import com.thoughtworks.qdox.parser.ParseException;
import com.thoughtworks.qdox.type.TypeResolver;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract class to fix Javadoc documentation and tags in source files.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/javadoc/doc-comment-spec.html#where-tags-can-be-used">Where Tags Can Be Used</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.6
 */
public abstract class AbstractFixJavadocMojo extends AbstractMojo {
    /**
     * The vm line separator
     */
    private static final String EOL = System.lineSeparator();

    /**
     * Tag name for &#64;author *
     */
    private static final String AUTHOR_TAG = "author";

    /**
     * Tag name for &#64;version *
     */
    private static final String VERSION_TAG = "version";

    /**
     * Tag name for &#64;since *
     */
    private static final String SINCE_TAG = "since";

    /**
     * Tag name for &#64;param *
     */
    private static final String PARAM_TAG = "param";

    /**
     * Tag name for &#64;return *
     */
    private static final String RETURN_TAG = "return";

    /**
     * Tag name for &#64;throws *
     */
    private static final String THROWS_TAG = "throws";

    /**
     * Tag name for &#64;link *
     */
    private static final String LINK_TAG = "link";

    /**
     * Tag name for {&#64;inheritDoc} *
     */
    private static final String INHERITED_TAG = "{@inheritDoc}";

    /**
     * Start Javadoc String i.e. <code>&#47;&#42;&#42;</code> *
     */
    private static final String START_JAVADOC = "/**";

    /**
     * End Javadoc String i.e. <code>&#42;&#47;</code> *
     */
    private static final String END_JAVADOC = "*/";

    /**
     * Javadoc Separator i.e. <code> &#42; </code> *
     */
    private static final String SEPARATOR_JAVADOC = " * ";

    /**
     * Inherited Javadoc i.e. <code>&#47;&#42;&#42; {&#64;inheritDoc} &#42;&#47;</code> *
     */
    private static final String INHERITED_JAVADOC = START_JAVADOC + " " + INHERITED_TAG + " " + END_JAVADOC;

    /**
     * <code>all</code> parameter used by {@link #fixTags} *
     */
    private static final String FIX_TAGS_ALL = "all";

    /**
     * <code>public</code> parameter used by {@link #level} *
     */
    private static final String LEVEL_PUBLIC = "public";

    /**
     * <code>protected</code> parameter used by {@link #level} *
     */
    private static final String LEVEL_PROTECTED = "protected";

    /**
     * <code>package</code> parameter used by {@link #level} *
     */
    private static final String LEVEL_PACKAGE = "package";

    /**
     * <code>private</code> parameter used by {@link #level} *
     */
    private static final String LEVEL_PRIVATE = "private";

    /**
     * The Clirr Maven plugin groupId <code>org.codehaus.mojo</code> *
     */
    private static final String CLIRR_MAVEN_PLUGIN_GROUPID = "org.codehaus.mojo";

    /**
     * The Clirr Maven plugin artifactId <code>clirr-maven-plugin</code> *
     */
    private static final String CLIRR_MAVEN_PLUGIN_ARTIFACTID = "clirr-maven-plugin";

    /**
     * The latest Clirr Maven plugin version <code>2.8</code> *
     */
    private static final String CLIRR_MAVEN_PLUGIN_VERSION = "2.8";

    /**
     * The Clirr Maven plugin goal <code>check</code> *
     */
    private static final String CLIRR_MAVEN_PLUGIN_GOAL = "check";

    private static final JavaVersion JAVA_VERSION = JavaVersion.JAVA_SPECIFICATION_VERSION;

    /**
     * Java Files Pattern.
     */
    public static final String JAVA_FILES = "**\\/*.java";

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Input handler, needed for command line handling.
     */
    private InputHandler inputHandler;

    public AbstractFixJavadocMojo(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Version to compare the current code against using the
     * <a href="https://www.mojohaus.org/clirr-maven-plugin/">Clirr Maven Plugin</a>.
     * <br/>
     * See <a href="#defaultSince">defaultSince</a>.
     */
    @Parameter(property = "comparisonVersion", defaultValue = "(,${project.version})")
    private String comparisonVersion;

    /**
     * Default value for the Javadoc tag <code>&#64;author</code>.
     */
    @Parameter(property = "defaultAuthor", defaultValue = "${user.name}")
    private String defaultAuthor;

    /**
     * Default value for the Javadoc tag <code>&#64;since</code>.
     */
    @Parameter(property = "defaultSince", defaultValue = "${project.version}")
    private String defaultSince;

    /**
     * Default value for the Javadoc tag <code>&#64;version</code>.
     */
    @Parameter(property = "defaultVersion")
    private String defaultVersion;

    /**
     * The file encoding to use when reading the source files. If the property
     * <code>project.build.sourceEncoding</code> is not set, the platform default encoding is used.
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /**
     * Comma separated excludes Java files, i.e. <code>&#42;&#42;/&#42;Test.java</code>.
     */
    @Parameter(property = "excludes")
    private String excludes;

    /**
     * Comma separated tags to fix in classes, interfaces or methods Javadoc comments.
     * Possible values are:
     * <ul>
     * <li>all (fix all Javadoc tags)</li>
     * <li>author (fix only &#64;author tag)</li>
     * <li>version (fix only &#64;version tag)</li>
     * <li>since (fix only &#64;since tag)</li>
     * <li>param (fix only &#64;param tag)</li>
     * <li>return (fix only &#64;return tag)</li>
     * <li>throws (fix only &#64;throws tag)</li>
     * <li>link (fix only &#64;link tag)</li>
     * </ul>
     */
    @Parameter(property = "fixTags", defaultValue = FIX_TAGS_ALL)
    private String fixTags;

    /**
     * Flag to fix the classes or interfaces Javadoc comments according the <code>level</code>.
     */
    @Parameter(property = "fixClassComment", defaultValue = "true")
    private boolean fixClassComment;

    /**
     * Flag to fix the fields Javadoc comments according the <code>level</code>.
     */
    @Parameter(property = "fixFieldComment", defaultValue = "true")
    private boolean fixFieldComment;

    /**
     * Flag to fix the methods Javadoc comments according the <code>level</code>.
     */
    @Parameter(property = "fixMethodComment", defaultValue = "true")
    private boolean fixMethodComment;

    /**
     * <p>Flag to remove throws tags from unknown classes.</p>
     * <p><strong>NOTE:</strong>Since 3.1.0 the default value has been changed to {@code true},
     * due to JavaDoc 8 strictness.
     */
    @Parameter(property = "removeUnknownThrows", defaultValue = "true")
    private boolean removeUnknownThrows;

    /**
     * Forcing the goal execution i.e. skip warranty messages (not recommended).
     */
    @Parameter(property = "force")
    private boolean force;

    /**
     * Flag to ignore or not Clirr.
     */
    @Parameter(property = "ignoreClirr", defaultValue = "false")
    protected boolean ignoreClirr;

    /**
     * Comma separated includes Java files, i.e. <code>&#42;&#42;/&#42;Test.java</code>.
     * <p/>
     * <strong>Note:</strong> default value is {@code **\/*.java}.
     */
    @Parameter(property = "includes", defaultValue = JAVA_FILES)
    private String includes;

    /**
     * Specifies the access level for classes and members to show in the Javadocs.
     * Possible values are:
     * <ul>
     * <li>public (shows only public classes and members)</li>
     * <li>protected (shows only public and protected classes and members)</li>
     * <li>package (shows all classes and members not marked private)</li>
     * <li>private (shows all classes and members)</li>
     * </ul>
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/man/javadoc.html#options-for-javadoc">private, protected, public, package options for Javadoc</a>
     */
    @Parameter(property = "level", defaultValue = LEVEL_PROTECTED)
    private String level;

    /**
     * Output directory where Java classes will be rewritten.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.sourceDirectory}")
    private File outputDirectory;

    /**
     * The Maven Project Object.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The current user system settings for use in Maven.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    // ----------------------------------------------------------------------
    // Internal fields
    // ----------------------------------------------------------------------

    /**
     * The current project class loader.
     */
    private ClassLoader projectClassLoader;

    /**
     * Split {@link #fixTags} by comma.
     *
     * @see #init()
     */
    private String[] fixTagsSplitted;

    /**
     * New classes found by Clirr.
     */
    private List<String> clirrNewClasses;

    /**
     * New Methods in a Class (the key) found by Clirr.
     */
    private Map<String, List<String>> clirrNewMethods;

    /**
     * List of classes where <code>&#42;since</code> is added. Will be used to add or not this tag in the methods.
     */
    private List<String> sinceClasses;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!fixClassComment && !fixFieldComment && !fixMethodComment) {
            getLog().info("Specified to NOT fix classes, fields and methods. Nothing to do.");
            return;
        }

        // verify goal params
        init();

        if (fixTagsSplitted.length == 0) {
            getLog().info("No fix tag specified. Nothing to do.");
            return;
        }

        // add warranty msg
        if (!preCheck()) {
            return;
        }

        // run clirr
        try {
            executeClirr();
        } catch (MavenInvocationException e) {
            if (getLog().isDebugEnabled()) {
                getLog().error("MavenInvocationException: " + e.getMessage(), e);
            } else {
                getLog().error("MavenInvocationException: " + e.getMessage());
            }
            getLog().info("Clirr is ignored.");
        }

        // run qdox and process
        try {
            Collection<JavaClass> javaClasses = getQdoxClasses();

            if (javaClasses != null) {
                for (JavaClass javaClass : javaClasses) {
                    processFix(javaClass);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("IOException: " + e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------------
    // protected methods
    // ----------------------------------------------------------------------

    protected final MavenProject getProject() {
        return project;
    }

    /**
     * @param p not null maven project.
     * @return the artifact type.
     */
    protected String getArtifactType(MavenProject p) {
        return p.getArtifact().getType();
    }

    /**
     * @param p not null maven project.
     * @return the list of source paths for the given project.
     */
    protected List<String> getProjectSourceRoots(MavenProject p) {
        return p.getCompileSourceRoots() == null
                ? Collections.emptyList()
                : new LinkedList<>(p.getCompileSourceRoots());
    }

    /**
     * @param p not null
     * @return the compile classpath elements
     * @throws DependencyResolutionRequiredException
     *          if any
     */
    protected List<String> getCompileClasspathElements(MavenProject p) throws DependencyResolutionRequiredException {
        return p.getCompileClasspathElements() == null
                ? Collections.emptyList()
                : new LinkedList<>(p.getCompileClasspathElements());
    }

    /**
     * @param javaExecutable not null
     * @return the fully qualify name of javaMethod with signature
     */
    protected static String getJavaMethodAsString(JavaExecutable javaExecutable) {
        return javaExecutable.getDeclaringClass().getFullyQualifiedName() + "#" + javaExecutable.getCallSignature();
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * Init goal parameters.
     */
    private void init() {
        // defaultSince
        int i = defaultSince.indexOf("-" + Artifact.SNAPSHOT_VERSION);
        if (i != -1) {
            defaultSince = defaultSince.substring(0, i);
        }

        // fixTags
        if (!FIX_TAGS_ALL.equalsIgnoreCase(fixTags.trim())) {
            String[] split = StringUtils.split(fixTags, ",");
            List<String> filtered = new LinkedList<>();
            for (String aSplit : split) {
                String s = aSplit.trim();
                if (JavadocUtil.equalsIgnoreCase(
                        s,
                        FIX_TAGS_ALL,
                        AUTHOR_TAG,
                        VERSION_TAG,
                        SINCE_TAG,
                        PARAM_TAG,
                        THROWS_TAG,
                        LINK_TAG,
                        RETURN_TAG)) {
                    filtered.add(s);
                } else {
                    if (getLog().isWarnEnabled()) {
                        getLog().warn("Unrecognized '" + s + "' for fixTags parameter. Ignored it!");
                    }
                }
            }
            fixTags = StringUtils.join(filtered.iterator(), ",");
        }
        fixTagsSplitted = StringUtils.split(fixTags, ",");

        // encoding
        if (encoding == null || encoding.isEmpty()) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("File encoding has not been set, using platform encoding " + Charset.defaultCharset()
                        + ", i.e. build is platform dependent!");
            }
            encoding = Charset.defaultCharset().name();
        }

        // level
        level = level.trim();
        if (!JavadocUtil.equalsIgnoreCase(level, LEVEL_PUBLIC, LEVEL_PROTECTED, LEVEL_PACKAGE, LEVEL_PRIVATE)) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("Unrecognized '" + level + "' for level parameter, using 'protected' level.");
            }
            level = "protected";
        }
    }

    /**
     * @return <code>true</code> if the user wants to proceed, <code>false</code> otherwise.
     * @throws MojoExecutionException if any
     */
    private boolean preCheck() throws MojoExecutionException {
        if (force) {
            return true;
        }

        if (outputDirectory != null
                && !outputDirectory
                        .getAbsolutePath()
                        .equals(getProjectSourceDirectory().getAbsolutePath())) {
            return true;
        }

        if (!settings.isInteractiveMode()) {
            getLog().error("Maven is not interacting with the user for input. "
                    + "Verify the <interactiveMode/> configuration in your settings.");
            return false;
        }

        getLog().warn("");
        getLog().warn("    WARRANTY DISCLAIMER");
        getLog().warn("");
        getLog().warn("All warranties with regard to this Maven goal are disclaimed!");
        getLog().warn("The changes will be done directly in the source code.");
        getLog().warn(
                        "The Maven Team strongly recommends committing the code to source code management BEFORE executing this goal.");
        getLog().warn("");

        while (true) {
            getLog().info("Are you sure you want to proceed? [Y]es [N]o");

            try {
                String userExpression = inputHandler.readLine();
                if (userExpression == null || JavadocUtil.equalsIgnoreCase(userExpression, "Y", "Yes")) {
                    getLog().info("OK, let's proceed...");
                    break;
                }
                if (JavadocUtil.equalsIgnoreCase(userExpression, "N", "No")) {
                    getLog().info("OK, I will not change your source code.");
                    return false;
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to read from standard input.", e);
            }
        }

        return true;
    }

    /**
     * @return the source dir as File for the given project
     */
    private File getProjectSourceDirectory() {
        return new File(project.getBuild().getSourceDirectory());
    }

    /**
     * Invoke Maven to run clirr-maven-plugin to find API differences.
     *
     * @throws MavenInvocationException if any
     */
    private void executeClirr() throws MavenInvocationException {
        if (ignoreClirr) {
            getLog().info("Clirr is ignored.");
            return;
        }

        String clirrGoal = getFullClirrGoal();

        // https://www.mojohaus.org/clirr-maven-plugin/check-mojo.html
        File clirrTextOutputFile = FileUtils.createTempFile(
                "clirr", ".txt", new File(project.getBuild().getDirectory()));
        Properties properties = new Properties();
        properties.put("textOutputFile", clirrTextOutputFile.getAbsolutePath());
        properties.put("comparisonVersion", comparisonVersion);
        properties.put("failOnError", "false");
        properties.put("minSeverity", "info");

        File invokerDir = new File(project.getBuild().getDirectory(), "invoker");
        invokerDir.mkdirs();
        File invokerLogFile = FileUtils.createTempFile("clirr-maven-plugin", ".txt", invokerDir);

        JavadocUtil.invokeMaven(
                getLog(),
                session.getRepositorySession().getLocalRepository().getBasedir(),
                project.getFile(),
                Collections.singletonList(clirrGoal),
                properties,
                invokerLogFile,
                session.getRequest().getGlobalSettingsFile(),
                session.getRequest().getUserSettingsFile(),
                session.getRequest().getGlobalToolchainsFile(),
                session.getRequest().getUserToolchainsFile());

        try {
            if (invokerLogFile.exists()) {
                String invokerLogContent = StringUtils.unifyLineSeparators(FileUtils.fileRead(invokerLogFile, "UTF-8"));
                // see org.codehaus.mojo.clirr.AbstractClirrMojo#getComparisonArtifact()
                final String artifactNotFoundMsg = "Unable to find a previous version of the project in the repository";
                if (invokerLogContent.contains(artifactNotFoundMsg)) {
                    getLog().warn("No previous artifact has been deployed, Clirr is ignored.");
                    return;
                }
            }
        } catch (IOException e) {
            getLog().debug("IOException: " + e.getMessage());
        }

        try {
            parseClirrTextOutputFile(clirrTextOutputFile);
        } catch (IOException e) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("IOException: " + e.getMessage(), e);
            }
            getLog().info("IOException when parsing Clirr output '" + clirrTextOutputFile.getAbsolutePath()
                    + "', Clirr is ignored.");
        }
    }

    /**
     * @param clirrTextOutputFile not null
     * @throws IOException if any
     */
    private void parseClirrTextOutputFile(File clirrTextOutputFile) throws IOException {
        if (!clirrTextOutputFile.exists()) {
            if (getLog().isInfoEnabled()) {
                getLog().info("No Clirr output file '" + clirrTextOutputFile.getAbsolutePath()
                        + "' exists, Clirr is ignored.");
            }
            return;
        }

        if (getLog().isInfoEnabled()) {
            getLog().info("Clirr output file was created: " + clirrTextOutputFile.getAbsolutePath());
        }

        clirrNewClasses = new LinkedList<>();
        clirrNewMethods = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(clirrTextOutputFile.toPath(), StandardCharsets.UTF_8)) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] split = StringUtils.split(line, ":");
                if (split.length != 4) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Unable to parse the Clirr line: " + line);
                    }
                    continue;
                }

                int code;
                try {
                    code = Integer.parseInt(split[1].trim());
                } catch (NumberFormatException e) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Unable to parse the clirr line: " + line);
                    }
                    continue;
                }

                // https://clirr.sourceforge.net/clirr-core/exegesis.html
                // 7011 - Method Added
                // 7012 - Method Added to Interface
                // 8000 - Class Added

                // CHECKSTYLE_OFF: MagicNumber
                switch (code) {
                    case 7011:
                        methodAdded(split);
                        break;
                    case 7012:
                        methodAdded(split);
                        break;
                    case 8000:
                        clirrNewClasses.add(split[2].trim());
                        break;
                    default:
                        break;
                }
                // CHECKSTYLE_ON: MagicNumber
            }
        }
        if (clirrNewClasses.isEmpty() && clirrNewMethods.isEmpty()) {
            getLog().info("Clirr did NOT find any API differences.");
        } else {
            getLog().info("Clirr found API differences; e.g. new classes, interfaces, or methods.");
        }
    }

    private void methodAdded(String[] split) {
        List<String> list = clirrNewMethods.get(split[2].trim());
        if (list == null) {
            list = new ArrayList<>();
        }
        String[] splits2 = StringUtils.split(split[3].trim(), "'");
        if (splits2.length != 3) {
            return;
        }
        list.add(splits2[1].trim());
        clirrNewMethods.put(split[2].trim(), list);
    }

    /**
     * @param tag not null
     * @return <code>true</code> if <code>tag</code> is defined in {@link #fixTags}.
     */
    private boolean fixTag(String tag) {
        if (fixTagsSplitted.length == 1 && fixTagsSplitted[0].equals(FIX_TAGS_ALL)) {
            return true;
        }

        for (String aFixTagsSplitted : fixTagsSplitted) {
            if (aFixTagsSplitted.trim().equals(tag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calling Qdox to find {@link JavaClass} objects from the Maven project sources.
     * Ignore java class if Qdox has parsing errors.
     *
     * @return an array of {@link JavaClass} found by QDox
     * @throws IOException            if any
     * @throws MojoExecutionException if any
     */
    private Collection<JavaClass> getQdoxClasses() throws IOException, MojoExecutionException {
        if ("pom".equalsIgnoreCase(project.getPackaging())) {
            getLog().warn("This project has 'pom' packaging, no Java sources is available.");
            return null;
        }

        List<File> javaFiles = new LinkedList<>();
        for (String sourceRoot : getProjectSourceRoots(project)) {
            File f = new File(sourceRoot);
            if (f.isDirectory()) {
                javaFiles.addAll(FileUtils.getFiles(f, includes, excludes, true));
            } else {
                if (getLog().isWarnEnabled()) {
                    getLog().warn(f + " doesn't exist. Ignored it.");
                }
            }
        }

        ClassLibraryBuilder classLibraryBuilder = new OrderedClassLibraryBuilder();
        classLibraryBuilder.appendClassLoader(getProjectClassLoader());

        JavaProjectBuilder builder = new JavaProjectBuilder(classLibraryBuilder);
        builder.setEncoding(encoding);
        for (File f : javaFiles) {
            if (!f.getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith(".java") && getLog().isWarnEnabled()) {
                getLog().warn("'" + f + "' is not a Java file. Ignored it.");
                continue;
            }

            try {
                builder.addSource(f);
            } catch (ParseException e) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("QDOX ParseException: " + e.getMessage() + ". Can't fix it.");
                }
            }
        }

        return builder.getClasses();
    }

    /**
     * @return the classLoader for the given project using lazy instantiation.
     * @throws MojoExecutionException if any
     */
    private ClassLoader getProjectClassLoader() throws MojoExecutionException {
        if (projectClassLoader == null) {
            List<String> classPath;
            try {
                classPath = getCompileClasspathElements(project);
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("DependencyResolutionRequiredException: " + e.getMessage(), e);
            }

            List<URL> urls = new ArrayList<>(classPath.size());
            for (String filename : classPath) {
                try {
                    urls.add(new File(filename).toURI().toURL());
                } catch (MalformedURLException | IllegalArgumentException e) {
                    throw new MojoExecutionException("MalformedURLException: " + e.getMessage(), e);
                }
            }

            ClassLoader parent = null;
            if (JAVA_VERSION.isAtLeast("9")) {
                try {
                    parent = (ClassLoader) MethodUtils.invokeStaticMethod(ClassLoader.class, "getPlatformClassLoader");
                } catch (Exception e) {
                    throw new MojoExecutionException(
                            "Failed to invoke ClassLoader#getPlatformClassLoader() dynamically", e);
                }
            }

            projectClassLoader = new URLClassLoader(urls.toArray(new URL[0]), parent);
        }

        return projectClassLoader;
    }

    /**
     * Process the given {@link JavaClass}, ie add missing javadoc tags depending user parameters.
     *
     * @param javaClass not null
     * @throws IOException            if any
     * @throws MojoExecutionException if any
     */
    private void processFix(JavaClass javaClass) throws IOException, MojoExecutionException {
        // Skipping inner classes
        if (javaClass.isInner()) {
            return;
        }

        File javaFile;
        try {
            javaFile = Paths.get(javaClass.getSource().getURL().toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        // the original java content in memory
        final String originalContent = StringUtils.unifyLineSeparators(FileUtils.fileRead(javaFile, encoding));

        if (getLog().isDebugEnabled()) {
            getLog().debug("Analyzing " + javaClass.getFullyQualifiedName());
        }

        final StringWriter stringWriter = new StringWriter();
        boolean changeDetected = false;
        try (BufferedReader reader = new BufferedReader(new StringReader(originalContent))) {

            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final String indent = autodetectIndentation(line);

                // fixing classes
                if (javaClass.getComment() == null
                        && javaClass.getAnnotations() != null
                        && !javaClass.getAnnotations().isEmpty()) {
                    if (lineNumber == javaClass.getAnnotations().get(0).getLineNumber()) {
                        changeDetected |= fixClassComment(stringWriter, originalContent, javaClass, indent);

                        takeCareSingleComment(stringWriter, originalContent, javaClass);
                    }
                } else if (lineNumber == javaClass.getLineNumber()) {
                    changeDetected |= fixClassComment(stringWriter, originalContent, javaClass, indent);

                    takeCareSingleComment(stringWriter, originalContent, javaClass);
                }

                // fixing fields
                if (javaClass.getFields() != null) {
                    for (JavaField field : javaClass.getFields()) {
                        if (lineNumber == field.getLineNumber()) {
                            changeDetected |= fixFieldComment(stringWriter, javaClass, field, indent);
                        }
                    }
                }

                // fixing methods
                if (javaClass.getConstructors() != null) {
                    for (JavaConstructor method : javaClass.getConstructors()) {
                        if (lineNumber == method.getLineNumber()) {
                            final boolean commentUpdated =
                                    fixMethodComment(stringWriter, originalContent, method, indent);
                            if (commentUpdated) {
                                takeCareSingleComment(stringWriter, originalContent, method);
                            }
                            changeDetected |= commentUpdated;
                        }
                    }
                }

                // fixing methods
                for (JavaMethod method : javaClass.getMethods()) {
                    int methodLineNumber;
                    if (method.getComment() == null && !method.getAnnotations().isEmpty()) {
                        methodLineNumber = method.getAnnotations().get(0).getLineNumber();
                    } else {
                        methodLineNumber = method.getLineNumber();
                    }

                    if (lineNumber == methodLineNumber) {
                        final boolean commentUpdated = fixMethodComment(stringWriter, originalContent, method, indent);
                        if (commentUpdated) {
                            takeCareSingleComment(stringWriter, originalContent, method);
                        }
                        changeDetected |= commentUpdated;
                    }
                }

                stringWriter.write(line);
                stringWriter.write(EOL);
            }
        }

        if (changeDetected) {
            if (getLog().isInfoEnabled()) {
                getLog().info("Saving changes to " + javaClass.getFullyQualifiedName());
            }

            if (outputDirectory != null
                    && !outputDirectory
                            .getAbsolutePath()
                            .equals(getProjectSourceDirectory().getAbsolutePath())) {
                String path = StringUtils.replace(
                        javaFile.getAbsolutePath().replaceAll("\\\\", "/"),
                        project.getBuild().getSourceDirectory().replaceAll("\\\\", "/"),
                        "");
                javaFile = new File(outputDirectory, path);
                javaFile.getParentFile().mkdirs();
            }
            writeFile(javaFile, encoding, stringWriter.toString());
        } else {
            if (getLog().isDebugEnabled()) {
                getLog().debug("No changes made to " + javaClass.getFullyQualifiedName());
            }
        }
    }

    /**
     * Take care of block or single comments between Javadoc comment and entity declaration ie:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;{Javadoc&nbsp;Comment}</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f7f5f">&#47;&#42;</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f7f5f">&#42;&nbsp;{Block&nbsp;Comment}</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f7f5f">&#42;&#47;</font><br />
     * <font color="#808080">7</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f7f5f">&#47;&#47;&nbsp;{Single&nbsp;comment}</font><br />
     * <font color="#808080">8</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font>
     * </code>
     *
     * @param stringWriter    not null
     * @param originalContent not null
     * @param entity          not null
     * @throws IOException if any
     * @see #extractOriginalJavadoc
     */
    private void takeCareSingleComment(
            final StringWriter stringWriter, final String originalContent, final JavaAnnotatedElement entity)
            throws IOException {
        if (entity.getComment() == null) {
            return;
        }

        String javadocComment = trimRight(extractOriginalJavadoc(originalContent, entity));
        String extraComment = javadocComment.substring(javadocComment.indexOf(END_JAVADOC) + END_JAVADOC.length());
        if (extraComment != null && !extraComment.isEmpty()) {
            if (extraComment.contains(EOL)) {
                stringWriter.write(extraComment.substring(extraComment.indexOf(EOL) + EOL.length()));
            } else {
                stringWriter.write(extraComment);
            }
            stringWriter.write(EOL);
        }
    }

    /**
     * Add/update Javadoc class comment.
     *
     * @param stringWriter
     * @param originalContent
     * @param javaClass
     * @param indent
     * @return {@code true} if the comment is updated, otherwise {@code false}
     * @throws MojoExecutionException
     * @throws IOException
     */
    private boolean fixClassComment(
            final StringWriter stringWriter,
            final String originalContent,
            final JavaClass javaClass,
            final String indent)
            throws MojoExecutionException, IOException {
        if (!fixClassComment) {
            return false;
        }

        if (!isInLevel(javaClass.getModifiers())) {
            return false;
        }

        // add
        if (javaClass.getComment() == null) {
            addDefaultClassComment(stringWriter, javaClass, indent);
            return true;
        }

        // update
        return updateEntityComment(stringWriter, originalContent, javaClass, indent);
    }

    /**
     * @param modifiers list of modifiers (public, private, protected, package)
     * @return <code>true</code> if modifier is align with <code>level</code>.
     */
    private boolean isInLevel(List<String> modifiers) {
        if (LEVEL_PUBLIC.equalsIgnoreCase(level.trim())) {
            return modifiers.contains(LEVEL_PUBLIC);
        }

        if (LEVEL_PROTECTED.equalsIgnoreCase(level.trim())) {
            return modifiers.contains(LEVEL_PUBLIC) || modifiers.contains(LEVEL_PROTECTED);
        }

        if (LEVEL_PACKAGE.equalsIgnoreCase(level.trim())) {
            return !modifiers.contains(LEVEL_PRIVATE);
        }

        // should be private (shows all classes and members)
        return true;
    }

    /**
     * Add a default Javadoc for the given class, i.e.:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;{Comment&nbsp;based&nbsp;on&nbsp;the&nbsp;class&nbsp;name}</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@author&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingAuthor}</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@version&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingVersion}</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@since&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingSince&nbsp;and&nbsp;new&nbsp;classes
     * from&nbsp;previous&nbsp;version}</font><br />
     * <font color="#808080">7</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">8</font>&nbsp;<font color="#7f0055"><b>public&nbsp;class&nbsp;</b></font>
     * <font color="#000000">DummyClass&nbsp;</font><font color="#000000">{}</font></code>
     * </code>
     *
     * @param stringWriter not null
     * @param javaClass    not null
     * @param indent       not null
     * @see #getDefaultClassJavadocComment(JavaClass)
     * @see #appendDefaultAuthorTag(StringBuilder, String)
     * @see #appendDefaultSinceTag(StringBuilder, String)
     * @see #appendDefaultVersionTag(StringBuilder, String)
     */
    private void addDefaultClassComment(
            final StringWriter stringWriter, final JavaClass javaClass, final String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append(START_JAVADOC);
        sb.append(EOL);
        sb.append(indent).append(SEPARATOR_JAVADOC);
        sb.append(getDefaultClassJavadocComment(javaClass));
        sb.append(EOL);

        appendSeparator(sb, indent);

        appendDefaultAuthorTag(sb, indent);

        appendDefaultVersionTag(sb, indent);

        if (fixTag(SINCE_TAG)) {
            if (!ignoreClirr) {
                if (isNewClassFromLastVersion(javaClass)) {
                    appendDefaultSinceTag(sb, indent);
                }
            } else {
                appendDefaultSinceTag(sb, indent);
                addSinceClasses(javaClass);
            }
        }

        sb.append(indent).append(" ").append(END_JAVADOC);
        sb.append(EOL);

        stringWriter.write(sb.toString());
    }

    /**
     * Add Javadoc field comment, only for static fields or interface fields.
     *
     * @param stringWriter not null
     * @param javaClass    not null
     * @param field        not null
     * @param indent       not null
     * @return {@code true} if comment was updated, otherwise {@code false}
     * @throws IOException if any
     */
    private boolean fixFieldComment(
            final StringWriter stringWriter, final JavaClass javaClass, final JavaField field, final String indent)
            throws IOException {
        if (!fixFieldComment) {
            return false;
        }

        if (!javaClass.isInterface() && (!isInLevel(field.getModifiers()) || !field.isStatic())) {
            return false;
        }

        // add
        if (field.getComment() == null) {
            addDefaultFieldComment(stringWriter, field, indent);
            return true;
        }

        // no update
        return false;
    }

    /**
     * Add a default Javadoc for the given field, i.e.:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;&nbsp;Constant&nbsp;</font><font color="#7f7f9f">&lt;code&gt;</font>
     * <font color="#3f5fbf">MY_STRING_CONSTANT=&#34;value&#34;</font>
     * <font color="#7f7f9f">&lt;/code&gt;&nbsp;</font><font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;static&nbsp;final&nbsp;</b></font>
     * <font color="#000000">String&nbsp;MY_STRING_CONSTANT&nbsp;=&nbsp;</font>
     * <font color="#2a00ff">&#34;value&#34;</font><font color="#000000">;</font>
     * </code>
     *
     * @param stringWriter not null
     * @param field        not null
     * @param indent       not null
     * @throws IOException if any
     */
    private void addDefaultFieldComment(final StringWriter stringWriter, final JavaField field, final String indent)
            throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append(START_JAVADOC).append(" ");
        sb.append("Constant <code>").append(field.getName());

        if (StringUtils.isNotEmpty(field.getInitializationExpression())) {
            String qualifiedName = field.getType().getFullyQualifiedName();

            if (qualifiedName.equals(Byte.TYPE.toString())
                    || qualifiedName.equals(Short.TYPE.toString())
                    || qualifiedName.equals(Integer.TYPE.toString())
                    || qualifiedName.equals(Long.TYPE.toString())
                    || qualifiedName.equals(Float.TYPE.toString())
                    || qualifiedName.equals(Double.TYPE.toString())
                    || qualifiedName.equals(Boolean.TYPE.toString())
                    || qualifiedName.equals(Character.TYPE.toString())) {
                sb.append("=");
                sb.append(StringEscapeUtils.escapeHtml4(
                        field.getInitializationExpression().trim()));
            }

            if (qualifiedName.equals(String.class.getName())) {
                StringBuilder value = new StringBuilder();
                String[] lines = getLines(field.getInitializationExpression());
                for (String line : lines) {
                    StringTokenizer token = new StringTokenizer(line.trim(), "\"\n\r");
                    while (token.hasMoreTokens()) {
                        String s = token.nextToken();

                        if (s.trim().equals("+")) {
                            continue;
                        }
                        if (s.trim().endsWith("\\")) {
                            s += "\"";
                        }
                        value.append(s);
                    }
                }

                sb.append("=\"");
                String escapedValue = StringEscapeUtils.escapeHtml4(value.toString());
                // reduce the size
                // CHECKSTYLE_OFF: MagicNumber
                if (escapedValue.length() < 40) {
                    sb.append(escapedValue).append("\"");
                } else {
                    sb.append(escapedValue, 0, 39).append("\"{trunked}");
                }
                // CHECKSTYLE_ON: MagicNumber
            }
        }

        sb.append("</code> ").append(END_JAVADOC);
        sb.append(EOL);

        stringWriter.write(sb.toString());
    }

    /**
     * Add/update Javadoc method comment.
     *
     * @param stringWriter    not null
     * @param originalContent not null
     * @param javaExecutable      not null
     * @param indent          not null
     * @return {@code true} if comment was updated, otherwise {@code false}
     * @throws MojoExecutionException if any
     * @throws IOException            if any
     */
    private boolean fixMethodComment(
            final StringWriter stringWriter,
            final String originalContent,
            final JavaExecutable javaExecutable,
            final String indent)
            throws MojoExecutionException, IOException {
        if (!fixMethodComment) {
            return false;
        }

        if (!javaExecutable.getDeclaringClass().isInterface() && !isInLevel(javaExecutable.getModifiers())) {
            return false;
        }

        // add
        if (javaExecutable.getComment() == null) {
            addDefaultMethodComment(stringWriter, javaExecutable, indent);
            return true;
        }

        // update
        return updateEntityComment(stringWriter, originalContent, javaExecutable, indent);
    }

    /**
     * Add in the buffer a default Javadoc for the given class:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;{Comment&nbsp;based&nbsp;on&nbsp;the&nbsp;method&nbsp;name}</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingParam}</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@return&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingReturn}</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@throws&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingThrows}</font><br />
     * <font color="#808080">7</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@since&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingSince&nbsp;and&nbsp;new&nbsp;classes
     * from&nbsp;previous&nbsp;version}</font><br />
     * <font color="#808080">8</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">9</font>&nbsp;<font color="#7f0055"><b>public&nbsp;</b></font>
     * <font color="#7f0055"><b>void&nbsp;</b></font><font color="#000000">dummyMethod</font>
     * <font color="#000000">(&nbsp;</font><font color="#000000">String&nbsp;s&nbsp;</font>
     * <font color="#000000">){}</font>
     * </code>
     *
     * @param stringWriter   not null
     * @param javaExecutable not null
     * @param indent         not null
     * @throws MojoExecutionException if any
     * @see #getDefaultMethodJavadocComment
     * @see #appendDefaultSinceTag(StringBuilder, String)
     */
    private void addDefaultMethodComment(
            final StringWriter stringWriter, final JavaExecutable javaExecutable, final String indent)
            throws MojoExecutionException {
        StringBuilder sb = new StringBuilder();

        // special case
        if (isInherited(javaExecutable)) {
            sb.append(indent).append(INHERITED_JAVADOC);
            sb.append(EOL);

            stringWriter.write(sb.toString());
            return;
        }

        sb.append(indent).append(START_JAVADOC);
        sb.append(EOL);
        sb.append(indent).append(SEPARATOR_JAVADOC);
        sb.append(getDefaultMethodJavadocComment(javaExecutable));
        sb.append(EOL);

        boolean separatorAdded = false;
        if (fixTag(PARAM_TAG)) {
            if (javaExecutable.getParameters() != null) {
                for (JavaParameter javaParameter : javaExecutable.getParameters()) {
                    separatorAdded = appendDefaultParamTag(sb, indent, separatorAdded, javaParameter);
                }
            }
            // is generic?
            if (javaExecutable.getTypeParameters() != null) {
                for (JavaTypeVariable<JavaGenericDeclaration> typeParam : javaExecutable.getTypeParameters()) {
                    separatorAdded = appendDefaultParamTag(sb, indent, separatorAdded, typeParam);
                }
            }
        }
        if (javaExecutable instanceof JavaMethod) {
            JavaMethod javaMethod = (JavaMethod) javaExecutable;
            if (fixTag(RETURN_TAG)
                    && javaMethod.getReturns() != null
                    && !javaMethod.getReturns().isVoid()) {
                separatorAdded = appendDefaultReturnTag(sb, indent, separatorAdded, javaMethod);
            }
        }
        if (fixTag(THROWS_TAG) && javaExecutable.getExceptions() != null) {
            for (JavaType exception : javaExecutable.getExceptions()) {
                separatorAdded = appendDefaultThrowsTag(sb, indent, separatorAdded, exception);
            }
        }
        if (fixTag(SINCE_TAG) && isNewMethodFromLastRevision(javaExecutable)) {
            separatorAdded = appendDefaultSinceTag(sb, indent, separatorAdded);
        }

        sb.append(indent).append(" ").append(END_JAVADOC);
        sb.append(EOL);

        stringWriter.write(sb.toString());
    }

    /**
     * @param stringWriter    not null
     * @param originalContent not null
     * @param entity          not null
     * @param indent          not null
     * @return the updated changeDetected flag
     * @throws MojoExecutionException if any
     * @throws IOException            if any
     */
    private boolean updateEntityComment(
            final StringWriter stringWriter,
            final String originalContent,
            final JavaAnnotatedElement entity,
            final String indent)
            throws MojoExecutionException, IOException {
        boolean changeDetected = false;

        String old = null;
        String s = stringWriter.toString();
        int i = s.lastIndexOf(START_JAVADOC);
        if (i != -1) {
            String tmp = s.substring(0, i);
            if (tmp.lastIndexOf(EOL) != -1) {
                tmp = tmp.substring(0, tmp.lastIndexOf(EOL));
            }

            old = stringWriter.getBuffer().substring(i);

            stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length());
            stringWriter.write(tmp);
            stringWriter.write(EOL);
        } else {
            changeDetected = true;
        }

        updateJavadocComment(stringWriter, originalContent, entity, indent);

        if (changeDetected) {
            return true; // return now if we already know there's a change
        }

        return !stringWriter.getBuffer().substring(i).equals(old);
    }

    /**
     * @param stringWriter    not null
     * @param originalContent not null
     * @param entity          not null
     * @param indent          not null
     * @throws MojoExecutionException if any
     * @throws IOException            if any
     */
    private void updateJavadocComment(
            final StringWriter stringWriter,
            final String originalContent,
            final JavaAnnotatedElement entity,
            final String indent)
            throws MojoExecutionException, IOException {
        if (entity.getComment() == null
                && (entity.getTags() == null || entity.getTags().isEmpty())) {
            return;
        }

        boolean isJavaExecutable = entity instanceof JavaExecutable;

        StringBuilder sb = new StringBuilder();

        // special case for inherited method
        if (isJavaExecutable) {
            JavaExecutable javaMethod = (JavaExecutable) entity;

            if (isInherited(javaMethod)) {
                // QDOX-154 could be empty
                if (StringUtils.isEmpty(javaMethod.getComment())) {
                    sb.append(indent).append(INHERITED_JAVADOC);
                    sb.append(EOL);
                    stringWriter.write(sb.toString());
                    return;
                }

                String javadoc = getJavadocComment(originalContent, javaMethod);

                // case: /** {@inheritDoc} */ or no tags
                if (hasInheritedTag(javadoc)
                        && (javaMethod.getTags() == null || javaMethod.getTags().isEmpty())) {
                    sb.append(indent).append(INHERITED_JAVADOC);
                    sb.append(EOL);
                    stringWriter.write(sb.toString());
                    return;
                }

                if (javadoc.contains(START_JAVADOC)) {
                    javadoc = javadoc.substring(javadoc.indexOf(START_JAVADOC) + START_JAVADOC.length());
                }
                if (javadoc.contains(END_JAVADOC)) {
                    javadoc = javadoc.substring(0, javadoc.indexOf(END_JAVADOC));
                }

                sb.append(indent).append(START_JAVADOC);
                sb.append(EOL);
                if (!javadoc.contains(INHERITED_TAG)) {
                    sb.append(indent).append(SEPARATOR_JAVADOC).append(INHERITED_TAG);
                    sb.append(EOL);
                    appendSeparator(sb, indent);
                }
                javadoc = removeLastEmptyJavadocLines(javadoc);
                javadoc = alignIndentationJavadocLines(javadoc, indent);
                sb.append(javadoc);
                sb.append(EOL);
                if (javaMethod.getTags() != null) {
                    for (DocletTag docletTag : javaMethod.getTags()) {
                        // Voluntary ignore these tags
                        if (JavadocUtil.equals(docletTag.getName(), PARAM_TAG, RETURN_TAG, THROWS_TAG)) {
                            continue;
                        }

                        String s = getJavadocComment(originalContent, entity, docletTag);
                        s = removeLastEmptyJavadocLines(s);
                        s = alignIndentationJavadocLines(s, indent);
                        sb.append(s);
                        sb.append(EOL);
                    }
                }
                sb.append(indent).append(" ").append(END_JAVADOC);
                sb.append(EOL);

                if (hasInheritedTag(sb.toString().trim())) {
                    sb = new StringBuilder();
                    sb.append(indent).append(INHERITED_JAVADOC);
                    sb.append(EOL);
                    stringWriter.write(sb.toString());
                    return;
                }

                stringWriter.write(sb.toString());
                return;
            }
        }

        sb.append(indent).append(START_JAVADOC);
        sb.append(EOL);

        // comment
        if (StringUtils.isNotEmpty(entity.getComment())) {
            updateJavadocComment(sb, originalContent, entity, indent);
        } else {
            addDefaultJavadocComment(sb, entity, indent, isJavaExecutable);
        }

        // tags
        updateJavadocTags(sb, originalContent, entity, indent, isJavaExecutable);

        sb = new StringBuilder(removeLastEmptyJavadocLines(sb.toString())).append(EOL);

        sb.append(indent).append(" ").append(END_JAVADOC);
        sb.append(EOL);

        stringWriter.write(sb.toString());
    }

    /**
     * @param sb              not null
     * @param originalContent not null
     * @param entity          not null
     * @param indent          not null
     * @throws IOException if any
     */
    private void updateJavadocComment(
            final StringBuilder sb,
            final String originalContent,
            final JavaAnnotatedElement entity,
            final String indent)
            throws IOException {
        String comment = getJavadocComment(originalContent, entity);
        comment = removeLastEmptyJavadocLines(comment);
        comment = alignIndentationJavadocLines(comment, indent);

        if (comment.contains(START_JAVADOC)) {
            comment = comment.substring(comment.indexOf(START_JAVADOC) + START_JAVADOC.length());
            comment = indent + SEPARATOR_JAVADOC + comment.trim();
        }
        if (comment.contains(END_JAVADOC)) {
            comment = comment.substring(0, comment.indexOf(END_JAVADOC));
        }

        if (fixTag(LINK_TAG)) {
            comment = replaceLinkTags(comment, entity);
        }

        String[] lines = getLines(comment);
        for (String line : lines) {
            sb.append(indent).append(" ").append(line.trim());
            sb.append(EOL);
        }
    }

    private static final Pattern REPLACE_LINK_TAGS_PATTERN = Pattern.compile("\\{@link\\s");

    static String replaceLinkTags(String comment, JavaAnnotatedElement entity) {
        StringBuilder resolvedComment = new StringBuilder();
        // scan comment for {@link someClassName} and try to resolve this
        Matcher linktagMatcher = REPLACE_LINK_TAGS_PATTERN.matcher(comment);
        int startIndex = 0;
        while (linktagMatcher.find()) {
            int startName = linktagMatcher.end();
            resolvedComment.append(comment, startIndex, startName);
            int endName = comment.indexOf("}", startName);
            if (endName >= 0) {
                String name;
                String link = comment.substring(startName, endName);
                int hashIndex = link.indexOf('#');
                if (hashIndex >= 0) {
                    name = link.substring(0, hashIndex);
                } else {
                    name = link;
                }
                if (StringUtils.isNotBlank(name)) {
                    String typeName;
                    if (entity instanceof JavaClass) {
                        JavaClass clazz = (JavaClass) entity;
                        typeName = TypeResolver.byClassName(
                                        clazz.getBinaryName(),
                                        clazz.getJavaClassLibrary(),
                                        clazz.getSource().getImports())
                                .resolveType(name.trim());
                    } else if (entity instanceof JavaMember) {
                        JavaClass clazz = ((JavaMember) entity).getDeclaringClass();
                        typeName = TypeResolver.byClassName(
                                        clazz.getBinaryName(),
                                        clazz.getJavaClassLibrary(),
                                        clazz.getSource().getImports())
                                .resolveType(name.trim());
                    } else {
                        typeName = null;
                    }

                    if (typeName == null) {
                        typeName = name.trim();
                    } else {
                        typeName = typeName.replaceAll("\\$", ".");
                    }
                    // adjust name for inner classes
                    resolvedComment.append(typeName);
                }
                if (hashIndex >= 0) {
                    resolvedComment.append(link.substring(hashIndex).trim());
                }
                startIndex = endName;
            } else {
                startIndex = startName;
            }
        }
        resolvedComment.append(comment.substring(startIndex));
        return resolvedComment.toString();
    }

    /**
     * @param sb           not null
     * @param entity       not null
     * @param indent       not null
     * @param isJavaExecutable
     */
    private void addDefaultJavadocComment(
            final StringBuilder sb,
            final JavaAnnotatedElement entity,
            final String indent,
            final boolean isJavaExecutable) {
        sb.append(indent).append(SEPARATOR_JAVADOC);
        if (isJavaExecutable) {
            sb.append(getDefaultMethodJavadocComment((JavaExecutable) entity));
        } else {
            sb.append(getDefaultClassJavadocComment((JavaClass) entity));
        }
        sb.append(EOL);
    }

    /**
     * @param sb              not null
     * @param originalContent not null
     * @param entity          not null
     * @param indent          not null
     * @param isJavaExecutable
     * @throws IOException            if any
     * @throws MojoExecutionException if any
     */
    private void updateJavadocTags(
            final StringBuilder sb,
            final String originalContent,
            final JavaAnnotatedElement entity,
            final String indent,
            final boolean isJavaExecutable)
            throws IOException, MojoExecutionException {
        appendSeparator(sb, indent);

        // parse tags
        JavaEntityTags javaEntityTags = parseJavadocTags(originalContent, entity, indent, isJavaExecutable);

        // update and write tags
        updateJavadocTags(sb, entity, isJavaExecutable, javaEntityTags);

        // add missing tags...
        addMissingJavadocTags(sb, entity, indent, isJavaExecutable, javaEntityTags);
    }

    /**
     * Parse entity tags
     *
     * @param originalContent not null
     * @param entity          not null
     * @param indent          not null
     * @param isJavaMethod
     * @return an instance of {@link JavaEntityTags}
     * @throws IOException if any
     */
    JavaEntityTags parseJavadocTags(
            final String originalContent,
            final JavaAnnotatedElement entity,
            final String indent,
            final boolean isJavaMethod)
            throws IOException {
        JavaEntityTags javaEntityTags = new JavaEntityTags(entity, isJavaMethod);
        for (DocletTag docletTag : entity.getTags()) {
            String originalJavadocTag = getJavadocComment(originalContent, entity, docletTag);
            originalJavadocTag = removeLastEmptyJavadocLines(originalJavadocTag);
            originalJavadocTag = alignIndentationJavadocLines(originalJavadocTag, indent);

            javaEntityTags.getNamesTags().add(docletTag.getName());

            if (isJavaMethod) {
                List<String> params = docletTag.getParameters();
                if (params.isEmpty()) {
                    continue;
                }

                String paramName = params.get(0);
                switch (docletTag.getName()) {
                    case PARAM_TAG:
                        javaEntityTags.putJavadocParamTag(paramName, docletTag.getValue(), originalJavadocTag);
                        break;
                    case RETURN_TAG:
                        javaEntityTags.setJavadocReturnTag(originalJavadocTag);
                        break;
                    case THROWS_TAG:
                        javaEntityTags.putJavadocThrowsTag(paramName, originalJavadocTag);
                        break;
                    default:
                        javaEntityTags.getUnknownTags().add(originalJavadocTag);
                        break;
                }
            } else {
                javaEntityTags.getUnknownTags().add(originalJavadocTag);
            }
        }

        return javaEntityTags;
    }

    /**
     * Write tags according javaEntityTags.
     *
     * @param sb             not null
     * @param entity         not null
     * @param isJavaExecutable
     * @param javaEntityTags not null
     */
    private void updateJavadocTags(
            final StringBuilder sb,
            final JavaAnnotatedElement entity,
            final boolean isJavaExecutable,
            final JavaEntityTags javaEntityTags) {
        for (DocletTag docletTag : entity.getTags()) {
            if (isJavaExecutable) {
                JavaExecutable javaExecutable = (JavaExecutable) entity;

                List<String> params = docletTag.getParameters();
                if (params.isEmpty()) {
                    continue;
                }

                if (docletTag.getName().equals(PARAM_TAG)) {
                    writeParamTag(sb, javaExecutable, javaEntityTags, params.get(0), docletTag.getValue());
                } else if (docletTag.getName().equals(RETURN_TAG) && javaExecutable instanceof JavaMethod) {
                    writeReturnTag(sb, (JavaMethod) javaExecutable, javaEntityTags);
                } else if (docletTag.getName().equals(THROWS_TAG)) {
                    writeThrowsTag(sb, javaExecutable, javaEntityTags, params);
                } else {
                    // write unknown tags
                    for (Iterator<String> it = javaEntityTags.getUnknownTags().iterator(); it.hasNext(); ) {
                        String originalJavadocTag = it.next();
                        String simplified = StringUtils.removeDuplicateWhitespace(originalJavadocTag)
                                .trim();

                        if (simplified.contains("@" + docletTag.getName())) {
                            it.remove();
                            sb.append(originalJavadocTag);
                            sb.append(EOL);
                        }
                    }
                }
            } else {
                for (Iterator<String> it = javaEntityTags.getUnknownTags().iterator(); it.hasNext(); ) {
                    String originalJavadocTag = it.next();
                    String simplified = StringUtils.removeDuplicateWhitespace(originalJavadocTag)
                            .trim();

                    if (simplified.contains("@" + docletTag.getName())) {
                        it.remove();
                        sb.append(originalJavadocTag);
                        sb.append(EOL);
                    }
                }
            }

            if (sb.toString().endsWith(EOL)) {
                sb.delete(sb.toString().lastIndexOf(EOL), sb.toString().length());
            }

            sb.append(EOL);
        }
    }

    private void writeParamTag(
            final StringBuilder sb,
            final JavaExecutable javaExecutable,
            final JavaEntityTags javaEntityTags,
            String paramName,
            String paramValue) {
        if (!fixTag(PARAM_TAG)) {
            // write original param tag if found
            String originalJavadocTag = javaEntityTags.getJavadocParamTag(paramValue);
            if (originalJavadocTag != null) {
                sb.append(originalJavadocTag);
            }
            return;
        }

        boolean found = false;
        JavaParameter javaParam = javaExecutable.getParameterByName(paramName);
        if (javaParam == null) {
            // is generic?
            List<JavaTypeVariable<JavaGenericDeclaration>> typeParams = javaExecutable.getTypeParameters();
            for (JavaTypeVariable<JavaGenericDeclaration> typeParam : typeParams) {
                if (("<" + typeParam.getName() + ">").equals(paramName)) {
                    found = true;
                }
            }
        } else {
            found = true;
        }

        if (!found) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("Fixed unknown param '" + paramName + "' defined in "
                        + getJavaMethodAsString(javaExecutable));
            }

            if (sb.toString().endsWith(EOL)) {
                sb.delete(sb.toString().lastIndexOf(EOL), sb.toString().length());
            }
        } else {
            String originalJavadocTag = javaEntityTags.getJavadocParamTag(paramValue);
            if (originalJavadocTag != null) {
                sb.append(originalJavadocTag);
                String s = "@" + PARAM_TAG + " " + paramName;
                if (StringUtils.removeDuplicateWhitespace(originalJavadocTag)
                        .trim()
                        .endsWith(s)) {
                    sb.append(" ");
                    sb.append(getDefaultJavadocForType(javaParam.getJavaClass()));
                }
            }
        }
    }

    private void writeReturnTag(
            final StringBuilder sb, final JavaMethod javaMethod, final JavaEntityTags javaEntityTags) {
        String originalJavadocTag = javaEntityTags.getJavadocReturnTag();
        if (originalJavadocTag == null) {
            return;
        }

        if (!fixTag(RETURN_TAG)) {
            // write original param tag if found
            sb.append(originalJavadocTag);
            return;
        }

        if ((originalJavadocTag != null && !originalJavadocTag.isEmpty())
                && javaMethod.getReturns() != null
                && !javaMethod.getReturns().isVoid()) {
            sb.append(originalJavadocTag);
            if (originalJavadocTag.trim().endsWith("@" + RETURN_TAG)) {
                sb.append(" ");
                sb.append(getDefaultJavadocForType(javaMethod.getReturns()));
            }
        }
    }

    void writeThrowsTag(
            final StringBuilder sb,
            final JavaExecutable javaExecutable,
            final JavaEntityTags javaEntityTags,
            final List<String> params) {
        String exceptionClassName = params.get(0);

        String originalJavadocTag = javaEntityTags.getJavadocThrowsTag(exceptionClassName);
        if (originalJavadocTag == null) {
            return;
        }

        if (!fixTag(THROWS_TAG)) {
            // write original param tag if found
            sb.append(originalJavadocTag);
            return;
        }

        if (javaExecutable.getExceptions() != null) {
            for (JavaType exception : javaExecutable.getExceptions()) {
                if (exception.getFullyQualifiedName().endsWith(exceptionClassName)) {
                    originalJavadocTag = StringUtils.replace(
                            originalJavadocTag, exceptionClassName, exception.getFullyQualifiedName());
                    if (StringUtils.removeDuplicateWhitespace(originalJavadocTag)
                            .trim()
                            .endsWith("@" + THROWS_TAG + " " + exception.getValue())) {
                        originalJavadocTag += " if any.";
                    }

                    sb.append(originalJavadocTag);

                    // added qualified name
                    javaEntityTags.putJavadocThrowsTag(exception.getValue(), originalJavadocTag);

                    return;
                }
            }
        }

        Class<?> clazz = getClass(javaExecutable.getDeclaringClass(), exceptionClassName);

        if (clazz != null) {
            if (RuntimeException.class.isAssignableFrom(clazz)) {
                sb.append(StringUtils.replace(originalJavadocTag, exceptionClassName, clazz.getName()));

                // added qualified name
                javaEntityTags.putJavadocThrowsTag(clazz.getName(), originalJavadocTag);
            } else if (Throwable.class.isAssignableFrom(clazz)) {
                getLog().debug("Removing '" + originalJavadocTag + "'; Throwable not specified by "
                        + getJavaMethodAsString(javaExecutable) + " and it is not a RuntimeException.");
            } else {
                getLog().debug("Removing '" + originalJavadocTag + "'; It is not a Throwable");
            }
        } else if (removeUnknownThrows) {
            getLog().warn("Ignoring unknown throws '" + exceptionClassName + "' defined on "
                    + getJavaMethodAsString(javaExecutable));
        } else {
            getLog().warn("Found unknown throws '" + exceptionClassName + "' defined on "
                    + getJavaMethodAsString(javaExecutable));

            sb.append(originalJavadocTag);

            if (params.size() == 1) {
                sb.append(" if any.");
            }

            javaEntityTags.putJavadocThrowsTag(exceptionClassName, originalJavadocTag);
        }
    }

    /**
     * Add missing tags not already written.
     *
     * @param sb             not null
     * @param entity         not null
     * @param indent         not null
     * @param isJavaExecutable
     * @param javaEntityTags not null
     * @throws MojoExecutionException if any
     */
    private void addMissingJavadocTags(
            final StringBuilder sb,
            final JavaAnnotatedElement entity,
            final String indent,
            final boolean isJavaExecutable,
            final JavaEntityTags javaEntityTags)
            throws MojoExecutionException {
        if (isJavaExecutable) {
            JavaExecutable javaExecutable = (JavaExecutable) entity;

            if (fixTag(PARAM_TAG)) {
                if (javaExecutable.getParameters() != null) {
                    for (JavaParameter javaParameter : javaExecutable.getParameters()) {
                        if (!javaEntityTags.hasJavadocParamTag(javaParameter.getName())) {
                            appendDefaultParamTag(sb, indent, javaParameter);
                        }
                    }
                }
                // is generic?
                if (javaExecutable.getTypeParameters() != null) {
                    for (JavaTypeVariable<JavaGenericDeclaration> typeParam : javaExecutable.getTypeParameters()) {
                        if (!javaEntityTags.hasJavadocParamTag("<" + typeParam.getName() + ">")) {
                            appendDefaultParamTag(sb, indent, typeParam);
                        }
                    }
                }
            }

            if (javaExecutable instanceof JavaMethod) {
                JavaMethod javaMethod = (JavaMethod) javaExecutable;
                if (fixTag(RETURN_TAG)
                        && StringUtils.isEmpty(javaEntityTags.getJavadocReturnTag())
                        && javaMethod.getReturns() != null
                        && !javaMethod.getReturns().isVoid()) {
                    appendDefaultReturnTag(sb, indent, javaMethod);
                }
            }

            if (fixTag(THROWS_TAG) && javaExecutable.getExceptions() != null) {
                for (JavaType exception : javaExecutable.getExceptions()) {
                    if (javaEntityTags.getJavadocThrowsTag(exception.getValue(), true) == null) {
                        appendDefaultThrowsTag(sb, indent, exception);
                    }
                }
            }
        } else {
            if (!javaEntityTags.getNamesTags().contains(AUTHOR_TAG)) {
                appendDefaultAuthorTag(sb, indent);
            }
            if (!javaEntityTags.getNamesTags().contains(VERSION_TAG)) {
                appendDefaultVersionTag(sb, indent);
            }
        }

        if (fixTag(SINCE_TAG) && !javaEntityTags.getNamesTags().contains(SINCE_TAG)) {
            if (!isJavaExecutable) {
                if (!ignoreClirr) {
                    if (isNewClassFromLastVersion((JavaClass) entity)) {
                        appendDefaultSinceTag(sb, indent);
                    }
                } else {
                    appendDefaultSinceTag(sb, indent);
                    addSinceClasses((JavaClass) entity);
                }
            } else {
                if (!ignoreClirr) {
                    if (isNewMethodFromLastRevision((JavaExecutable) entity)) {
                        appendDefaultSinceTag(sb, indent);
                    }
                } else if (sinceClasses != null) {
                    if (entity instanceof JavaMember
                            && !sinceClassesContains(((JavaMember) entity).getDeclaringClass())) {
                        appendDefaultSinceTag(sb, indent);
                    } else if (entity instanceof JavaClass
                            && !sinceClassesContains(((JavaClass) entity).getDeclaringClass())) {
                        appendDefaultSinceTag(sb, indent);
                    }
                }
            }
        }
    }

    /**
     * @param sb     not null
     * @param indent not null
     */
    private void appendDefaultAuthorTag(final StringBuilder sb, final String indent) {
        if (!fixTag(AUTHOR_TAG)) {
            return;
        }

        sb.append(indent).append(" * @").append(AUTHOR_TAG).append(" ");
        sb.append(defaultAuthor);
        sb.append(EOL);
    }

    /**
     * @param sb             not null
     * @param indent         not null
     * @param separatorAdded
     * @return true if separator has been added.
     */
    private boolean appendDefaultSinceTag(final StringBuilder sb, final String indent, boolean separatorAdded) {
        if (!fixTag(SINCE_TAG)) {
            return separatorAdded;
        }

        if (!separatorAdded) {
            appendSeparator(sb, indent);
            separatorAdded = true;
        }

        appendDefaultSinceTag(sb, indent);
        return separatorAdded;
    }

    /**
     * @param sb     not null
     * @param indent not null
     */
    private void appendDefaultSinceTag(final StringBuilder sb, final String indent) {
        if (!fixTag(SINCE_TAG)) {
            return;
        }

        sb.append(indent).append(" * @").append(SINCE_TAG).append(" ");
        sb.append(defaultSince);
        sb.append(EOL);
    }

    /**
     * @param sb     not null
     * @param indent not null
     */
    private void appendDefaultVersionTag(final StringBuilder sb, final String indent) {
        if (!fixTag(VERSION_TAG) || StringUtils.isEmpty(defaultVersion)) {
            return;
        }

        sb.append(indent).append(" * @").append(VERSION_TAG).append(" ");
        sb.append(defaultVersion);
        sb.append(EOL);
    }

    /**
     * @param sb             not null
     * @param indent         not null
     * @param separatorAdded
     * @param typeParam  not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultParamTag(
            final StringBuilder sb, final String indent, boolean separatorAdded, final JavaParameter typeParam) {
        if (!fixTag(PARAM_TAG)) {
            return separatorAdded;
        }

        if (!separatorAdded) {
            appendSeparator(sb, indent);
            separatorAdded = true;
        }

        appendDefaultParamTag(sb, indent, typeParam);
        return separatorAdded;
    }

    /**
     * @param sb             not null
     * @param indent         not null
     * @param separatorAdded
     * @param typeParameter  not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultParamTag(
            final StringBuilder sb,
            final String indent,
            boolean separatorAdded,
            final JavaTypeVariable<JavaGenericDeclaration> typeParameter) {
        if (!fixTag(PARAM_TAG)) {
            return separatorAdded;
        }

        if (!separatorAdded) {
            appendSeparator(sb, indent);
            separatorAdded = true;
        }

        appendDefaultParamTag(sb, indent, typeParameter);
        return separatorAdded;
    }

    /**
     * @param sb            not null
     * @param indent        not null
     * @param typeParam not null
     */
    private void appendDefaultParamTag(final StringBuilder sb, final String indent, final JavaParameter typeParam) {
        if (!fixTag(PARAM_TAG)) {
            return;
        }

        sb.append(indent).append(" * @").append(PARAM_TAG).append(" ");
        sb.append(typeParam.getName());
        sb.append(" ");
        sb.append(getDefaultJavadocForType(typeParam.getJavaClass()));
        sb.append(EOL);
    }

    /**
     * @param sb            not null
     * @param indent        not null
     * @param typeParameter not null
     */
    private void appendDefaultParamTag(
            final StringBuilder sb, final String indent, final JavaTypeVariable<JavaGenericDeclaration> typeParameter) {
        if (!fixTag(PARAM_TAG)) {
            return;
        }

        sb.append(indent).append(" * @").append(PARAM_TAG).append(" ");
        sb.append("<").append(typeParameter.getName()).append(">");
        sb.append(" ");
        sb.append(getDefaultJavadocForType(typeParameter));
        sb.append(EOL);
    }

    /**
     * @param sb             not null
     * @param indent         not null
     * @param separatorAdded
     * @param javaMethod     not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultReturnTag(
            final StringBuilder sb, final String indent, boolean separatorAdded, final JavaMethod javaMethod) {
        if (!fixTag(RETURN_TAG)) {
            return separatorAdded;
        }

        if (!separatorAdded) {
            appendSeparator(sb, indent);
            separatorAdded = true;
        }

        appendDefaultReturnTag(sb, indent, javaMethod);
        return separatorAdded;
    }

    /**
     * @param sb         not null
     * @param indent     not null
     * @param javaMethod not null
     */
    private void appendDefaultReturnTag(final StringBuilder sb, final String indent, final JavaMethod javaMethod) {
        if (!fixTag(RETURN_TAG)) {
            return;
        }

        sb.append(indent).append(" * @").append(RETURN_TAG).append(" ");
        sb.append(getDefaultJavadocForType(javaMethod.getReturns()));
        sb.append(EOL);
    }

    /**
     * @param sb             not null
     * @param indent         not null
     * @param separatorAdded
     * @param exception      not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultThrowsTag(
            final StringBuilder sb, final String indent, boolean separatorAdded, final JavaType exception) {
        if (!fixTag(THROWS_TAG)) {
            return separatorAdded;
        }

        if (!separatorAdded) {
            appendSeparator(sb, indent);
            separatorAdded = true;
        }

        appendDefaultThrowsTag(sb, indent, exception);
        return separatorAdded;
    }

    /**
     * @param sb        not null
     * @param indent    not null
     * @param exception not null
     */
    private void appendDefaultThrowsTag(final StringBuilder sb, final String indent, final JavaType exception) {
        if (!fixTag(THROWS_TAG)) {
            return;
        }

        sb.append(indent).append(" * @").append(THROWS_TAG).append(" ");
        sb.append(exception.getFullyQualifiedName());
        sb.append(" if any.");
        sb.append(EOL);
    }

    /**
     * @param sb     not null
     * @param indent not null
     */
    private void appendSeparator(final StringBuilder sb, final String indent) {
        sb.append(indent).append(" *");
        sb.append(EOL);
    }

    /**
     * Verify if a method has <code>&#64;java.lang.Override()</code> annotation or if it is an inherited method
     * from an interface or a super class. The goal is to handle <code>&#123;&#64;inheritDoc&#125;</code> tag.
     *
     * @param javaMethod not null
     * @return <code>true</code> if the method is inherited, <code>false</code> otherwise.
     * @throws MojoExecutionException if any
     */
    private boolean isInherited(JavaExecutable javaMethod) throws MojoExecutionException {
        if (javaMethod.getAnnotations() != null) {
            for (JavaAnnotation annotation : javaMethod.getAnnotations()) {
                if (annotation.toString().equals("@java.lang.Override()")) {
                    return true;
                }
            }
        }

        Class<?> clazz = getClass(javaMethod.getDeclaringClass().getFullyQualifiedName());

        List<Class<?>> interfaces = ClassUtils.getAllInterfaces(clazz);
        for (Class<?> intface : interfaces) {
            if (isInherited(intface, javaMethod)) {
                return true;
            }
        }

        List<Class<?>> classes = ClassUtils.getAllSuperclasses(clazz);
        for (Class<?> superClass : classes) {
            if (isInherited(superClass, javaMethod)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param clazz      the Java class object, not null
     * @param javaMethod the QDox JavaMethod object not null
     * @return <code>true</code> if <code>javaMethod</code> exists in the given <code>clazz</code>,
     *         <code>false</code> otherwise.
     * @see #isInherited(JavaExecutable)
     */
    private boolean isInherited(Class<?> clazz, JavaExecutable javaMethod) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(javaMethod.getName())) {
                continue;
            }

            if (method.getParameterTypes().length != javaMethod.getParameters().size()) {
                continue;
            }

            boolean found = false;
            int j = 0;
            for (Class<?> paramType : method.getParameterTypes()) {
                String name1 = paramType.getName();
                String name2 = javaMethod.getParameters().get(j++).getType().getFullyQualifiedName();
                found = name1.equals(name2); // TODO check algo, seems broken (only takes in account the last param)
            }

            return found;
        }

        return false;
    }

    /**
     * @param clazz
     * @return
     */
    private String getDefaultJavadocForType(JavaClass clazz) {
        StringBuilder sb = new StringBuilder();

        if (!JavaTypeVariable.class.isAssignableFrom(clazz.getClass()) && clazz.isPrimitive()) {
            if (clazz.isArray()) {
                sb.append("an array of ").append(clazz.getComponentType().getCanonicalName());
            } else {
                sb.append("a ").append(clazz.getCanonicalName());
            }
            return sb.toString();
        }

        StringBuilder javadocLink = new StringBuilder();
        try {
            getClass(clazz.getCanonicalName());

            javadocLink.append("{@link ");

            if (clazz.isArray()) {
                javadocLink.append(clazz.getComponentType().getCanonicalName());
            } else {
                javadocLink.append(clazz.getCanonicalName());
            }
            javadocLink.append("}");
        } catch (Exception e) {
            javadocLink.append(clazz.getValue());
        }

        if (clazz.isArray()) {
            sb.append("an array of ").append(javadocLink).append(" objects");
        } else {
            sb.append("a ").append(javadocLink).append(" object");
        }

        return sb.toString();
    }

    private String getDefaultJavadocForType(JavaTypeVariable<JavaGenericDeclaration> typeParameter) {
        return "a " + typeParameter.getName() + " class";
    }

    /**
     * Check under Clirr if this given class is newer from the last version.
     *
     * @param javaClass a given class not null
     * @return <code>true</code> if Clirr said that this class is added from the last version,
     *         <code>false</code> otherwise or if {@link #clirrNewClasses} is null.
     */
    private boolean isNewClassFromLastVersion(JavaClass javaClass) {
        return (clirrNewClasses != null) && clirrNewClasses.contains(javaClass.getFullyQualifiedName());
    }

    /**
     * Check under Clirr if this given method is newer from the last version.
     *
     * @param javaExecutable a given method not null
     * @return <code>true</code> if Clirr said that this method is added from the last version,
     *         <code>false</code> otherwise or if {@link #clirrNewMethods} is null.
     * @throws MojoExecutionException if any
     */
    private boolean isNewMethodFromLastRevision(JavaExecutable javaExecutable) throws MojoExecutionException {
        if (clirrNewMethods == null) {
            return false;
        }

        List<String> clirrMethods =
                clirrNewMethods.get(javaExecutable.getDeclaringClass().getFullyQualifiedName());
        if (clirrMethods == null) {
            return false;
        }

        for (String clirrMethod : clirrMethods) {
            // see net.sf.clirr.core.internal.checks.MethodSetCheck#getMethodId(JavaType clazz, Method method)
            String retrn = "";
            if (javaExecutable instanceof JavaMethod && ((JavaMethod) javaExecutable).getReturns() != null) {
                retrn = ((JavaMethod) javaExecutable).getReturns().getFullyQualifiedName();
            }
            StringBuilder params = new StringBuilder();
            for (JavaParameter parameter : javaExecutable.getParameters()) {
                if (params.length() > 0) {
                    params.append(", ");
                }
                params.append(parameter.getResolvedFullyQualifiedName());
            }
            if (clirrMethod.contains(retrn + " ")
                    && clirrMethod.contains(javaExecutable.getName() + "(")
                    && clirrMethod.contains("(" + params + ")")) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param className not null
     * @return the Class corresponding to the given class name using the project classloader.
     * @throws MojoExecutionException if class not found
     * @see ClassUtils#getClass(ClassLoader, String, boolean)
     * @see #getProjectClassLoader()
     */
    private Class<?> getClass(String className) throws MojoExecutionException {
        try {
            return ClassUtils.getClass(getProjectClassLoader(), className, false);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("ClassNotFoundException: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the Class object assignable for {@link RuntimeException} class and associated with the given
     * exception class name.
     *
     * @param currentClass       not null
     * @param exceptionClassName not null, an exception class name defined as:
     *                           <ul>
     *                           <li>exception class fully qualified</li>
     *                           <li>exception class in the same package</li>
     *                           <li>exception inner class</li>
     *                           <li>exception class in java.lang package</li>
     *                           </ul>
     * @return the class if found, otherwise {@code null}.
     * @see #getClass(String)
     */
    private Class<?> getClass(JavaClass currentClass, String exceptionClassName) {
        String[] potentialClassNames = new String[] {
            exceptionClassName,
            currentClass.getPackage().getName() + "." + exceptionClassName,
            currentClass.getPackage().getName() + "." + currentClass.getName() + "$" + exceptionClassName,
            "java.lang." + exceptionClassName
        };

        Class<?> clazz = null;
        for (String potentialClassName : potentialClassNames) {
            try {
                clazz = getClass(potentialClassName);
            } catch (MojoExecutionException e) {
                // nop
            }
            if (clazz != null) {
                return clazz;
            }
        }

        return null;
    }

    /**
     * @param javaClass not null
     */
    private void addSinceClasses(JavaClass javaClass) {
        if (sinceClasses == null) {
            sinceClasses = new ArrayList<>();
        }
        sinceClasses.add(javaClass.getFullyQualifiedName());
    }

    private boolean sinceClassesContains(JavaClass javaClass) {
        return sinceClasses.contains(javaClass.getFullyQualifiedName());
    }

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

    /**
     * Write content into the given javaFile and using the given encoding.
     * All line separators will be unified.
     *
     * @param javaFile not null
     * @param encoding not null
     * @param content  not null
     * @throws IOException if any
     */
    private static void writeFile(final File javaFile, final String encoding, final String content) throws IOException {
        String unified = StringUtils.unifyLineSeparators(content);
        FileUtils.fileWrite(javaFile, encoding, unified);
    }

    /**
     * @return the full clirr goal, i.e. <code>groupId:artifactId:version:goal</code>. The clirr-plugin version
     *         could be load from the pom.properties in the clirr-maven-plugin dependency.
     */
    private static String getFullClirrGoal() {
        StringBuilder sb = new StringBuilder();

        sb.append(CLIRR_MAVEN_PLUGIN_GROUPID)
                .append(":")
                .append(CLIRR_MAVEN_PLUGIN_ARTIFACTID)
                .append(":");

        String clirrVersion = CLIRR_MAVEN_PLUGIN_VERSION;

        String resource = "META-INF/maven/" + CLIRR_MAVEN_PLUGIN_GROUPID + "/" + CLIRR_MAVEN_PLUGIN_ARTIFACTID
                + "/pom.properties";

        try (InputStream resourceAsStream =
                AbstractFixJavadocMojo.class.getClassLoader().getResourceAsStream(resource)) {

            if (resourceAsStream != null) {
                Properties properties = new Properties();
                properties.load(resourceAsStream);
                if (StringUtils.isNotEmpty(properties.getProperty("version"))) {
                    clirrVersion = properties.getProperty("version");
                }
            }
        } catch (IOException e) {
            // nop
        }

        sb.append(clirrVersion).append(":").append(CLIRR_MAVEN_PLUGIN_GOAL);

        return sb.toString();
    }

    /**
     * Default comment for class.
     *
     * @param javaClass not null
     * @return a default comment for class.
     */
    private static String getDefaultClassJavadocComment(final JavaClass javaClass) {
        StringBuilder sb = new StringBuilder();

        sb.append("<p>");
        if (javaClass.isAbstract()) {
            sb.append("Abstract ");
        }

        sb.append(javaClass.getName());

        if (!javaClass.isInterface()) {
            sb.append(" class.");
        } else {
            sb.append(" interface.");
        }

        sb.append("</p>");

        return sb.toString();
    }

    /**
     * Default comment for method with taking care of getter/setter in the javaMethod name.
     *
     * @param javaExecutable not null
     * @return a default comment for method
     */
    private static String getDefaultMethodJavadocComment(final JavaExecutable javaExecutable) {
        if (javaExecutable instanceof JavaConstructor) {
            return "<p>Constructor for " + javaExecutable.getName() + ".</p>";
        }

        if (javaExecutable.getName().length() > 3
                && (javaExecutable.getName().startsWith("get")
                        || javaExecutable.getName().startsWith("set"))) {
            String field =
                    StringUtils.lowercaseFirstLetter(javaExecutable.getName().substring(3));

            JavaClass clazz = javaExecutable.getDeclaringClass();

            if (clazz.getFieldByName(field) == null) {
                return "<p>" + javaExecutable.getName() + ".</p>";
            }

            StringBuilder sb = new StringBuilder();

            sb.append("<p>");
            if (javaExecutable.getName().startsWith("get")) {
                sb.append("Getter ");
            } else if (javaExecutable.getName().startsWith("set")) {
                sb.append("Setter ");
            }
            sb.append("for the field <code>").append(field).append("</code>.</p>");

            return sb.toString();
        }

        return "<p>" + javaExecutable.getName() + ".</p>";
    }

    /**
     * Try to find if a Javadoc comment has an {@link #INHERITED_TAG} for instance:
     * <pre>
     * &#47;&#42;&#42; {&#64;inheritDoc} &#42;&#47;
     * </pre>
     * or
     * <pre>
     * &#47;&#42;&#42;
     * &#32;&#42; {&#64;inheritDoc}
     * &#32;&#42;&#47;
     * </pre>
     *
     * @param content not null
     * @return <code>true</code> if the content has an inherited tag, <code>false</code> otherwise.
     */
    private static boolean hasInheritedTag(final String content) {
        final String inheritedTagPattern =
                "^\\s*(\\/\\*\\*)?(\\s*(\\*)?)*(\\{)@inheritDoc\\s*(\\})(\\s*(\\*)?)*(\\*\\/)?$";
        return Pattern.matches(inheritedTagPattern, StringUtils.removeDuplicateWhitespace(content));
    }

    /**
     * Workaround for QDOX-146 about whitespace.
     * Ideally we want to use <code>entity.getComment()</code>
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     * <p/>
     * <br/>
     * The return will be:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * </code>
     *
     * @param javaClassContent original class content not null
     * @param entity           not null
     * @return the javadoc comment for the entity without any tags.
     * @throws IOException if any
     */
    static String getJavadocComment(final String javaClassContent, final JavaAnnotatedElement entity)
            throws IOException {
        if (entity.getComment() == null) {
            return "";
        }

        String originalJavadoc = extractOriginalJavadocContent(javaClassContent, entity);

        StringBuilder sb = new StringBuilder();
        BufferedReader lr = new BufferedReader(new StringReader(originalJavadoc));
        String line;
        while ((line = lr.readLine()) != null) {
            String l = StringUtils.removeDuplicateWhitespace(line.trim());
            if (l.startsWith("* @") || l.startsWith("*@")) {
                break;
            }
            sb.append(line).append(EOL);
        }

        return trimRight(sb.toString());
    }

    /**
     * Work around for QDOX-146 about whitespace.
     * Ideally we want to use <code>docletTag.getValue()</code>
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     * <p/>
     * <br/>
     * The return will be:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * </code>
     *
     * @param javaClassContent original class content not null
     * @param entity           not null
     * @param docletTag        not null
     * @return the javadoc comment for the entity without Javadoc tags.
     * @throws IOException if any
     */
    String getJavadocComment(
            final String javaClassContent, final JavaAnnotatedElement entity, final DocletTag docletTag)
            throws IOException {
        if (docletTag.getValue() == null || docletTag.getParameters().isEmpty()) {
            return "";
        }

        String originalJavadoc = extractOriginalJavadocContent(javaClassContent, entity);

        StringBuilder sb = new StringBuilder();
        BufferedReader lr = new BufferedReader(new StringReader(originalJavadoc));
        String line;
        boolean found = false;

        // matching first line of doclettag
        Pattern p = Pattern.compile("(\\s*\\*\\s?@" + docletTag.getName() + ")\\s+" + "(\\Q"
                + docletTag.getValue().split("\r\n|\r|\n")[0] + "\\E)");

        while ((line = lr.readLine()) != null) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                if (fixTag(LINK_TAG)) {
                    line = replaceLinkTags(line, entity);
                }
                sb.append(line).append(EOL);
                found = true;
            } else {
                if (line.trim().startsWith("* @") || line.trim().startsWith("*@")) {
                    found = false;
                }
                if (found) {
                    if (fixTag(LINK_TAG)) {
                        line = replaceLinkTags(line, entity);
                    }
                    sb.append(line).append(EOL);
                }
            }
        }

        return trimRight(sb.toString());
    }

    /**
     * Extract the original Javadoc and others comments up to {@link #START_JAVADOC} form the entity. This method
     * takes care of the Javadoc indentation. All javadoc lines will be trimmed on right.
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     * <p/>
     * <br/>
     * The return will be:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * </code>
     *
     * @param javaClassContent not null
     * @param entity           not null
     * @return return the original javadoc as String for the current entity
     * @throws IOException if any
     */
    static String extractOriginalJavadoc(final String javaClassContent, final JavaAnnotatedElement entity)
            throws IOException {
        if (entity.getComment() == null) {
            return "";
        }

        String[] javaClassContentLines = getLines(javaClassContent);
        List<String> list = new LinkedList<>();
        for (int i = entity.getLineNumber() - 2; i >= 0; i--) {
            String line = javaClassContentLines[i];

            list.add(trimRight(line));
            if (line.trim().startsWith(START_JAVADOC)) {
                break;
            }
        }

        Collections.reverse(list);

        return StringUtils.join(list.iterator(), EOL);
    }

    /**
     * Extract the Javadoc comment between {@link #START_JAVADOC} and {@link #END_JAVADOC} form the entity. This method
     * takes care of the Javadoc indentation. All javadoc lines will be trimmed on right.
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     * <p/>
     * <br/>
     * The return will be:
     * <br/>
     * <p/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * </code>
     *
     * @param javaClassContent not null
     * @param entity           not null
     * @return return the original javadoc as String for the current entity
     * @throws IOException if any
     */
    static String extractOriginalJavadocContent(final String javaClassContent, final JavaAnnotatedElement entity)
            throws IOException {
        if (entity.getComment() == null) {
            return "";
        }

        String originalJavadoc = extractOriginalJavadoc(javaClassContent, entity);
        int index = originalJavadoc.indexOf(START_JAVADOC);
        if (index != -1) {
            originalJavadoc = originalJavadoc.substring(index + START_JAVADOC.length());
        }
        index = originalJavadoc.indexOf(END_JAVADOC);
        if (index != -1) {
            originalJavadoc = originalJavadoc.substring(0, index);
        }
        if (originalJavadoc.startsWith("\r\n")) {
            originalJavadoc = originalJavadoc.substring(2);
        } else if (originalJavadoc.startsWith("\n") || originalJavadoc.startsWith("\r")) {
            originalJavadoc = originalJavadoc.substring(1);
        }

        return trimRight(originalJavadoc);
    }

    /**
     * @param content not null
     * @return the content without last lines containing javadoc separator (ie <code> * </code>)
     * @throws IOException if any
     * @see #getJavadocComment(String, JavaAnnotatedElement, DocletTag)
     */
    private static String removeLastEmptyJavadocLines(final String content) throws IOException {
        if (!content.contains(EOL)) {
            return content;
        }

        String[] lines = getLines(content);
        if (lines.length == 1) {
            return content;
        }

        List<String> linesList = new LinkedList<>(Arrays.asList(lines));

        Collections.reverse(linesList);

        for (Iterator<String> it = linesList.iterator(); it.hasNext(); ) {
            String line = it.next();

            if (line.trim().equals("*")) {
                it.remove();
            } else {
                break;
            }
        }

        Collections.reverse(linesList);

        return StringUtils.join(linesList.iterator(), EOL);
    }

    /**
     * @param content not null
     * @return the javadoc comment with the given indentation
     * @throws IOException if any
     * @see #getJavadocComment(String, JavaAnnotatedElement, DocletTag)
     */
    private static String alignIndentationJavadocLines(final String content, final String indent) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : getLines(content)) {
            if (sb.length() > 0) {
                sb.append(EOL);
            }
            if (!line.trim().startsWith("*")) {
                line = "*" + line;
            }
            sb.append(indent).append(" ").append(trimLeft(line));
        }

        return sb.toString();
    }

    /**
     * Autodetect the indentation of a given line:
     * <pre>
     * autodetectIndentation( null ) = "";
     * autodetectIndentation( "a" ) = "";
     * autodetectIndentation( "    a" ) = "    ";
     * autodetectIndentation( "\ta" ) = "\t";
     * </pre>
     *
     * @param line not null
     * @return the indentation for the given line.
     */
    private static String autodetectIndentation(final String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }

        return line.substring(0, line.indexOf(trimLeft(line)));
    }

    /**
     * @param content not null
     * @return an array of all content lines
     * @throws IOException if any
     */
    private static String[] getLines(final String content) throws IOException {
        List<String> lines = new LinkedList<>();

        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            line = reader.readLine();
        }

        return lines.toArray(new String[lines.size()]);
    }

    /**
     * Trim a given line on the left:
     * <pre>
     * trimLeft( null ) = "";
     * trimLeft( "  " ) = "";
     * trimLeft( "a" ) = "a";
     * trimLeft( "    a" ) = "a";
     * trimLeft( "\ta" ) = "a";
     * trimLeft( "    a    " ) = "a    ";
     * </pre>
     *
     * @param text
     * @return the text trimmed on left side or empty if text is null.
     */
    private static String trimLeft(final String text) {
        if ((text == null || text.isEmpty()) || StringUtils.isEmpty(text.trim())) {
            return "";
        }

        String textTrimmed = text.trim();
        return text.substring(text.indexOf(textTrimmed));
    }

    /**
     * Trim a given line on the right:
     * <pre>
     * trimRight( null ) = "";
     * trimRight( "  " ) = "";
     * trimRight( "a" ) = "a";
     * trimRight( "a\t" ) = "a";
     * trimRight( "    a    " ) = "    a";
     * </pre>
     *
     * @param text
     * @return the text trimmed on tight side or empty if text is null.
     */
    private static String trimRight(final String text) {
        if ((text == null || text.isEmpty()) || StringUtils.isEmpty(text.trim())) {
            return "";
        }

        String textTrimmed = text.trim();
        return text.substring(0, text.indexOf(textTrimmed) + textTrimmed.length());
    }

    /**
     * Wrapper class for the entity's tags.
     */
    class JavaEntityTags {
        private final JavaAnnotatedElement entity;

        private final boolean isJavaMethod;

        /**
         * List of tag names.
         */
        private List<String> namesTags;

        /**
         * Map with java parameter as key and original Javadoc lines as values.
         */
        private Map<String, String> tagParams;

        private Set<String> documentedParams = new HashSet<>();

        /**
         * Original javadoc lines.
         */
        private String tagReturn;

        /**
         * Map with java throw as key and original Javadoc lines as values.
         */
        private Map<String, String> tagThrows;

        /**
         * Original javadoc lines for unknown tags.
         */
        private List<String> unknownsTags;

        JavaEntityTags(JavaAnnotatedElement entity, boolean isJavaMethod) {
            this.entity = entity;
            this.isJavaMethod = isJavaMethod;
            this.namesTags = new LinkedList<>();
            this.tagParams = new LinkedHashMap<>();
            this.tagThrows = new LinkedHashMap<>();
            this.unknownsTags = new LinkedList<>();
        }

        public List<String> getNamesTags() {
            return namesTags;
        }

        public String getJavadocReturnTag() {
            return tagReturn;
        }

        public void setJavadocReturnTag(String s) {
            tagReturn = s;
        }

        public List<String> getUnknownTags() {
            return unknownsTags;
        }

        public void putJavadocParamTag(String paramName, String paramValue, String originalJavadocTag) {
            documentedParams.add(paramName);
            tagParams.put(paramValue, originalJavadocTag);
        }

        public String getJavadocParamTag(String paramValue) {
            String originalJavadocTag = tagParams.get(paramValue);
            if (originalJavadocTag == null && getLog().isWarnEnabled()) {
                getLog().warn(getMessage(paramValue, "javaEntityTags.tagParams"));
            }
            return originalJavadocTag;
        }

        public boolean hasJavadocParamTag(String paramName) {
            return documentedParams.contains(paramName);
        }

        public void putJavadocThrowsTag(String paramName, String originalJavadocTag) {
            tagThrows.put(paramName, originalJavadocTag);
        }

        public String getJavadocThrowsTag(String paramName) {
            return getJavadocThrowsTag(paramName, false);
        }

        public String getJavadocThrowsTag(String paramName, boolean nullable) {
            String originalJavadocTag = tagThrows.get(paramName);
            if (!nullable && originalJavadocTag == null && getLog().isWarnEnabled()) {
                getLog().warn(getMessage(paramName, "javaEntityTags.tagThrows"));
            }

            return originalJavadocTag;
        }

        private String getMessage(String paramName, String mapName) {
            StringBuilder msg = new StringBuilder();
            msg.append("No param '")
                    .append(paramName)
                    .append("' key found in ")
                    .append(mapName)
                    .append(" for the entity: ");
            if (isJavaMethod) {
                JavaMethod javaMethod = (JavaMethod) entity;
                msg.append(getJavaMethodAsString(javaMethod));
            } else {
                JavaClass javaClass = (JavaClass) entity;
                msg.append(javaClass.getFullyQualifiedName());
            }

            return msg.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("namesTags=").append(namesTags).append("\n");
            sb.append("tagParams=").append(tagParams).append("\n");
            sb.append("tagReturn=").append(tagReturn).append("\n");
            sb.append("tagThrows=").append(tagThrows).append("\n");
            sb.append("unknownsTags=").append(unknownsTags).append("\n");

            return sb.toString();
        }
    }
}
