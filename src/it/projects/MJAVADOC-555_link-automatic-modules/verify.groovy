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

def classFile
int javaVersion = System.getProperty( "java.specification.version" ) as Integer
if ( javaVersion >= 11 ) {
 classFile = new File( basedir, 'target/reports/apidocs/jul_to_slf4j/com/testcase/Testcase.html')
} else {
 classFile = new File( basedir, 'target/reports/apidocs/com/testcase/Testcase.html')
}
assert classFile.exists() : "Can't locate ${classFile}"

def p = /<a href="([^"]+)"(?:[^>]+)>Multimap<\/a>/

def m = classFile.text =~ p

assert m.hasGroup()
try {
  // https://bugs.openjdk.java.net/browse/JDK-8232438
  // As of Java 15 ?is-external=true is removed
  assert m[0][1].startsWith('https://guava.dev/releases/32.0.0-jre/api/docs/com/google/common/collect/Multimap.html')
}
catch(IndexOutOfBoundsException ioobe) {
  // seems to happen with some Java 11 releases...
  if ( javaVersion != 11 ) { throw ioobe }
}

