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
 
def buildLog = new File(basedir,'build.log')

def expectedLines = ['Creating an aggregated report for both named and unnamed modules is not possible.',
                     'Ensure that every module has a module descriptor or is a jar with a MANIFEST.MF containing an Automatic-Module-Name.',
                     'Fix the following projects:',
                     ' - testcase:module2:1.0-SNAPSHOT']

def errorLines = buildLog.readLines() 
                         .dropWhile{ !it.startsWith('[ERROR] Creating') }
                         .takeWhile{ it.startsWith('[ERROR]') }
                         .collect  { it - '[ERROR] '}
                         
assert expectedLines == errorLines                   
