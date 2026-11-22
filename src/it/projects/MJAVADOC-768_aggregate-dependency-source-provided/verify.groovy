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

// MJAVADOC-768: thing1/thing2 each have a module-info.java requiring jakarta.annotation statically
// (one via <optional>true</optional>, one via <scope>provided</scope>). doc aggregates their sources
// via includeDependencySources/dependencySourceIncludes rather than its own <modules>, so this only
// reproduces the bug if the aggregated jar actually gets built.

def javadocJar = new File(basedir, "doc/target/doc-1.0-SNAPSHOT-javadoc.jar")
assert javadocJar.exists()
