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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>Generates documentation for the <code>Java Test code</code> in an <b>aggregator</b> project using the standard
 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/javadoc/">Javadoc Tool</a>.</p>
 *
 * <p>Since version 3.1.0 an aggregated report is created for every module of a Maven multimodule project.</p>
 * 
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.5
 */
@Mojo( name = "test-aggregate", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST )
@Execute( phase = LifecyclePhase.TEST_COMPILE )
public class AggregatorTestJavadocReport
    extends TestJavadocReport
{
    @Override
    protected boolean isAggregator()
    {
        return true;
    }
}
