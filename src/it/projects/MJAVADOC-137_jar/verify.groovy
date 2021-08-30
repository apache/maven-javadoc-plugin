
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

import java.util.jar.*

File target1 = new File( basedir, 'test1/target/test1-1.0-SNAPSHOT-javadoc.jar' )
assert target1.exists()

File target2 = new File( basedir, 'test2/target/test2-1.0-SNAPSHOT-javadoc.jar' )
assert target2.exists()

JarFile jar = new JarFile( target1 )
Enumeration jarEntries = jar.entries()
long timestamp = -1
while ( jarEntries.hasMoreElements() )
{
    JarEntry entry = (JarEntry) jarEntries.nextElement()
    if ( timestamp == -1 )
    {
        timestamp = entry.getTime(); // reproducible timestamp in jar file cause local timestamp depending on timezone
    }
    assert entry.getTime() == timestamp
}
jarEntries = new JarFile( target1 ).entries()
while ( jarEntries.hasMoreElements() )
{
    JarEntry entry = (JarEntry) jarEntries.nextElement()
	assert entry.getTime() == timestamp
}