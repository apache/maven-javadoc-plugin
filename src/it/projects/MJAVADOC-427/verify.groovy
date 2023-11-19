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

// assert that javadoc of class correctly contains link, just like method details
assert file.text =~ /Link to slf4j <a href=".*?".*?><code>LoggerFactory<\/code><\/a>/
assert file.text =~ /public.*?<a href=".*?".*?>LoggerFactory<\/a>.*?getLoggerFactory.*?\(\)/
