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
package org.apache.maven.plugins.javadoc.options.io.xpp3;

import java.io.StringReader;

import org.apache.maven.plugins.javadoc.options.JavadocOptions;
import org.apache.maven.plugins.javadoc.options.Tag;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavadocOptionsXpp3ReaderTest {

    @Test
    public void testNameAndHead() throws Exception {
        JavadocOptionsXpp3Reader parser = new JavadocOptionsXpp3Reader();
        String testString = "<javadocOptions><tags><tag><name>foo</name><head>bar</head></tag></tags></javadocOptions>";
        StringReader reader = new StringReader(testString);

        JavadocOptions options = parser.read(reader);
        assertThat(options.getTags().size()).isEqualTo(1);
        Tag tag = options.getTags().get(0);
        assertThat(tag.getName()).isEqualTo("foo");
        assertThat(tag.getHead()).isEqualTo("bar");
    }

    @Test
    public void testPlacement() throws Exception {
        JavadocOptionsXpp3Reader parser = new JavadocOptionsXpp3Reader();
        String testString =
                "<javadocOptions><tags><tag><name>foo</name><placement>Xaoptcmf</placement><head>bar</head></tag></tags></javadocOptions>";
        StringReader reader = new StringReader(testString);

        JavadocOptions options = parser.read(reader);
        assertThat(options.getTags().size()).isEqualTo(1);
        Tag tag = options.getTags().get(0);
        assertThat(tag.getName()).isEqualTo("foo");
        assertThat(tag.getPlacement()).isEqualTo("Xaoptcmf");
    }
}
