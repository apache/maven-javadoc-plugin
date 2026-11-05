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

def file = new File( basedir, 'target/reports/apidocs/mjavadoc427/App.html' );

assert file.exists()

// (1) Cross-reference resolves against the localhost mock — confirms the IT
//     does not depend on any external network. The original IT pointed at
//     slf4j.org, which flaked across multiple JVM/OS combinations whenever
//     CDN egress to slf4j.org was unreliable (see setup.groovy for the
//     full rationale, including the deliberate HTTP/HTTPS coverage trade-off).
assert file.text =~ /Link to slf4j <a href="http:\/\/localhost:[0-9]+\/[^"]*".*?><code>LoggerFactory<\/code><\/a>/
assert file.text =~ /public.*?<a href="http:\/\/localhost:[0-9]+\/[^"]*".*?>LoggerFactory<\/a>.*?getLoggerFactory.*?\(\)/

// (2) Hit log produced by setup.groovy directly proves the plugin's link
//     resolver followed the 301: both the base URL and the /redirected/
//     URL must have been requested. This is a stronger signal than the
//     original IT, which only inferred redirect-following from the
//     absence of a broken link.
def hitsFile = new File( basedir, 'redirect-hits.log' );
assert hitsFile.exists(), "redirect-hits.log not found — setup.groovy did not start the mock server"
def hits = hitsFile.readLines()
assert hits.any { it.startsWith('base ') }, \
    "javadoc never probed the base URL (no 'base ...' line in redirect-hits.log)"
assert hits.any { it.startsWith('redirected ') }, \
    "javadoc did not follow the 301 to /redirected/ (no 'redirected ...' line in redirect-hits.log)"

// (3) Defensive: no 'Error fetching link' warning. That string was the
//     canonical flake signature on CI when the plugin tried (and failed)
//     to reach slf4j.org. It must not reappear via a stray external URL.
def buildLog = new File( basedir, 'build.log' );
if ( buildLog.exists() ) {
    assert ! ( buildLog.text =~ /Error fetching link/ ), "Unexpected 'Error fetching link' warning in build.log"
}
