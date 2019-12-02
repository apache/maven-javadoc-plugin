
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

File target = new File( basedir, "module2/target" );
assert target.exists()
assert target.isDirectory()

File apidocs = new File( target, "apidocs" );
assert apidocs.exists()
assert apidocs.isDirectory()

// module3 must be included
File module3File = new File( apidocs, "org/apache/maven/plugin/javadoc/it/Module3Class.html" );
assert module3File.exists()
assert module3File.isFile()

// el-api must be included
File elApiFile = new File( apidocs, "javax/el/ValueExpression.html" );
assert elApiFile.exists()
assert elApiFile.isFile()

// module1 must NOT be included
File module1File = new File( apidocs, "org/apache/maven/plugin/javadoc/it/Module1Class.html" );
assert !module1File.exists()
assert !module1File.isFile()

// servlet-api must NOT be included
File servletSpecFile = new File( apidocs, "javax/servlet/ServletContext.html" );
assert !servletSpecFile.exists()
assert !servletSpecFile.isFile()

