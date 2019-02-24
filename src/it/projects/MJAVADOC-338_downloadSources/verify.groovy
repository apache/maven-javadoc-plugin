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
 // NOOO never from central

def buildLog = new File(basedir,'build.log')

if ( mavenVersion ==~ /3\.[0-3]\..*/ )
{
  assert 1 == buildLog.readLines().count{it ==~ /\[INFO\] Downloading\: .+\/mjavadoc338-direct-1\.0-sources\.jar/} 
  assert 1 == buildLog.readLines().count{it ==~ /\[INFO\] Downloading\: .+\/mjavadoc338-direct-1\.0-javadoc-resources\.jar/} 
}
else
{
  assert 1 == buildLog.readLines().count{it ==~ /\[INFO\] Downloading from mrm-maven-plugin\: .+\/mjavadoc338-direct-1\.0-sources\.jar/} 
  assert 1 == buildLog.readLines().count{it ==~ /\[INFO\] Downloading from mrm-maven-plugin\: .+\/mjavadoc338-direct-1\.0-javadoc-resources\.jar/} 
}


assert !buildLog.readLines().any{it ==~ /\[INFO\] Downloading from .+\/mjavadoc338-transitive-1\.0-sources\.jar/} 
assert !buildLog.readLines().any{it ==~ /\[INFO\] Downloading from .+\/mjavadoc338-transitive-1\.0-javadoc-resources\.jar/} 
