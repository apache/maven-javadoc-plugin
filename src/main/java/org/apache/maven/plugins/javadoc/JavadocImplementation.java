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

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;

/**
 * Alternative way to invoke Javadoc. By default the system uses
 * {@link CommandLineUtils#executeCommandLine}
 * to invoke the {@code javadoc} command. By registering a different
 * implementation of {@code JavadocImplementation}, one can change the way
 * the {@code javadoc} tool is invoked.
 */
public interface JavadocImplementation
{
  int execute(
        org.codehaus.plexus.util.cli.Commandline cmd, 
        org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer out, 
        org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer err
  ) throws CommandLineException;
}
