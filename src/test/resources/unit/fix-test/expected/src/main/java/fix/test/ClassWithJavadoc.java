package fix.test;

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

/**
 * To add default class tags.
 *
 * @author <a href="mailto:vsiveton@apache.org">vsiveton@apache.org</a>
 */
@SuppressWarnings("SameReturnValue")
public class ClassWithJavadoc
    implements InterfaceWithJavadoc
{
    /**
     * Constructor comment.
     */
    public ClassWithJavadoc()
    {
    }

    /**
     * <p>Constructor for ClassWithJavadoc.</p>
     *
     * @param aString a {@link java.lang.String} object
     */
    public ClassWithJavadoc( String aString )
    {
    }

    /**
     * public constructor with annotation
     *
     * @param b a {@link java.lang.Boolean} object
     * @since 1.1
     */
    @SuppressWarnings("SameReturnValue")
    public ClassWithJavadoc( Boolean b )
    {
    }

    /**
     * private constructor with annotation
     */
    @SuppressWarnings("SameReturnValue")
    private ClassWithJavadoc( Integer i )
    {
    }

    /**
     * Spaces in tags.
     *
     * @param args      an array of strings that contains the arguments
     */
    public void spacesInJavadocTags( String[] args )
    {
    }

    /**
     * <p>missingJavadocTags.</p>
     *
     * @param str a {@link java.lang.String} object
     * @param b a boolean
     * @param i a int
     * @return a {@link java.lang.String} object
     */
    public String missingJavadocTags( String str, boolean b, int i )
    {
        return null;
    }

    /**
     * <p>missingJavadocTags2.</p>
     *
     * @param str a {@link java.lang.String} object
     * @throws java.lang.UnsupportedOperationException if any
     * @param b a boolean
     * @param i a int
     * @return a {@link java.lang.String} object
     */
    public String missingJavadocTags2( String str, boolean b, int i )
        throws UnsupportedOperationException
    {
        return null;
    }

    /**
     * <p>wrongJavadocTag.</p>
     *
     * @param aString a {@link java.lang.String} object
     */
    public void wrongJavadocTag( String aString )
    {
    }

    /**
     * <p>multiLinesJavadocTags.</p>
     *
     * @param aString
     *      a string
     * @param anotherString
     *      with
     *      multi
     *      line
     *      comments
     * @return a
     *      String
     * @throws java.lang.UnsupportedOperationException
     *      if any
     */
    public String multiLinesJavadocTags( String aString, String anotherString )
        throws UnsupportedOperationException
    {
        return null;
    }

    /**
     * To take care of the Javadoc indentation.
     *
     * @param aString a
     *      String
     * @return dummy
     *      value
     */
    public String wrongJavadocIndentation( String aString )
    {
        return null;
    }

    // one single comment
    /**
     * To take care of single comments.
     *
     * @param aString a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    // other single comment
    public String singleComments( String aString )
    {
        return null;
    }

    /**
     * To take care of same comments.
     *
     * @param aString a string
     * @return a string
     */
    public String sameString( String aString )
    {
        return null;
    }

    /**
     * Empty Javadoc tag.
     *
     * @return a string
     * @param aString a {@link java.lang.String} object
     */
    public String emptyJavadocTag( String aString )
    {
        return null;
    }

    /**
     * Comment on first line.
     *
     * @param aString a string
     * @return a string
     */
    public String javadocCommentOnFirstLine( String aString )
    {
        return null;
    }

    /**
     * Take care of last empty javadoc with unused tags.
     */
    public void unusedTag()
    {
    }

    /**
     * Take care of RuntimeException.
     *
     * @throws java.lang.UnsupportedOperationException if any
     */
    public void throwsTagWithRuntimeException()
    {
    }

    /**
     * Take care of inner RuntimeException.
     *
     * @throws fix.test.ClassWithJavadoc$MyRuntimeException if any
     */
    public void throwsTagWithInnerRuntimeException()
    {
    }

    /**
     * Unknown throws RuntimeException.
     *
     * @throws UnknownRuntimeException if any
     */
    public void throwsTagWithUnknownRuntimeException()
    {
    }

    // ----------------------------------------------------------------------
    // New methods to be found by Clirr.
    // ----------------------------------------------------------------------

    /**
     * New class method to be found by Clirr.
     *
     * @param aString a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     * @since 1.1
     */
    public String newClassMethod( String aString )
    {
        return null;
    }

    // ----------------------------------------------------------------------
    // Inheritance
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void method1( String aString )
    {
    }

    /**
     * {@inheritDoc}
     *
     * specific comment
     */
    public void method2( String aString )
    {
    }

    /** {@inheritDoc} */
    public String method3( String aString )
    {
        return null;
    }

    /** {@inheritDoc} */
    public String method4( String aString )
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Specific comment
     */
    public String method5( String aString )
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * New interface method to be found by Clirr.
     */
    public String newInterfaceMethod( String aString )
    {
        return null;
    }

    // ----------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------

    /**
     * No javadoc for inner class.
     */
    public class InnerClass
    {
        /**
         * constructor
         */
        public InnerClass()
        {
        }

        public void nothing()
        {
        }
    }

    /**
     * RuntimeException
     */
    public static class MyRuntimeException
        extends RuntimeException
    {
    }

    /**
     * private method with annotation
     */
    @SuppressWarnings("SameReturnValue")
    private void t000()
    {
        return;
    }

    /**
     * private method with line comment
     */
    //test comment
    private void t001()
    {
        return;
    }

    /**
     * public method with annotation
     *
     * @since 1.1
     */
    @SuppressWarnings("SameReturnValue")
    public void t010()
    {
        return;
    }

    /**
     * public method with annotation
     *
     * @since 1.1
     */
    //test comment
    public void t011()
    {
        return;
    }

    /**
     * test generic function (with only head javadoc)
     *
     * @param tClass a {@link java.lang.Class} object
     * @param o a {@link java.lang.Object} object
     * @param <T> a T class
     * @since 1.1
     */
    public <T extends Object> void testGeneric0(Class<T> tClass, Object o)
    {
        return;
    }

    /**
     * test generic function (with full javadoc)
     *
     * @param tClass this comment should be preserved
     * @param o this comment should be preserved
     * @param <T> this comment should be preserved
     * @since 1.1
     */
    public <T extends Object> void testGeneric1(Class<T> tClass, Object o)
    {
        return;
    }

    /**
     * test generic function (with full javadoc, except generic docs)
     *
     * @param tClass this comment should be preserved
     * @param o this comment should be preserved
     * @param <T> a T class
     * @since 1.1
     */
    public <T extends Object> void testGeneric2(Class<T> tClass, Object o)
    {
        return;
    }

    /**
     * test whether it will change exception description when using "fix"
     * test if we use fully qualified name for the exception.
     *
     * @throws java.lang.Exception original description, should not be changed to "if any".
     * @since 1.1
     */
    public void testException0()
            throws Exception
    {
        throw new Exception();
    }

    /**
     * test whether it will change exception description when using "fix"
     * test if we use short name for the exception.
     *
     * @throws java.lang.Exception original description, should not be changed to "if any".
     * @since 1.1
     */
    public void testException1()
            throws Exception
    {
        throw new Exception();
    }

    /**
     * test whether it will change exception description when using "fix"
     * test if we use a wrong name for the exception.
     *
     * @throws RuaaaaaaException abcdefghijklmn.
     * @throws java.lang.Exception if any.
     * @since 1.1
     */
    public void testException2()
            throws Exception
    {
        throw new Exception();
    }

    /**
     * test whether it will change exception description when using "fix"
     * test if we provide only one exception description.
     *
     * @throws java.lang.RuntimeException text.
     * @throws java.lang.Exception if any.
     * @since 1.1
     */
    public void testException3()
            throws RuntimeException , Exception
    {
        throw new Exception();
    }

    /**
     * to test if it will handle static final int field with left shift operators correctly.
     */
    public static final int TEST_STATIC_FINAL_FIELD_0 = 1 << 2;

    /**
     * to test if it will handle static final int field with right shift operators correctly.
     */
    public static final int TEST_STATIC_FINAL_FIELD_1 = 2 >> 1;

    /**
     * to test if it will handle static final String field with left shift operator and right shift operator correctly.
     */
    public static final String TEST_STATIC_FINAL_FIELD_2 = "<>?";
}

/**
 * To test package class
 */
@SuppressWarnings("SameReturnValue")
class PrivateTestClassWithJavadoc
{
}
