
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

// Generated files checks
File apidocs1 = new File( basedir, "module1/target/reports/apidocs" );
assert apidocs1.exists() && apidocs1.isDirectory() : apidocs1.getAbsolutePath() + " dir is missing or a directory."
File apidocs2 = new File( basedir, "module2/target/reports/apidocs" );
assert apidocs2.exists() && apidocs2.isDirectory() : apidocs2.getAbsolutePath() + " dir is missing or a directory."

File options1 = new File( apidocs1, "options" );
assert options1.exists() && options1.isFile() : options1.getAbsolutePath() + " file is missing or not a file."
File options2 = new File( apidocs2, "options" );
assert options2.exists() && options2.isFile() : options2.getAbsolutePath() + " file is missing or not a file."

// Read files
String contentOptions1 = options1.getText( 'UTF-8' );
String contentOptions2 = options2.getText( 'UTF-8' );
String log = new File( basedir, "build.log" ).getText( 'UTF-8' );

// Generated files content checks
// module1 does not depend on module2, then it should not link
assert !contentOptions1.contains( "module2" ) : "module1 -linkoffline module2 added: " + options1
int link1 = contentOptions1.indexOf( "-link" );
assert link1 != -1 : "-link not added: " + options1
assert contentOptions1.substring( link1 ).contains( "commons.apache.org" ) || log.contains( "Error fetching link: http://commons.apache.org/lang/apidocs" ) : "link for commons-lang not added: " + options1
assert contentOptions1.substring( link1 ).contains( "junit.org" ) || log.contains( "Error fetching link: http://junit.org/apidocs" ) : "link for junit not added: " + options1
assert !contentOptions1.contains( "http://maven.apache.org/maven2/maven-script/maven-script-marmalade/apidocs" ) : "link for maven-script-marmalade added: " + options1

int linkoffline2 = contentOptions2.indexOf( "module1" );
// module2 depends on module1, then it should link
assert linkoffline2 >= 0 : "module2 -linkoffline module1 not added: " + options2
assert contentOptions2.substring( linkoffline2 ).contains( apidocs1.getCanonicalPath().replaceAll( "\\\\", "/" ) ) : apidocs1.getCanonicalPath().replaceAll( "\\\\", "/" ) + " not added: " + options2
int link2 = contentOptions2.indexOf( "-link" );
assert link2 != -1 : "-link not added: " + options2
assert contentOptions2.substring( link2 ).contains( "commons.apache.org" ) || log.contains( "Error fetching link: http://commons.apache.org/lang/apidocs" ) : "link for commons-lang not added: " + options2
assert contentOptions2.substring( link2 ).contains( "junit.org" ) || log.contains( "Error fetching link: http://junit.org/apidocs" ) : "link for junit not added: " + options2
assert !contentOptions2.contains( "http://maven.apache.org/maven2/maven-script/maven-script-marmalade/apidocs" ) : "link for maven-script-marmalade added: " + options2
