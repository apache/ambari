/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.config.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

public class LogSearchConfigFactoryTest {

  @Test
  public void testDefaultConfigServer() throws Exception {
    LogSearchConfigServer config = LogSearchConfigFactory.createLogSearchConfigServer( Collections.<String, String> emptyMap(),
        LogSearchConfigServerClass1.class);
    
    Assert.assertSame(config.getClass(), LogSearchConfigServerClass1.class);
  }

  @Test
  public void testCustomConfigServer() throws Exception {
    Map<String, String> logsearchConfClassMap = new HashMap<>();
    logsearchConfClassMap.put("logsearch.config.server.class", "org.apache.ambari.logsearch.config.api.LogSearchConfigServerClass2");
    LogSearchConfig config = LogSearchConfigFactory.createLogSearchConfigServer(logsearchConfClassMap,
        LogSearchConfigServerClass1.class);
    
    Assert.assertSame(config.getClass(), LogSearchConfigServerClass2.class);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testNonConfigClassServer() throws Exception {
    Map<String, String> logsearchConfClassMap = new HashMap<>();
    logsearchConfClassMap.put("logsearch.config.server.class", "org.apache.ambari.logsearch.config.api.NonLogSearchConfigClass");
    LogSearchConfigFactory.createLogSearchConfigServer(logsearchConfClassMap, LogSearchConfigServerClass1.class);
  }

  @Test
  public void testDefaultConfigLogFeeder() throws Exception {
    LogSearchConfigLogFeeder config = LogSearchConfigFactory.createLogSearchConfigLogFeeder( Collections.<String, String> emptyMap(),
        null, LogSearchConfigLogFeederClass1.class);
    
    Assert.assertSame(config.getClass(), LogSearchConfigLogFeederClass1.class);
  }

  @Test
  public void testCustomConfigLogFeeder() throws Exception {
    Map<String, String> logsearchConfClassMap = new HashMap<>();
    logsearchConfClassMap.put("logsearch.config.logfeeder.class", "org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeederClass2");
    LogSearchConfigLogFeeder config = LogSearchConfigFactory.createLogSearchConfigLogFeeder(logsearchConfClassMap, null,
        LogSearchConfigLogFeederClass1.class);
    
    Assert.assertSame(config.getClass(), LogSearchConfigLogFeederClass2.class);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testNonConfigClassLogFeeder() throws Exception {
    Map<String, String> logsearchConfClassMap = new HashMap<>();
    logsearchConfClassMap.put("logsearch.config.logfeeder.class", "org.apache.ambari.logsearch.config.api.NonLogSearchConfigClass");
    LogSearchConfigFactory.createLogSearchConfigLogFeeder(logsearchConfClassMap, null, LogSearchConfigLogFeederClass1.class);
  }
}
