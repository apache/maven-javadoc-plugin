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
 
def javaVersion = System.getProperty( 'java.specification.version' )

if ( javaVersion.startsWith('1.') || Integer.parseInt(javaVersion) < 16 )
{
  def buildLog = new File(basedir,'build.log')
  assert buildLog.readLines().any{ it ==~ /\[DEBUG\] Found Java API link: .*\/javase\/\d+\/docs\/api\// }
}
else
{
  def barHtml = new File(basedir,'target/reports/apidocs/foo/Bar.html')
  assert barHtml.text =~ /<a href="https:[^"]+Object.html"/
}
