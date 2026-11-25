
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

import java.io.*;

File target = new File( basedir, "target" );
assert target.exists() && target.isDirectory() : "target file is missing or a directory."

File apidocs = new File( target, "reports/apidocs" );
assert apidocs.exists() && apidocs.isDirectory() : "target/reports/apidocs file is missing or a directory."

File htmlFile = new File( apidocs, "Test.html" );
assert htmlFile.isFile() : "target/reports/apidocs/Test.html file is missing or a a file."
