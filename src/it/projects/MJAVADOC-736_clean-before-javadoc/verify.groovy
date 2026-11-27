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

// MJAVADOC-736: javadoc:javadoc must detect the module-info.java in source even when
// target/classes doesn't exist yet, e.g. because this goal was run directly without a
// prior compile.
def moduleSummary = new File(basedir, 'target/reports/apidocs/mjavadoc736/module-summary.html')
assert moduleSummary.exists()

def classFile = new File(basedir,
        'target/reports/apidocs/mjavadoc736/org/apache/maven/plugins/javadoc/it/mjavadoc736/HelloWorld.html')
assert classFile.exists()
assert classFile.text =~ /greet/
