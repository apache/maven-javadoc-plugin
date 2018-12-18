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

// A 
assert new File( basedir, 'target/site/apidocs/a/b/c/d/D1.html').exists()
assert new File( basedir, 'target/site/apidocs/a/b/c/d/D2.html').exists()
assert new File( basedir, 'target/site/apidocs/a/b/e/E.html').exists()
assert new File( basedir, 'target/site/apidocs/a/f/F.html').exists()

// B
assert new File( basedir, 'b/target/site/apidocs/a/b/c/d/D1.html').exists()
assert new File( basedir, 'b/target/site/apidocs/a/b/c/d/D2.html').exists()
assert new File( basedir, 'b/target/site/apidocs/a/b/e/E.html').exists()
assert !(new File( basedir, 'b/target/site/apidocs/a/f/F.html').exists())

// C
assert new File( basedir, 'b/c/target/site/apidocs/a/b/c/d/D1.html').exists()
assert new File( basedir, 'b/c/target/site/apidocs/a/b/c/d/D2.html').exists()
assert !(new File( basedir, 'b/c/target/site/apidocs/a/b/e/E.html').exists())
assert !(new File( basedir, 'b/c/target/site/apidocs/a/f/F.html').exists())

// D1
assert new File( basedir, 'b/c/d1/target/site/apidocs/a/b/c/d/D1.html').exists()
assert !(new File( basedir, 'b/c/d1/target/site/apidocs/a/b/c/d/D2.html').exists())
assert !(new File( basedir, 'b/c/d1/target/site/apidocs/a/b/e/E.html').exists())
assert !(new File( basedir, 'b/c/d1/target/site/apidocs/a/f/F.html').exists())

// D2
assert !(new File( basedir, 'b/c/d2/target/site/apidocs/a/b/c/d/D1.html').exists())
assert new File( basedir, 'b/c/d2/target/site/apidocs/a/b/c/d/D2.html').exists()
assert !(new File( basedir, 'b/c/d2/target/site/apidocs/a/b/e/E.html').exists())
assert !(new File( basedir, 'b/c/d2/target/site/apidocs/a/f/F.html').exists())

// E
assert !(new File( basedir, 'b/e/target/site/apidocs/a/b/c/d/D1.html').exists())
assert !(new File( basedir, 'b/e/target/site/apidocs/a/b/c/d/D2.html').exists())
assert new File( basedir, 'b/e/target/site/apidocs/a/b/e/E.html').exists()
assert !(new File( basedir, 'b/e/target/site/apidocs/a/f/F.html').exists())

// F
assert !(new File( basedir, 'f/target/site/apidocs/a/b/c/d/D1.html').exists())
assert !(new File( basedir, 'f/target/site/apidocs/a/b/c/d/D2.html').exists())
assert !(new File( basedir, 'f/target/site/apidocs/a/b/e/E.html').exists())
assert new File( basedir, 'f/target/site/apidocs/a/f/F.html').exists()
