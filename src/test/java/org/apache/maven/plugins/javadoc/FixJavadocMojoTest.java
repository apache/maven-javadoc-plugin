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
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.plugins.javadoc.AbstractFixJavadocMojo.JavaEntityTags;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class FixJavadocMojoTest extends AbstractMojoTestCase {
    /** The vm line separator */
    private static final String EOL = System.getProperty("line.separator");

    /** flag to copy repo only one time */
    private static boolean TEST_REPO_CREATED = false;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        createTestRepo();
    }

    /**
     * Create test repository in target directory.
     *
     * @throws IOException if any
     */
    private void createTestRepo() throws Exception {
        if (TEST_REPO_CREATED) {
            return;
        }

        File localRepo = new File(getBasedir(), "target/local-repo/");
        localRepo.mkdirs();

        // ----------------------------------------------------------------------
        // fix-test-1.0.jar
        // ----------------------------------------------------------------------

        File sourceDir = new File(getBasedir(), "src/test/resources/unit/fix-test/repo/");
        assertTrue(sourceDir.exists());
        FileUtils.copyDirectoryStructure(sourceDir, localRepo);

        // Remove SCM files
        List<String> files = FileUtils.getFileAndDirectoryNames(
                localRepo, FileUtils.getDefaultExcludesAsString(), null, true, true, true, true);
        for (String filename : files) {
            File file = new File(filename);

            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                file.delete();
            }
        }

        TEST_REPO_CREATED = true;
    }

    /**
     * @throws Exception if any
     */
    public void testFix() throws Exception {
        File testPomBasedir = new File(getBasedir(), "target/test/unit/fix-test");

        executeMojoAndTest(testPomBasedir, new String[] {
            "ClassWithJavadoc.java",
            "ClassWithNoJavadoc.java",
            "InterfaceWithJavadoc.java",
            "InterfaceWithNoJavadoc.java"
        });
    }

    // ----------------------------------------------------------------------
    // Test private static methods
    // ----------------------------------------------------------------------

    /**
     * @throws Exception if any
     */
    public void testAutodetectIndentation() throws Exception {
        Method method = AbstractFixJavadocMojo.class.getDeclaredMethod("autodetectIndentation", String.class);
        method.setAccessible(true);

        String s = null;
        assertEquals("", (String) method.invoke(null, s));

        s = "no indentation";
        assertEquals("", (String) method.invoke(null, s));

        s = "no indentation with right spaces  ";
        assertEquals("", (String) method.invoke(null, s));

        s = "    indentation";
        assertEquals("    ", (String) method.invoke(null, s));

        s = "    indentation with right spaces  ";
        assertEquals("    ", (String) method.invoke(null, s));

        s = "\ttab indentation";
        assertEquals("\t", (String) method.invoke(null, s));

        s = "  \n  indentation with right spaces  ";
        assertEquals("  \n  ", (String) method.invoke(null, s));
    }

    /**
     * @throws Exception if any
     */
    public void testTrimLeft() throws Exception {
        Method method = AbstractFixJavadocMojo.class.getDeclaredMethod("trimLeft", String.class);
        method.setAccessible(true);

        assertEquals("", (String) method.invoke(null, (String) null));
        assertEquals("", (String) method.invoke(null, "  "));
        assertEquals("", (String) method.invoke(null, "  \t  "));
        assertEquals("a", (String) method.invoke(null, "a"));
        assertEquals("a", (String) method.invoke(null, "  a"));
        assertEquals("a", (String) method.invoke(null, "\ta"));
        assertEquals("a  ", (String) method.invoke(null, "  a  "));
        assertEquals("a\t", (String) method.invoke(null, "\ta\t"));
    }

    /**
     * @throws Exception if any
     */
    public void testTrimRight() throws Exception {
        Method method = AbstractFixJavadocMojo.class.getDeclaredMethod("trimRight", String.class);
        method.setAccessible(true);

        assertEquals("", (String) method.invoke(null, (String) null));
        assertEquals("", (String) method.invoke(null, "  "));
        assertEquals("", (String) method.invoke(null, "  \t  "));
        assertEquals("a", (String) method.invoke(null, "a"));
        assertEquals("a", (String) method.invoke(null, "a  "));
        assertEquals("a", (String) method.invoke(null, "a\t"));
        assertEquals("  a", (String) method.invoke(null, "  a  "));
        assertEquals("\ta", (String) method.invoke(null, "\ta\t"));
    }

    /**
     * @throws Exception if any
     */
    public void testHasInheritedTag() throws Exception {
        Method method = AbstractFixJavadocMojo.class.getDeclaredMethod("hasInheritedTag", String.class);
        method.setAccessible(true);

        String content = "/** {@inheritDoc} */";
        Boolean has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.TRUE, has);

        content = "/**{@inheritDoc}*/";
        has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.TRUE, has);

        content = "/**{@inheritDoc  }  */";
        has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.TRUE, has);

        content = "/**  {@inheritDoc  }  */";
        has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.TRUE, has);

        content = "/** */";
        has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.FALSE, has);

        content = "/**{  @inheritDoc  }*/";
        has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.FALSE, has);

        content = "/**{@ inheritDoc}*/";
        has = (Boolean) method.invoke(null, content);
        assertEquals(Boolean.FALSE, has);
    }

    /**
     * @throws Throwable if any
     */
    public void testJavadocComment() throws Throwable {
        String content = "/**" + EOL + " * Dummy Class."
                + EOL + " */"
                + EOL + "public class DummyClass"
                + EOL + "{"
                + EOL + "    /**"
                + EOL + "     *"
                + EOL + "     * Dummy"
                + EOL + "     *"
                + EOL + "     *      Method."
                + EOL + "     *"
                + EOL + "     * @param args not"
                + EOL + "     *"
                + EOL + "     * null"
                + EOL + "     * @param i non negative"
                + EOL + "     * @param object could"
                + EOL + "     * be"
                + EOL + "     *      null"
                + EOL + "     * @return a"
                + EOL + "     * String"
                + EOL + "     *"
                + EOL + "     * @throws Exception if"
                + EOL + "     * any"
                + EOL + "     *"
                + EOL + "     */"
                + EOL + "    public static String dummyMethod( String[] args, int i, Object object )"
                + EOL + "        throws Exception"
                + EOL + "    {"
                + EOL + "        return null;"
                + EOL + "    }"
                + EOL + "}";

        JavaProjectBuilder builder = new JavaProjectBuilder();
        builder.setEncoding("UTF-8");
        builder.addSource(new StringReader(content));

        JavaClass clazz = builder.addSource(new StringReader(content)).getClassByName("DummyClass");

        JavaMethod javaMethod = clazz.getMethods().get(0);

        String javadoc = AbstractFixJavadocMojo.extractOriginalJavadoc(content, javaMethod);
        assertEquals(
                "    /**" + EOL + "     *"
                        + EOL + "     * Dummy"
                        + EOL + "     *"
                        + EOL + "     *      Method."
                        + EOL + "     *"
                        + EOL + "     * @param args not"
                        + EOL + "     *"
                        + EOL + "     * null"
                        + EOL + "     * @param i non negative"
                        + EOL + "     * @param object could"
                        + EOL + "     * be"
                        + EOL + "     *      null"
                        + EOL + "     * @return a"
                        + EOL + "     * String"
                        + EOL + "     *"
                        + EOL + "     * @throws Exception if"
                        + EOL + "     * any"
                        + EOL + "     *"
                        + EOL + "     */",
                javadoc);

        String javadocContent = AbstractFixJavadocMojo.extractOriginalJavadocContent(content, javaMethod);
        assertEquals(
                "     *" + EOL + "     * Dummy"
                        + EOL + "     *"
                        + EOL + "     *      Method."
                        + EOL + "     *"
                        + EOL + "     * @param args not"
                        + EOL + "     *"
                        + EOL + "     * null"
                        + EOL + "     * @param i non negative"
                        + EOL + "     * @param object could"
                        + EOL + "     * be"
                        + EOL + "     *      null"
                        + EOL + "     * @return a"
                        + EOL + "     * String"
                        + EOL + "     *"
                        + EOL + "     * @throws Exception if"
                        + EOL + "     * any"
                        + EOL + "     *",
                javadocContent);

        Method method = AbstractFixJavadocMojo.class.getDeclaredMethod("removeLastEmptyJavadocLines", String.class);
        method.setAccessible(true);

        String withoutEmptyJavadocLines = (String) method.invoke(null, javadocContent);
        assertTrue(withoutEmptyJavadocLines.endsWith("any"));

        String methodJavadoc = AbstractFixJavadocMojo.getJavadocComment(content, javaMethod);
        assertEquals(
                "     *" + EOL + "     * Dummy" + EOL + "     *" + EOL + "     *      Method." + EOL + "     *",
                methodJavadoc);
        withoutEmptyJavadocLines = (String) method.invoke(null, methodJavadoc);
        assertTrue(withoutEmptyJavadocLines.endsWith("Method."));

        assertEquals(5, javaMethod.getTags().size());

        AbstractFixJavadocMojo mojoInstance = new FixJavadocMojo(null);
        setVariableValueToObject(mojoInstance, "fixTagsSplitted", new String[] {"all"});

        DocletTag tag = javaMethod.getTags().get(0);
        String tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @param args not" + EOL + "     *" + EOL + "     * null", tagJavadoc);
        withoutEmptyJavadocLines = (String) method.invoke(null, tagJavadoc);
        assertTrue(withoutEmptyJavadocLines.endsWith("null"));

        tag = javaMethod.getTags().get(1);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @param i non negative", tagJavadoc);
        withoutEmptyJavadocLines = (String) method.invoke(null, tagJavadoc);
        assertTrue(withoutEmptyJavadocLines.endsWith("negative"));

        tag = javaMethod.getTags().get(2);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @param object could" + EOL + "     * be" + EOL + "     *      null", tagJavadoc);
        withoutEmptyJavadocLines = (String) method.invoke(null, tagJavadoc);
        assertTrue(withoutEmptyJavadocLines.endsWith("null"));

        tag = javaMethod.getTags().get(3);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @return a" + EOL + "     * String" + EOL + "     *", tagJavadoc);
        withoutEmptyJavadocLines = (String) method.invoke(null, tagJavadoc);
        assertTrue(withoutEmptyJavadocLines.endsWith("String"));

        tag = javaMethod.getTags().get(4);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @throws Exception if" + EOL + "     * any" + EOL + "     *", tagJavadoc);
        withoutEmptyJavadocLines = (String) method.invoke(null, tagJavadoc);
        assertTrue(withoutEmptyJavadocLines.endsWith("any"));
    }

    public void testJavadocCommentJdk5() throws Exception {
        String content = "/**" + EOL + " * Dummy Class."
                + EOL + " */"
                + EOL + "public class DummyClass"
                + EOL + "{"
                + EOL + "    /**"
                + EOL + "     * Dummy method."
                + EOL + "     *"
                + EOL + "     * @param <K>  The Key type for the method"
                + EOL + "     * @param <V>  The Value type for the method"
                + EOL + "     * @param name The name."
                + EOL + "     * @return A map configured."
                + EOL + "     */"
                + EOL + "    public <K, V> java.util.Map<K, V> dummyMethod( String name )"
                + EOL + "    {"
                + EOL + "        return null;"
                + EOL + "    }"
                + EOL + "}";

        JavaProjectBuilder builder = new JavaProjectBuilder();
        builder.setEncoding("UTF-8");
        JavaClass clazz = builder.addSource(new StringReader(content)).getClassByName("DummyClass");

        JavaMethod javaMethod = clazz.getMethods().get(0);

        String methodJavadoc = AbstractFixJavadocMojo.getJavadocComment(content, javaMethod);
        assertEquals("     * Dummy method." + EOL + "     *", methodJavadoc);

        assertEquals(4, javaMethod.getTags().size());

        AbstractFixJavadocMojo mojoInstance = new FixJavadocMojo(null);
        setVariableValueToObject(mojoInstance, "fixTagsSplitted", new String[] {"all"});

        DocletTag tag = javaMethod.getTags().get(0);
        String tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @param <K>  The Key type for the method", tagJavadoc);

        tag = javaMethod.getTags().get(1);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @param <V>  The Value type for the method", tagJavadoc);

        tag = javaMethod.getTags().get(2);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @param name The name.", tagJavadoc);

        tag = javaMethod.getTags().get(3);
        tagJavadoc = mojoInstance.getJavadocComment(content, javaMethod, tag);
        assertEquals("     * @return A map configured.", tagJavadoc);
    }

    public void testInitParameters() throws Throwable {
        AbstractFixJavadocMojo mojoInstance = new FixJavadocMojo(null);
        setVariableValueToObject(mojoInstance, "fixTags", "author, version, since, param, return, throws, link");
        setVariableValueToObject(mojoInstance, "defaultSince", "1.0");
        setVariableValueToObject(mojoInstance, "level", "protected");

        invokeMethod(mojoInstance, true, "init");

        String[] fixTags = (String[]) getVariableValueFromObject(mojoInstance, "fixTagsSplitted");

        assertEquals("author", fixTags[0]);
        assertEquals("version", fixTags[1]);
        assertEquals("since", fixTags[2]);
        assertEquals("param", fixTags[3]);
        assertEquals("return", fixTags[4]);
        assertEquals("throws", fixTags[5]);
        assertEquals("link", fixTags[6]);
        assertEquals(7, fixTags.length);

        setVariableValueToObject(mojoInstance, "fixTags", "return, fake_value");
        invokeMethod(mojoInstance, true, "init");
        fixTags = (String[]) getVariableValueFromObject(mojoInstance, "fixTagsSplitted");

        assertEquals("return", fixTags[0]);
        assertEquals(1, fixTags.length);
    }

    public void testRemoveUnknownExceptions() throws Exception {
        AbstractFixJavadocMojo mojoInstance = new FixJavadocMojo(null);
        setVariableValueToObject(mojoInstance, "fixTagsSplitted", new String[] {"all"});
        setVariableValueToObject(mojoInstance, "project", new MavenProjectStub());

        String source = "package a.b.c;" + EOL
                + "public class Clazz {" + EOL
                + " /**" + EOL
                + " * @throws java.lang.RuntimeException" + EOL
                + " * @throws NumberFormatException" + EOL
                + " * @throws java.lang.Exception" + EOL // not thrown and no RTE -> remove
                + " * @throws com.foo.FatalException" + EOL // not on classpath (?!) -> see removeUnknownThrows
                + " */" + EOL
                + " public void method() {}" + EOL
                + "}";

        JavaProjectBuilder builder = new JavaProjectBuilder();
        JavaMethod javaMethod = builder.addSource(new StringReader(source))
                .getClassByName("Clazz")
                .getMethods()
                .get(0);

        JavaEntityTags javaEntityTags = mojoInstance.parseJavadocTags(source, javaMethod, "", true);

        StringBuilder sb = new StringBuilder();
        mojoInstance.writeThrowsTag(
                sb, javaMethod, javaEntityTags, Collections.singletonList("java.lang" + ".RuntimeException"));
        assertEquals(" * @throws java.lang.RuntimeException", sb.toString());

        sb = new StringBuilder();
        mojoInstance.writeThrowsTag(sb, javaMethod, javaEntityTags, Collections.singletonList("NumberFormatException"));
        assertEquals(" * @throws java.lang.NumberFormatException", sb.toString());

        sb = new StringBuilder();
        mojoInstance.writeThrowsTag(sb, javaMethod, javaEntityTags, Collections.singletonList("java.lang.Exception"));
        assertEquals("", sb.toString());

        setVariableValueToObject(mojoInstance, "removeUnknownThrows", true);
        sb = new StringBuilder();
        mojoInstance.writeThrowsTag(
                sb, javaMethod, javaEntityTags, Collections.singletonList("com.foo.FatalException"));
        assertEquals("", sb.toString());

        setVariableValueToObject(mojoInstance, "removeUnknownThrows", false);
        sb = new StringBuilder();
        mojoInstance.writeThrowsTag(
                sb, javaMethod, javaEntityTags, Collections.singletonList("com.foo.FatalException"));
        assertEquals(" * @throws com.foo.FatalException if any.", sb.toString());
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * @param testPomBasedir the basedir for the test project
     * @param clazzToCompare an array of the classes name to compare
     * @throws Exception if any
     */
    private void executeMojoAndTest(File testPomBasedir, String[] clazzToCompare) throws Exception {
        prepareTestProjects(testPomBasedir.getName());

        File testPom = new File(testPomBasedir, "pom.xml");
        assertTrue(testPom.getAbsolutePath() + " should exist", testPom.exists());

        FixJavadocMojo mojo = (FixJavadocMojo) lookupMojo("fix", testPom);
        assertNotNull(mojo);

        MavenSession session = newMavenSession(mojo.getProject());
        ((DefaultRepositorySystemSession) session.getRepositorySession())
                .setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                        .newInstance(
                                session.getRepositorySession(), new LocalRepository(new File("target/local-repo"))));
        setVariableValueToObject(mojo, "session", session);

        // compile the test project
        invokeCompileGoal(testPom, mojo.getLog());
        assertTrue(new File(testPomBasedir, "target/classes").exists());

        mojo.execute();

        File expectedDir = new File(testPomBasedir, "expected/src/main/java/fix/test");
        assertTrue(expectedDir.exists());

        File generatedDir = new File(testPomBasedir, "target/generated/fix/test");
        assertTrue(generatedDir.exists());

        for (String className : clazzToCompare) {
            assertEquals(new File(expectedDir, className), new File(generatedDir, className));
        }
    }

    /**
     * Invoke the compilation on the given pom file.
     *
     * @param testPom not null
     * @param log not null
     * @throws MavenInvocationException if any
     */
    private void invokeCompileGoal(File testPom, Log log) throws Exception {
        List<String> goals = new ArrayList<>();
        goals.add("clean");
        goals.add("compile");
        File invokerDir = new File(getBasedir(), "target/invoker");
        invokerDir.mkdirs();
        File invokerLogFile = FileUtils.createTempFile("FixJavadocMojoTest", ".txt", invokerDir);

        Properties properties = new Properties();

        JavadocUtil.invokeMaven(
                log,
                new File(getBasedir(), "target/local-repo"),
                testPom,
                goals,
                properties,
                invokerLogFile,
                null,
                null,
                null,
                null);
    }

    // ----------------------------------------------------------------------
    // static methods
    // ----------------------------------------------------------------------

    /**
     * Asserts that files are equal. If they are not an AssertionFailedError is thrown.
     *
     * @throws IOException if any
     */
    private static void assertEquals(File expected, File actual) throws Exception {
        assertTrue(" Expected file DNE: " + expected, expected.exists());
        String expectedContent = StringUtils.unifyLineSeparators(readFile(expected));

        assertTrue(" Actual file DNE: " + actual, actual.exists());
        String actualContent = StringUtils.unifyLineSeparators(readFile(actual));

        assertEquals(
                "Expected file: " + expected.getAbsolutePath() + ", actual file: " + actual.getAbsolutePath(),
                expectedContent,
                actualContent);
    }

    /**
     * @param testProjectDirName not null
     * @throws IOException if any
     */
    private static void prepareTestProjects(String testProjectDirName) throws Exception {
        File testPomBasedir = new File(getBasedir(), "target/test/unit/" + testProjectDirName);

        // Using unit test dir
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/" + testProjectDirName), testPomBasedir);
        List<String> scmFiles = FileUtils.getDirectoryNames(testPomBasedir, "**/.svn", null, true);
        for (String filename : scmFiles) {
            File dir = new File(filename);

            if (dir.isDirectory()) {
                FileUtils.deleteDirectory(dir);
            }
        }
    }

    /**
     * @param file not null
     * @return the content of the given file
     * @throws IOException if any
     */
    private static String readFile(File file) throws Exception {
        String content = FileUtils.fileRead(file, "UTF-8");
        return content;
    }
}
