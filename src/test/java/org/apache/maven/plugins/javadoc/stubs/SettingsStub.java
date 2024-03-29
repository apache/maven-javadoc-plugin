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
package org.apache.maven.plugins.javadoc.stubs;

import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class SettingsStub extends Settings {
    /** {@inheritDoc} */
    @Override
    public synchronized Proxy getActiveProxy() {
        Proxy proxy = new Proxy();
        proxy.setActive(true);
        proxy.setHost("http://localhost");
        proxy.setPort(80);
        proxy.setUsername("toto");
        proxy.setPassword("toto");
        proxy.setNonProxyHosts("www.google.com|*.somewhere.com");

        return proxy;
    }

    /** {@inheritDoc} */
    @Override
    public List<Proxy> getProxies() {
        return Collections.singletonList(getActiveProxy());
    }
}
