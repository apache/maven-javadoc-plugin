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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * A Proxy server.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.6
 */
class ProxyServer {
    private Server proxyServer;

    private ServerConnector serverConnector;

    /**
     * @param proxyServlet the wanted auth proxy servlet
     */
    public ProxyServer(AuthAsyncProxyServlet proxyServlet) {
        this(null, 0, proxyServlet);
    }

    /**
     * @param hostName the server name
     * @param port the server port
     * @param proxyServlet the wanted auth proxy servlet
     */
    public ProxyServer(String hostName, int port, AuthAsyncProxyServlet proxyServlet) {
        proxyServer = new Server();

        serverConnector = new ServerConnector(proxyServer);
        serverConnector.setHost(InetAddress.getLoopbackAddress().getHostName());
        serverConnector.setReuseAddress(true);
        serverConnector.setPort(0);

        proxyServer.addConnector(serverConnector);

        // Setup proxy handler to handle CONNECT methods
        ConnectHandler proxy = new ConnectHandler();
        proxyServer.setHandler(proxy);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler(proxy, "/", true, false);
        ServletHolder appServletHolder = new ServletHolder(proxyServlet);
        context.addServlet(appServletHolder, "/*");
    }

    /**
     * @return the host name
     */
    public String getHostName() {
        return serverConnector.getHost() == null
                ? InetAddress.getLoopbackAddress().getHostName()
                : serverConnector.getHost();
    }

    /**
     * @return the host port
     */
    public int getPort() {
        return serverConnector.getLocalPort();
    }

    /**
     * @throws Exception if any
     */
    public void start() throws Exception {
        if (proxyServer != null) {
            proxyServer.start();
        }
    }

    /**
     * @throws Exception if any
     */
    public void stop() throws Exception {
        if (proxyServer != null) {
            proxyServer.stop();
        }
        proxyServer = null;
    }

    /**
     * A proxy servlet with authentication support.
     */
    static class AuthAsyncProxyServlet extends AsyncProxyServlet {
        private Map<String, String> authentications;

        private long sleepTime = 0;

        /**
         * Constructor for non authentication servlet.
         */
        public AuthAsyncProxyServlet() {
            super();
        }

        /**
         * Constructor for authentication servlet.
         *
         * @param authentications a map of user/password
         */
        public AuthAsyncProxyServlet(Map<String, String> authentications) {
            this();

            this.authentications = authentications;
        }

        /**
         * Constructor for authentication servlet.
         *
         * @param authentications a map of user/password
         * @param sleepTime a positive time to sleep the service thread (for timeout)
         */
        public AuthAsyncProxyServlet(Map<String, String> authentications, long sleepTime) {
            this();
            this.authentications = authentications;
            this.sleepTime = sleepTime;
        }

        /** {@inheritDoc} */
        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            final HttpServletRequest request = (HttpServletRequest) req;
            final HttpServletResponse response = (HttpServletResponse) res;

            if (this.authentications != null && !this.authentications.isEmpty()) {
                String proxyAuthorization = request.getHeader("Proxy-Authorization");
                if (proxyAuthorization != null && proxyAuthorization.startsWith("Basic ")) {
                    String proxyAuth = proxyAuthorization.substring("Basic ".length());
                    String authorization = new String(Base64.getDecoder().decode(proxyAuth), StandardCharsets.UTF_8);

                    String[] authTokens = authorization.split(":");
                    String user = authTokens[0];
                    String password = authTokens[1];

                    if (this.authentications.get(user) == null) {
                        throw new IllegalArgumentException(user + " not found in the map!");
                    }

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            // nop
                        }
                    }
                    String authPass = this.authentications.get(user);
                    if (password.equals(authPass)) {
                        // could throw exceptions...
                        super.service(req, res);
                        return;
                    }
                }

                // Proxy-Authenticate Basic realm="CCProxy Authorization"
                response.addHeader("Proxy-Authenticate", "Basic realm=\"Jetty Proxy Authorization\"");
                response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                return;
            }

            super.service(req, res);
        }
    }
}
