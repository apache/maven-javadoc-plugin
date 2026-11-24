
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

def options = new File( basedir, 'app/target/reports/apidocs/options' )
assert options.isFile()

def str = options.text
// dependency whose version has no dot
assert str.contains( 'maven-MJAVADOC620-jar-1-SNAPSHOT.jar' ) : 'Javadoc does not use the artifact with a dot-less version'
// dependency whose class lives in the top-level package
assert str.contains( 'maven-MJAVADOC620-toplevel-1.0-SNAPSHOT.jar' ) : 'Javadoc does not use the artifact with a top-level class'
