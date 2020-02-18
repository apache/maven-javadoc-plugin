package com.javamodularity.modulec;

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

/**
 * <strong>WorldCollector</strong>
 */
public class WorldCollector {

   public void collect() 
   {
       getA().main();
       
       try
       {
           getB().main();
       }
       catch ( Exception e )
       {
           // noop
       }
   }

   public com.javamodularity.modulea.HelloWorld getA()
   {
       return new com.javamodularity.modulea.HelloWorld();
   }

   public com.javamodularity.moduleb.HelloWorld getB()
   {
       return new com.javamodularity.moduleb.HelloWorld();
   }
}
