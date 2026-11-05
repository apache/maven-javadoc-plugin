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
package org.apache.maven.plugins.javadoc;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.codehaus.plexus.languages.java.version.JavaVersion;

class EncodingUtils {
    /**
     * Compute the encoding that Javadoc expects for reading and writing of data
     *
     * @return the expected encoding
     * @since 3.12.1
     */
    public static Charset getExpectedEncoding() {
        if (JavaVersion.JAVA_SPECIFICATION_VERSION.isAtLeast("9")
                && JavaVersion.JAVA_SPECIFICATION_VERSION.isBefore("12")) {
            return StandardCharsets.UTF_8;
        } else {
            return Charset.defaultCharset();
        }
    }
}
