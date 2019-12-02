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

import java.util.jar.*

def aggregatedJavadoc = new File( basedir, 'target/mjavadoc613-1.0-SNAPSHOT-javadoc.jar');

assert aggregatedJavadoc.exists()
assert new File( basedir, 'b/c/d1/target/mjavadoc613d1-1.0-SNAPSHOT.jar').exists()
assert !(new File( basedir, 'b/c/d1/target/mjavadoc613d1-1.0-SNAPSHOT-javadoc.jar').exists())
assert new File( basedir, 'b/c/d2/target/mjavadoc613d2-1.0-SNAPSHOT.jar').exists()
assert !(new File( basedir, 'b/c/d2/target/mjavadoc613d2-1.0-SNAPSHOT-javadoc.jar').exists())
assert new File( basedir, 'b/e/target/mjavadoc613e-1.0-SNAPSHOT.jar').exists()
assert new File( basedir, 'b/e/target/mjavadoc613e-1.0-SNAPSHOT-javadoc.jar').exists()
assert new File( basedir, 'f/target/mjavadoc613f-1.0-SNAPSHOT.jar').exists()
assert !(new File( basedir, 'f/target/mjavadoc613f-1.0-SNAPSHOT-javadoc.jar').exists())

def jar = new JarFile(aggregatedJavadoc)

def files = new ArrayList()

for (JarEntry file in jar.entries()){
  files.add(file.getName())
}

assert files.contains("a/b/e/E.html")
assert !files.contains("a/b/c/d/D1.html")
assert !files.contains("a/b/c/d/D2.html")

