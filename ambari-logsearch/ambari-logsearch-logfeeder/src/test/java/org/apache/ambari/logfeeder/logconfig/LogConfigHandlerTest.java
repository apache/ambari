/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.logconfig;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogConfigHandlerTest {
  
  private static LogConfigFetcher mockFetcher;
  
  private static final Map<String, Object> CONFIG_MAP = new HashMap<>();
  static {
    CONFIG_MAP.put("jsons",
        "{'filter':{" +
          "'configured_log_file':{" +
            "'label':'configured_log_file'," +
            "'hosts':[]," +
            "'defaultLevels':['FATAL','ERROR','WARN','INFO']," +
            "'overrideLevels':[]}," +
          "'configured_log_file2':{" +
            "'label':'configured_log_file2'," +
            "'hosts':['host1']," +
            "'defaultLevels':['FATAL','ERROR','WARN','INFO']," +
            "'overrideLevels':['FATAL','ERROR','WARN','INFO','DEBUG','TRACE']," +
            "'expiryTime':'3000-01-01T00:00:00.000Z'}," +
          "'configured_log_file3':{" +
            "'label':'configured_log_file3'," +
            "'hosts':['host1']," +
            "'defaultLevels':['FATAL','ERROR','WARN','INFO']," +
            "'overrideLevels':['FATAL','ERROR','WARN','INFO','DEBUG','TRACE']," +
            "'expiryTime':'1000-01-01T00:00:00.000Z'}" +
          "}}");
  }
  
  @BeforeClass
  public static void init() throws Exception {
    mockFetcher = strictMock(LogConfigFetcher.class);
    Field f = LogConfigFetcher.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, mockFetcher);
    expect(mockFetcher.getConfigDoc()).andReturn(CONFIG_MAP).anyTimes();
    replay(mockFetcher);
    
    LogFeederUtil.loadProperties("logfeeder.properties", null);
    LogConfigHandler.handleConfig();
    Thread.sleep(1000);
  }
  
  @Test
  public void testLogConfigHandler_emptyDataAllowed() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed((String)null));
    assertTrue(FilterLogData.INSTANCE.isAllowed(""));
    assertTrue(FilterLogData.INSTANCE.isAllowed(Collections.<String, Object> emptyMap()));
  }
  
  @Test
  public void testLogConfigHandler_notConfiguredLogAllowed() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'not_configured_log_file', 'level':'INFO'}"));
  }
  
  @Test
  public void testLogConfigHandler_configuredDataAllow() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file', 'level':'INFO'}"));
  }
  
  @Test
  public void testLogConfigHandler_configuredDataDontAllow() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file', 'level':'DEBUG'}"));
  }
  
  @Test
  public void testLogConfigHandler_overridenConfiguredData() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file2', 'level':'DEBUG'}"));
  }
  
  @Test
  public void testLogConfigHandler_overridenConfiguredDataDifferentHost() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host2', 'type':'configured_log_file2', 'level':'DEBUG'}"));
  }
  
  @Test
  public void testLogConfigHandler_overridenConfiguredDataExpired() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file3', 'level':'DEBUG'}"));
  }
  
  @AfterClass
  public static void finish() {
    verify(mockFetcher);
  }
}
