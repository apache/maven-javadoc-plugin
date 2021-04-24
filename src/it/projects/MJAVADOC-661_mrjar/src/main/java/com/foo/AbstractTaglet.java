package com.foo;

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

import com.sun.source.doctree.DocTree;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Taglet;

public abstract class AbstractTaglet implements Taglet
{
    @Override
    public String getName( )
    {
        return getClass( ).getSimpleName( );
    }

    @Override
    public boolean isInlineTag( )
    {
        return true;
    }


    @Override
    public Set<Taglet.Location> getAllowedLocations()
    {
        return EnumSet.allOf(Taglet.Location.class);
    }

    @Override
    public String toString( List<? extends DocTree> tags, Element element )
    {
        return getName( );
    }
}
