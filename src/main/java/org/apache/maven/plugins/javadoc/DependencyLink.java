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
 * In case of autodetectLinks, this class can overwrite the url for that dependency
 *
 * @since 3.3.0
 * @author elharo
 */
public class DependencyLink {
    private String groupId;

    private String artifactId;

    private String classifier;

    private String url;

    /**
     * <p>Getter for the field <code>url</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>Setter for the field <code>url</code>.</p>
     *
     * @param url a {@link java.lang.String} object
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * <p>Getter for the field <code>groupId</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * <p>Setter for the field <code>groupId</code>.</p>
     *
     * @param groupId a {@link java.lang.String} object
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * <p>Getter for the field <code>artifactId</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * <p>Setter for the field <code>artifactId</code>.</p>
     *
     * @param artifactId a {@link java.lang.String} object
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * <p>Getter for the field <code>classifier</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * <p>Setter for the field <code>classifier</code>.</p>
     *
     * @param classifier a {@link java.lang.String} object
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }
}
