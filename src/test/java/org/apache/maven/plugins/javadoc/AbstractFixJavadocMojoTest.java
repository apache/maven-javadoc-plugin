package org.apache.maven.plugins.javadoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.StringReader;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFixJavadocMojoTest
{
    private JavaSource getJavaSource( String source )
    {
        return new JavaProjectBuilder().addSource( new StringReader( source ) );
    }

    @Test
    public void testReplaceLinkTags_noLinkTag()
    {
        String comment = "/** @see ConnectException */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class NoLinkTag {}";
                    
        JavaClass clazz = getJavaSource( source ).getClassByName( "NoLinkTag" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );

        assertThat( newComment ).isEqualTo( "/** @see ConnectException */" );
    }

    @Test
    public void testReplaceLinkTags_oneLinkTag()
    {
        String comment = "/** {@link ConnectException} */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class OneLinkTag {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "OneLinkTag" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link java.net.ConnectException} */" );
    }

    @Test
    public void testReplaceLinkTags_missingEndBrace()
    {
        String comment = "/** {@link ConnectException */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class MissingEndBrace {}";
                    
        JavaClass clazz = getJavaSource( source ).getClassByName( "MissingEndBrace" );
                    
        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link ConnectException */" );
    }

    @Test
    public void testReplaceLinkTags_spacesAfterLinkTag()
    {
        String comment = "/** {@link     ConnectException} */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class SpacesAfterLinkTag {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "SpacesAfterLinkTag" );
        
        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link java.net.ConnectException} */" );
    }

    @Test
    public void testReplaceLinkTags_spacesAfterClassName()
    {
        String comment = "/** {@link ConnectException       } */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class SpacesAfterClassName {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "SpacesAfterClassName" );
        
        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link java.net.ConnectException} */" );
    }

    @Test
    public void testReplaceLinkTags_spacesAfterMethod()
    {
        String comment = "/** {@link ConnectException#getMessage()       } */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class SpacesAfterMethod {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "SpacesAfterMethod" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link java.net.ConnectException#getMessage()} */" );
    }

    @Test
    public void testReplaceLinkTags_containingHash()
    {
        String comment = "/** {@link ConnectException#getMessage()} */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class ContainingHashes {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "ContainingHashes" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link java.net.ConnectException#getMessage()} */" );
    }

    @Test
    public void testReplaceLinkTags_followedByHash()
    {
        String comment = "/** {@link ConnectException} ##important## */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class FollowedByHash {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "FollowedByHash" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** {@link java.net.ConnectException} ##important## */" );
    }

    @Test
    public void testReplaceLinkTags_twoLinks()
    {
        String comment = "/** Use {@link ConnectException} instead of {@link Exception} */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class TwoLinks {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "TwoLinks" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo(
                "/** Use {@link java.net.ConnectException} instead of {@link java.lang.Exception} */" );
    }

    @Test
    public void testReplaceLinkTags_OnlyAnchor()
    {
        String comment = "/** There's a {@link #getClass()} but no setClass() */";
        String source = "import java.net.ConnectException;\n"
                        + comment + "\n"
                        + "public class OnlyAnchor {}";
        
        JavaClass clazz = getJavaSource( source ).getClassByName( "OnlyAnchor" );

        String newComment = AbstractFixJavadocMojo.replaceLinkTags( comment, clazz );
        assertThat( newComment ).isEqualTo( "/** There's a {@link #getClass()} but no setClass() */" );
    }
}
