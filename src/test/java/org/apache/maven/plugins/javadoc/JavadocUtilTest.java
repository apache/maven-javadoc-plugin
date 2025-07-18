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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.plugins.javadoc.ProxyServer.AuthAsyncProxyServlet;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class JavadocUtilTest extends PlexusTestCase {

    public void testParseJavadocVersionNull() {
        try {
            JavadocUtil.extractJavadocVersion(null);
            fail("Not catch null");
        } catch (NullPointerException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    public void testParseJavadocVersionEmptyString() {
        try {
            JavadocUtil.extractJavadocVersion("");
            fail("Not catch empty version");
        } catch (IllegalArgumentException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    /**
     * Test the javadoc version parsing.
     */
    public void testParseJavadocVersion() {
        // Sun JDK 1.4
        String version = "java full version \"1.4.2_12-b03\"";
        assertEquals("1.4.2", JavadocUtil.extractJavadocVersion(version));

        // Sun JDK 1.5
        version = "java full version \"1.5.0_07-164\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        // IBM JDK 1.4
        version = "java full version \"J2RE 1.4.2 IBM Windows 32 build cn1420-20040626\"";
        assertEquals("1.4.2", JavadocUtil.extractJavadocVersion(version));

        // IBM JDK 1.5
        version = "javadoc version complète de \"J2RE 1.5.0 IBM Windows 32 build pwi32pdev-20070426a\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        // IBM JDK 1.5
        version =
                "J2RE 1.5.0 IBM Windows 32 build pwi32devifx-20070323 (ifix 117674: SR4 + 116644 + 114941 + 116110 + 114881)";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        // FreeBSD
        version = "java full version \"diablo-1.5.0-b01\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        // BEA
        version = "java full version \"1.5.0_11-b03\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        // Other tests
        version = "java full version \"1.5.0_07-164\"" + System.lineSeparator();
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        version = System.lineSeparator() + "java full version \"1.5.0_07-164\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        version = System.lineSeparator() + "java full version \"1.5.0_07-164\"" + System.lineSeparator();
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        version = "java full" + System.lineSeparator() + " version \"1.5.0_07-164\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        version = "java full version \"1.99.123-b01\"";
        assertEquals("1.99.123", JavadocUtil.extractJavadocVersion(version));

        version = "java full version \"1.5.0.07-164\"";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        version = "java full version \"1.4\"";
        assertEquals("1.4", JavadocUtil.extractJavadocVersion(version));

        version = "SCO-UNIX-J2SE-1.5.0_09*FCS-UW714-OSR6*_20061114";
        assertEquals("1.5.0", JavadocUtil.extractJavadocVersion(version));

        // Java 9 EA
        version = "java full version \"9-ea+113\"";
        assertEquals("9", JavadocUtil.extractJavadocVersion(version));

        // Java 9 EA Jigsaw
        version = "java full version \"9-ea+113-2016-04-14-161743.javare.4852.nc\"";
        assertEquals("9", JavadocUtil.extractJavadocVersion(version));

        version = "java version \"9-ea\"";
        assertEquals("9", JavadocUtil.extractJavadocVersion(version));

        // JEP 223 example for future versions
        version = "java full version \"9+100\"";
        assertEquals("9", JavadocUtil.extractJavadocVersion(version));

        version = "java full version \"9.0.1+20\"";
        assertEquals("9.0.1", JavadocUtil.extractJavadocVersion(version));

        version = "java full version \"10+100\"";
        assertEquals("10", JavadocUtil.extractJavadocVersion(version));

        version = "java full version \"10.0.1+20\"";
        assertEquals("10.0.1", JavadocUtil.extractJavadocVersion(version));
    }

    public void testParseJavadocMemoryNull() {
        try {
            JavadocUtil.parseJavadocMemory(null);
            fail("Not catch null");
        } catch (NullPointerException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    public void testParseJavadocMemoryEmpty() {
        try {
            JavadocUtil.parseJavadocMemory("");
            fail("Not catch null");
        } catch (IllegalArgumentException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    /**
     * Method to test the javadoc memory parsing.
     */
    public void testParseJavadocMemory() {
        String memory = "128";
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));

        memory = "128k";
        assertEquals("128k", JavadocUtil.parseJavadocMemory(memory));
        memory = "128kb";
        assertEquals("128k", JavadocUtil.parseJavadocMemory(memory));

        memory = "128m";
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));
        memory = "128mb";
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));

        memory = "1g";
        assertEquals("1024m", JavadocUtil.parseJavadocMemory(memory));
        memory = "1gb";
        assertEquals("1024m", JavadocUtil.parseJavadocMemory(memory));

        memory = "1t";
        assertEquals("1048576m", JavadocUtil.parseJavadocMemory(memory));
        memory = "1tb";
        assertEquals("1048576m", JavadocUtil.parseJavadocMemory(memory));

        memory = System.lineSeparator() + "128m";
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));
        memory = System.lineSeparator() + "128m" + System.lineSeparator();
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));

        memory = "     128m";
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));
        memory = "     128m     ";
        assertEquals("128m", JavadocUtil.parseJavadocMemory(memory));

        memory = "1m28m";
        try {
            JavadocUtil.parseJavadocMemory(memory);
            fail("Not catch wrong pattern");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
        memory = "ABC128m";
        try {
            JavadocUtil.parseJavadocMemory(memory);
            fail("Not catch wrong pattern");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Method to test the validate encoding parsing.
     */
    public void testValidateEncoding() {
        assertFalse("Not catch null", JavadocUtil.validateEncoding(null));
        assertTrue("UTF-8 not supported on this platform", JavadocUtil.validateEncoding("UTF-8"));
        assertTrue("ISO-8859-1 not supported on this platform", JavadocUtil.validateEncoding("ISO-8859-1"));
        assertFalse("latin is supported on this platform???", JavadocUtil.validateEncoding("latin"));
        assertFalse("WRONG is supported on this platform???", JavadocUtil.validateEncoding("WRONG"));
    }

    /**
     * Method to test isValidPackageList()
     *
     * @throws Exception if any
     */
    public void testIsValidPackageList() throws Exception {
        Settings settings = null;
        Proxy proxy;

        URL url = null;
        URL wrongUrl;
        try {
            JavadocUtil.isValidPackageList(url, settings, false);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        url = new File(getBasedir(), "/pom.xml").toURI().toURL();
        assertTrue(JavadocUtil.isValidPackageList(url, settings, false));

        try {
            assertFalse(JavadocUtil.isValidPackageList(url, settings, true));
        } catch (IOException e) {
            assertNotNull(e.getMessage());
        }

        url = this.getClass()
                .getResource("/JavadocUtilTest-package-list.txt")
                .toURI()
                .toURL();
        assertTrue(JavadocUtil.isValidPackageList(url, settings, true));

        url = new URL("http://maven.apache.org/plugins-archives/maven-javadoc-plugin-3.5.0/apidocs/package-list");
        assertTrue(JavadocUtil.isValidPackageList(url, settings, true));

        wrongUrl = new URL("http://maven.apache.org/plugins/maven-javadoc-plugin/apidocs/package-list2");
        try {
            JavadocUtil.isValidPackageList(wrongUrl, settings, false);
            fail();
        } catch (IOException e) {
            assertNotNull(e.getMessage());
        }

        // real proxy
        AuthAsyncProxyServlet proxyServlet = new AuthAsyncProxyServlet();
        try (ProxyServer proxyServer = new ProxyServer(proxyServlet)) {
            proxyServer.start();
            settings = new Settings();
            assertTrue(JavadocUtil.isValidPackageList(url, settings, true));
            try {
                JavadocUtil.isValidPackageList(wrongUrl, settings, false);
                fail();
            } catch (IOException e) {
                assertNotNull(e.getMessage());
            }
        }

        Map<String, String> authentications = new HashMap<>();
        authentications.put("foo", "bar");
        // wrong auth
        proxyServlet = new AuthAsyncProxyServlet(authentications);
        try (ProxyServer proxyServer = new ProxyServer(proxyServlet)) {
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            settings.addProxy(proxy);

            JavadocUtil.isValidPackageList(url, settings, false);
            fail();
        } catch (FileNotFoundException e) {
            assertNotNull(e.getMessage());
        }

        // auth proxy
        proxyServlet = new AuthAsyncProxyServlet(authentications);
        try (ProxyServer proxyServer = new ProxyServer(proxyServlet)) {
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            proxy.setUsername("foo");
            proxy.setPassword("bar");
            settings.addProxy(proxy);

            assertTrue(JavadocUtil.isValidPackageList(url, settings, true));

            try {
                JavadocUtil.isValidPackageList(wrongUrl, settings, false);
                fail();
            } catch (IOException e) {
                assertNotNull(e.getMessage());
            }
        }

        // timeout
        proxyServlet = new AuthAsyncProxyServlet(authentications, 3000); // more than 2000, see fetchURL
        try (ProxyServer proxyServer = new ProxyServer(proxyServlet)) {
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            proxy.setUsername("foo");
            proxy.setPassword("bar");
            settings.addProxy(proxy);

            JavadocUtil.isValidPackageList(url, settings, true);
            fail();
        } catch (SocketTimeoutException e) {
            assertNotNull(e.getMessage());
        }

        // nonProxyHosts
        proxyServlet = new AuthAsyncProxyServlet(authentications);
        try (ProxyServer proxyServer = new ProxyServer(proxyServlet)) {
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive(true);
            proxy.setHost(proxyServer.getHostName());
            proxy.setPort(proxyServer.getPort());
            proxy.setProtocol("http");
            proxy.setUsername("foo");
            proxy.setPassword("bar");
            proxy.setNonProxyHosts("maven.apache.org");
            settings.addProxy(proxy);

            assertTrue(JavadocUtil.isValidPackageList(url, settings, true));
        }
    }

    public void testGetRedirectUrlNotHttp() throws Exception {
        URL url = new URI("ftp://some.where").toURL();
        assertEquals(
                url.toString(), JavadocUtil.getRedirectUrl(url, new Settings()).toString());

        url = new URI("file://some/where").toURL();
        assertEquals(
                url.toString(), JavadocUtil.getRedirectUrl(url, new Settings()).toString());
    }

    /**
     * Tests a redirect from localhost:port1 to localhost:port2
     */
    public void testGetRedirectUrl() throws Exception {
        Server server = null, redirectServer = null;
        try {
            redirectServer = new Server(0);
            redirectServer.setHandler(new AbstractHandler() {
                @Override
                public void handle(
                        String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                    response.setStatus(HttpServletResponse.SC_OK);
                    ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(100);
                    writer.write("<html>Hello world</html>");
                    writer.flush();
                    response.setContentLength(writer.size());
                    OutputStream out = response.getOutputStream();
                    writer.writeTo(out);
                    out.close();
                    writer.close();
                }
            });
            redirectServer.start();

            server = new Server(0);
            MovedContextHandler handler = new MovedContextHandler();
            int redirectPort = ((ServerConnector) redirectServer.getConnectors()[0]).getLocalPort();
            handler.setNewContextURL("http://localhost:" + redirectPort);
            server.setHandler(handler);
            server.start();

            URL url = new URI("http://localhost:"
                            + ((ServerConnector) redirectServer.getConnectors()[0]).getLocalPort())
                    .toURL();
            URL redirectUrl = JavadocUtil.getRedirectUrl(url, new Settings());

            assertTrue(redirectUrl.toString().startsWith("http://localhost:" + redirectPort));
        } finally {
            stopSilently(server);
            stopSilently(redirectServer);
        }
    }

    /**
     * Tests that getRedirectUrl returns the same URL when there are no redirects.
     */
    public void testGetRedirectUrlWithNoRedirects() throws Exception {
        Server server = null;
        try {
            server = new Server(0);
            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(
                        String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                    response.setStatus(HttpServletResponse.SC_OK);
                    ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(100);
                    writer.write("<html>Hello world</html>");
                    writer.flush();
                    response.setContentLength(writer.size());
                    OutputStream out = response.getOutputStream();
                    writer.writeTo(out);
                    out.close();
                    writer.close();
                }
            });
            server.start();

            URL url =
                    new URI("http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort()).toURL();
            URL redirectUrl = JavadocUtil.getRedirectUrl(url, new Settings());

            assertEquals(url.toURI(), redirectUrl.toURI());
        } finally {
            stopSilently(server);
        }
    }

    /**
     * Tests that getRedirectUrl adds an Accept header in HTTP requests. Necessary because some sites like Cloudflare
     * reject requests without an Accept header.
     */
    public void testGetRedirectUrlVerifyHeaders() throws Exception {
        Server server = null;
        try {
            server = new Server(0);
            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(
                        String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                        throws IOException {

                    if (request.getHeader("Accept") == null) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    } else {
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                    response.getOutputStream().close();
                }
            });
            server.start();

            URL url =
                    new URI("http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort()).toURL();
            JavadocUtil.getRedirectUrl(url, new Settings());
        } finally {
            stopSilently(server);
        }
    }

    /**
     * Method to test copyJavadocResources()
     *
     * @throws Exception if any
     */
    public void testCopyJavadocResources() throws Exception {
        File input = new File(getBasedir(), "src/test/resources/unit/docfiles-test/docfiles/");
        assertThat(input).exists();

        File output = new File(getBasedir(), "target/test/unit/docfiles-test/target/output");
        if (output.exists()) {
            FileUtils.deleteDirectory(output);
        }
        assertTrue(output.mkdirs());

        JavadocUtil.copyJavadocResources(output, input, null);

        assertThat(FileUtils.getFiles(output, null, null, false))
                .containsExactlyInAnyOrder(
                        Paths.get("test", "doc-files", "excluded-dir1", "sample-excluded1.gif")
                                .toFile(),
                        Paths.get("test", "doc-files", "excluded-dir2", "sample-excluded2.gif")
                                .toFile(),
                        Paths.get("test", "doc-files", "included-dir1", "sample-included1.gif")
                                .toFile(),
                        Paths.get("test", "doc-files", "included-dir2", "sample-included2.gif")
                                .toFile());

        assertThat(FileUtils.getDirectoryNames(new File(output, "test/doc-files"), null, null, false))
                .containsExactlyInAnyOrder("", "excluded-dir1", "excluded-dir2", "included-dir1", "included-dir2");

        input = new File(getBasedir(), "src/test/resources/unit/docfiles-test/docfiles/");
        assertTrue(input.exists());

        output = new File(getBasedir(), "target/test/unit/docfiles-test/target/output");
        if (output.exists()) {
            FileUtils.deleteDirectory(output);
        }
        assertTrue(output.mkdirs());

        JavadocUtil.copyJavadocResources(output, input, "excluded-dir1:excluded-dir2");

        assertThat(FileUtils.getFiles(output, null, null, false))
                .containsExactlyInAnyOrder(
                        Paths.get("test", "doc-files", "included-dir1", "sample-included1.gif")
                                .toFile(),
                        Paths.get("test", "doc-files", "included-dir2", "sample-included2.gif")
                                .toFile());

        assertThat(FileUtils.getDirectoryNames(new File(output, "test/doc-files"), null, null, false))
                .containsExactlyInAnyOrder("", "included-dir1", "included-dir2");
    }

    /**
     * Method to test pruneDirs()
     */
    public void testPruneDirs() {
        List<String> list = new ArrayList<>();
        String classesDir = getBasedir() + "/target/classes";
        list.add(classesDir);
        list.add(classesDir);
        list.add(classesDir);

        Set<Path> expected = Collections.singleton(Paths.get(classesDir));

        MavenProjectStub project = new MavenProjectStub();
        project.setFile(new File(getBasedir(), "pom.xml"));

        assertEquals(expected, JavadocUtil.pruneDirs(project, list));
    }

    /**
     * Method to test prunePaths()
     *
     */
    public void testPrunePaths() {
        List<String> list = new ArrayList<>();
        String classesDir = getBasedir() + "/target/classes";
        String tagletJar = getBasedir()
                + "/target/test-classes/unit/taglet-test/artifact-taglet/org/tullmann/taglets/1.0/taglets-1.0.jar";
        list.add(classesDir);
        list.add(classesDir);
        list.add(classesDir);
        list.add(tagletJar);
        list.add(tagletJar);
        list.add(tagletJar);

        Set<Path> expectedNoJar = Collections.singleton(Paths.get(classesDir));
        Set<Path> expectedWithJar = new HashSet<>(Arrays.asList(Paths.get(classesDir), Paths.get(tagletJar)));

        MavenProjectStub project = new MavenProjectStub();
        project.setFile(new File(getBasedir(), "pom.xml"));

        assertEquals(expectedNoJar, JavadocUtil.prunePaths(project, list, false));
        assertEquals(expectedWithJar, JavadocUtil.prunePaths(project, list, true));
    }

    /**
     * Method to test unifyPathSeparator()
     */
    public void testUnifyPathSeparator() {
        assertNull(JavadocUtil.unifyPathSeparator(null));

        final String ps = File.pathSeparator;

        // Windows
        String path1 = "C:\\maven-javadoc-plugin\\src\\main\\java";
        String path2 = "C:\\maven-javadoc-plugin\\src\\main\\javadoc";
        assertEquals(path1 + ps + path2, JavadocUtil.unifyPathSeparator(path1 + ";" + path2));
        assertEquals(path1 + ps + path2, JavadocUtil.unifyPathSeparator(path1 + ":" + path2));

        path1 = "C:/maven-javadoc-plugin/src/main/java";
        path2 = "C:/maven-javadoc-plugin/src/main/javadoc";
        assertEquals(path1 + ps + path2, JavadocUtil.unifyPathSeparator(path1 + ";" + path2));
        assertEquals(path1 + ps + path2, JavadocUtil.unifyPathSeparator(path1 + ":" + path2));
        assertEquals(
                path1 + ps + path2 + ps + path1 + ps + path2,
                JavadocUtil.unifyPathSeparator(path1 + ";" + path2 + ";" + path1 + ":" + path2));

        // Unix
        path1 = "/tmp/maven-javadoc-plugin/src/main/java";
        path2 = "/tmp/maven-javadoc-plugin/src/main/javadoc";
        assertEquals(path1 + ps + path2, JavadocUtil.unifyPathSeparator(path1 + ";" + path2));
        assertEquals(path1 + ps + path2, JavadocUtil.unifyPathSeparator(path1 + ":" + path2));
        assertEquals(
                path1 + ps + path2 + ps + path1 + ps + path2,
                JavadocUtil.unifyPathSeparator(path1 + ";" + path2 + ":" + path1 + ":" + path2));

        path1 = "/tmp/maven-javadoc-plugin/src/main/java;\n" + "/tmp/maven-javadoc-plugin/src/main/javadoc\n";
        assertEquals(
                "/tmp/maven-javadoc-plugin/src/main/java" + ps + "/tmp/maven-javadoc-plugin/src/main/javadoc",
                JavadocUtil.unifyPathSeparator(path1));
    }

    public void testGetIncludedFiles() {
        File sourceDirectory = new File("target/it").getAbsoluteFile();
        String[] fileList = new String[] {"Main.java"};
        Collection<String> excludePackages = Collections.singleton("*.it");

        List<String> includedFiles = JavadocUtil.getIncludedFiles(sourceDirectory, fileList, excludePackages);

        assertThat(includedFiles.toArray(new String[0])).isEqualTo(fileList);
    }

    private void stopSilently(Server server) {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    public void testQuotedArgument() {

        String value = "      org.apache.uima.analysis_component:\n      org.apache.uima.analysis_engine\n";

        String arg = JavadocUtil.quotedArgument(value);
        assertEquals("'org.apache.uima.analysis_component:org.apache.uima.analysis_engine'", arg);

        value = "org.apache.uima.analysis_component:org.apache.uima.analysis_engine";

        arg = JavadocUtil.quotedArgument(value);
        assertEquals("'org.apache.uima.analysis_component:org.apache.uima.analysis_engine'", arg);
    }

    public void testToList() {
        String value = "     *.internal:org.acme.exclude1.*:\n       org.acme.exclude2\n       ";
        List<String> values = JavadocUtil.toList(value);
        assertThat(values).containsExactly("*.internal", "org.acme.exclude1.*", "org.acme.exclude2");
    }
}
