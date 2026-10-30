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

/**
 * Keeps reports multi-page under maven-site-plugin 3.22 and older, which load maven-reporting-api 4.0.0 and only call
 * {@code generate(Sink, SinkFactory, Locale)} through {@code MavenMultiPageReport}. Remove once the site plugin
 * minimum is 3.23.0.
 */
@SuppressWarnings("deprecation")
interface MultiPageReportCompat extends org.apache.maven.reporting.MavenMultiPageReport {}
