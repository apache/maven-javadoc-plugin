
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

import java.io.*

File target = new File(basedir, "test/target")
assert target.isDirectory() : "target is missing or not a directory."

File apidocs = new File( basedir, "test/target/reports/apidocs" )
assert apidocs.isDirectory() : apidocs.getAbsolutePath() + " is missing or not a directory."

File file1 = new File( apidocs, "file1.txt" )
assert file1.isFile() : file1.getAbsolutePath() + " is missing or a directory."

File file2 = new File( apidocs, "file2.txt" )
assert file2.isFile() : file2.getAbsolutePath() + " is missing or a directory."

File metainf = new File( apidocs, "META-INF" )
assert !metainf.isDirectory() : metainf.getAbsolutePath() + " exists."
