
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

File target = new File(basedir, "target")
assert target.exists() && target.isDirectory() : "target file is missing or a directory."

File apidocs = new File( basedir, "target/reports/apidocs" )
assert apidocs.exists() && apidocs.isDirectory() : "target/reports/apidocs file is missing or a directory."

File index = new File( apidocs, "index.html" )
assert index.exists() && index.isFile() : "target/reports/apidocs/index.html file is missing or not a file."

File test1 = new File( apidocs, "test/Test1.html" )
assert test1.exists() && test1.isFile() : "target/reports/apidocs/test/Test1.html file is missing or not a file."

File test2 = new File( apidocs, "test/Test2.html" )
assert test2.exists() && test2.isFile() : "target/reports/apidocs/test/Test2.html file is missing or not a file."

File generatedTest1 = new File( apidocs, "test/GeneratedTest1.html" )
assert generatedTest1.exists() && generatedTest1.isFile() : "target/reports/apidocs/test/GeneratedTest1.html file is missing or not a file."

File generatedTest2 = new File( apidocs, "test/GeneratedTest2.html" )
assert generatedTest2.exists() && generatedTest2.isFile() : "target/reports/apidocs/test/GeneratedTest2.html file is missing or not a file."
