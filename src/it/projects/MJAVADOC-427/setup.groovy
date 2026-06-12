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

// MJAVADOC-427: localhost mock HTTP server to deterministically test redirect-following
// without depending on slf4j.org reachability or any external TLS handshake.
// Replaces the prior live http://www.slf4j.org/apidocs round-trip that flaked on
// OpenJ9, stable JDK 25 LTS on Windows, and -- under CDN egress pressure --
// mainstream HotSpot distributions too (Temurin / Zulu on JDK 17 and 21).
//
// Coverage trade-off: the original IT implicitly tested the HTTP -> HTTPS
// transition (slf4j.org's URL is http://, the CDN redirects to https://).
// This rewrite intentionally drops that specific assertion. Reasons:
//   * The protocol switch happens inside the JDK's HttpURLConnection
//     implementation, not in maven-javadoc-plugin's code. The plugin
//     delegates redirect transport to the JDK and is protocol-agnostic at
//     its own layer.
//   * Reproducing the http -> https transition on a localhost mock requires
//     spinning up an HTTPS listener with a self-signed cert and patching
//     the IT's JVM truststore -- a lot of infrastructure for coverage of
//     code that is not the plugin's responsibility.
// What this IT still asserts end-to-end (via the hit-counter below): the
// plugin's link resolver
//   1. probes the configured -link URL (`/element-list` on the base),
//   2. follows the 301 Location header,
//   3. reads the redirected element-list and uses it to build cross-refs.
// That is the entirety of the plugin's redirect contract.

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

// Dynamic port allocation: bind to port 0 and let the OS pick a free one.
// Avoids any chance of collision with whatever else is running on the host
// (developer's local services on a fixed port, parallel CI processes,
// previous failed runs that left a daemon listener behind).
//
// maven-invoker-plugin runs each IT twice (once for the integration-test
// mojo, once for the verify mojo) in the same invoker JVM. The daemon
// worker pool keeps the first run's server alive into the second run, so
// we detect "already set up" via a marker in the IT's copied pom.xml
// (whose '__MJAVADOC_427_PORT__' placeholder is replaced with the chosen
// port at first setup) and skip silently on the second pass.
def pomFile = new File(basedir, 'pom.xml')
def pomContent = pomFile.text
if (!pomContent.contains('__MJAVADOC_427_PORT__')) {
    println "[MJAVADOC-427 mock] pom.xml already patched (re-invocation), reusing existing listener"
    return
}

HttpServer server = HttpServer.create(new InetSocketAddress(0), 0)
int port = server.address.port
server.executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
    Thread newThread(Runnable r) {
        Thread t = new Thread(r, "mjavadoc-427-mock-" + System.identityHashCode(r))
        t.daemon = true
        t
    }
})

// Hit log: each handler appends one line per request so verify.groovy can
// directly assert that both the base URL and the redirected URL were
// probed -- i.e. that the plugin actually followed the 301. This is a
// stronger signal than the original IT's "a link appears in the HTML",
// which only inferred redirect-following from absence-of-broken-link.
//
// Truncate at first setup: a stale file from a previous mvn invocation
// (rare under invoker because it cleans target/it/<name>/ on each run,
// but possible) could otherwise leak false-positive 'redirected' lines.
// The second invocation within the same mvn run returns early above, so
// it does not re-truncate.
def hitsFile = new File(basedir, 'redirect-hits.log')
if (hitsFile.exists()) hitsFile.delete()

// /element-list and /package-list at the root -> 301 redirect to /redirected/<same>
// Anything else at root -> also redirect, to exercise generic Location-following.
server.createContext("/", { exchange ->
    String path = exchange.requestURI.path
    hitsFile.append("base " + path + "\n")
    String target = path == "/" ? "/redirected/" : "/redirected" + path
    exchange.responseHeaders.add("Location", target)
    exchange.sendResponseHeaders(301, -1)
    exchange.close()
})

// /redirected/element-list and /redirected/package-list -> serve a minimal package list
// covering only org.slf4j, which is what App.java references via {@link LoggerFactory}.
server.createContext("/redirected/", { exchange ->
    String path = exchange.requestURI.path
    hitsFile.append("redirected " + path + "\n")
    if (path.endsWith("/element-list") || path.endsWith("/package-list")) {
        byte[] body = "org.slf4j\n".getBytes("UTF-8")
        exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
        exchange.sendResponseHeaders(200, body.length)
        exchange.responseBody.write(body)
        exchange.close()
    } else {
        // Other paths under /redirected/ are not requested by javadoc during element-list
        // resolution. Return 404 so we'd see a clear signal if javadoc starts probing more.
        exchange.sendResponseHeaders(404, -1)
        exchange.close()
    }
})

server.start()

// Patch the IT's pom.xml: replace the '__MJAVADOC_427_PORT__' placeholder
// with the chosen port. The placeholder format intentionally avoids the
// '@property@' syntax used by maven-invoker-plugin's own filter step
// (which runs at IT-copy time, before this script). Plain string replace
// is safe and visible.
pomFile.text = pomContent.replace('__MJAVADOC_427_PORT__', port.toString())
println "[MJAVADOC-427 mock] HTTP server started on dynamic port " + port + " (daemon); pom.xml patched"
