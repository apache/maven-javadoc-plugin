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

// GITHUB-1271: excludePackageNames must reach javadoc as -exclude even when the project is a
// module (module-info.java present) and neither subpackages nor sourcepath are configured --
// the only case the plugin previously computed an empty -exclude argument for.
def optionsFile = new File(basedir, 'target/reports/apidocs/options')
assert optionsFile.exists()
def options = optionsFile.text
assert options.contains('-exclude')
assert options.contains('com.example.excluded')

assert !new File(basedir, 'target/reports/apidocs/github1271/com/example/excluded/Excluded.html').exists()
assert new File(basedir, 'target/reports/apidocs/github1271/com/example/included/Included.html').exists()
