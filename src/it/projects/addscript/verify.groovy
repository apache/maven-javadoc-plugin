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

def file = new File( basedir, 'target/reports/apidocs/com/example/AddScriptJavaDoc.html' );

assert file.exists()

def javaVersion = System.getProperty( 'java.specification.version' )
if( javaVersion ==~ /(1\..+|9\..+|1[0-7]\..+)/ )
{
  	assert new File(basedir, 'build.log').text.contains('--add-scripts option is not supported on Java version < 18')
}
else 
{
	assert 1 == file.text.count('custom-script.js')
	assert 1 == file.text.count('custom-script1.js')
	assert 1 == file.text.count('custom-script2.js')
}
