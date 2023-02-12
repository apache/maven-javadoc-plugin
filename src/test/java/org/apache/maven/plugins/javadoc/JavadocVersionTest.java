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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavadocVersionTest {
    /**
     * Parsing is lazy, only triggered when comparing
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testParse() {
        assertThat(JavadocVersion.parse("1.4"))
                .isLessThan(JavadocVersion.parse("1.4.2"))
                .isLessThan(JavadocVersion.parse("1.5"));

        assertThat(JavadocVersion.parse("1.8")).isLessThan(JavadocVersion.parse("9"));

        assertThat(JavadocVersion.parse("1.4")).isEqualByComparingTo(JavadocVersion.parse("1.4"));
        assertThat(JavadocVersion.parse("1.4.2")).isEqualByComparingTo(JavadocVersion.parse("1.4.2"));
        assertThat(JavadocVersion.parse("9")).isEqualByComparingTo(JavadocVersion.parse("9"));

        assertThat(JavadocVersion.parse("1.4.2")).isGreaterThan(JavadocVersion.parse("1.4"));
        assertThat(JavadocVersion.parse("1.5")).isGreaterThan(JavadocVersion.parse("1.4"));
        assertThat(JavadocVersion.parse("9")).isGreaterThan(JavadocVersion.parse("1.8"));
    }

    @Test
    public void testApiVersion() {
        Pattern p = Pattern.compile("(1\\.\\d|\\d\\d*)");
        Matcher m = p.matcher("9");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("9");

        m = p.matcher("1.4");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("1.4");

        m = p.matcher("1.4.2");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("1.4");
    }
}
