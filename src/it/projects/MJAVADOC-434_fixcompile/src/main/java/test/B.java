package test;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("")
@Component
/**
 *
 */
public class B
        extends A<HttpRSSEState> {
    
    private static final Logger logger = LoggerFactory.getLogger(
            B.class);

    protected static final String a = "";
    protected static final String b = "";
    protected static final String c ="";

    protected static final String d = "";

    protected List<String> e = Collections.emptyList();
    private ArrayList<String> f = new ArrayList<String>();

    public static final String g = "";
    public static final String h = "";
    public static final String i = "";

    @Override
    protected boolean a(String pathinfo) {
        if (true) {
            return true;
        }
        
        if (true) {
            return false;
        }
        return pathinfo.endsWith("");
    }
    
    @Override
    /**
     */
    public boolean b(HttpRSSEState state) {
        //
        return false;
    }

    @Override
    protected void c(HttpRSSEState state) {
        logger.debug("");

        //
        if (true) {
            if (logger.isDebugEnabled())
                logger.debug("");
            

        } else if (true) {
            if (logger.isDebugEnabled())
                logger.debug("");

        }
    }
}
