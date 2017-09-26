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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.loglevelfilter.FilterLogData;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.util.LogFeederPropertiesUtil;
import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.ambari.logsearch.config.api.model.loglevelfilter.LogLevelFilter;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.InputDescriptorImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogConfigHandlerTest {
  
  private static InputMarker inputMarkerAudit;
  private static InputMarker inputMarkerService;
  static {
    InputDescriptorImpl auditInputDescriptor = new InputDescriptorImpl() {};
    auditInputDescriptor.setRowtype("audit");
    
    Input auditInput = strictMock(Input.class);
    expect(auditInput.getInputDescriptor()).andReturn(auditInputDescriptor).anyTimes();
    inputMarkerAudit = new InputMarker(auditInput, null, 0);
    
    InputDescriptorImpl serviceInputDescriptor = new InputDescriptorImpl() {};
    serviceInputDescriptor.setRowtype("service");
    
    Input serviceInput = strictMock(Input.class);
    expect(serviceInput.getInputDescriptor()).andReturn(serviceInputDescriptor).anyTimes();
    inputMarkerService = new InputMarker(serviceInput, null, 0);
    
    replay(auditInput, serviceInput);
  }
  
  @BeforeClass
  public static void init() throws Exception {
    LogFeederPropertiesUtil.loadProperties("logfeeder.properties");
    
    LogSearchConfig config = strictMock(LogSearchConfig.class);
    config.createLogLevelFilter(anyString(), anyString(), anyObject(LogLevelFilter.class));
    expectLastCall().anyTimes();
    LogLevelFilterHandler.init(config);
    
    LogLevelFilter logLevelFilter1 = new LogLevelFilter();
    logLevelFilter1.setHosts(Collections.<String> emptyList());
    logLevelFilter1.setDefaultLevels(Arrays.asList("FATAL", "ERROR", "WARN", "INFO"));
    logLevelFilter1.setOverrideLevels(Collections.<String> emptyList());
    
    LogLevelFilter logLevelFilter2 = new LogLevelFilter();
    logLevelFilter2.setHosts(Arrays.asList("host1"));
    logLevelFilter2.setDefaultLevels(Arrays.asList("FATAL", "ERROR", "WARN", "INFO"));
    logLevelFilter2.setOverrideLevels(Arrays.asList("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"));
    logLevelFilter2.setExpiryTime(DateUtils.addDays(new Date(), 1));
    
    LogLevelFilter logLevelFilter3 = new LogLevelFilter();
    logLevelFilter3.setHosts(Arrays.asList("host1"));
    logLevelFilter3.setDefaultLevels(Arrays.asList("FATAL", "ERROR", "WARN", "INFO"));
    logLevelFilter3.setOverrideLevels(Arrays.asList("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"));
    logLevelFilter3.setExpiryTime(DateUtils.addDays(new Date(), -1));
    
    LogLevelFilterHandler h = new LogLevelFilterHandler();
    h.setLogLevelFilter("configured_log_file1", logLevelFilter1);
    h.setLogLevelFilter("configured_log_file2", logLevelFilter2);
    h.setLogLevelFilter("configured_log_file3", logLevelFilter3);
  }
  
  @Test
  public void testLogConfigHandler_auditAllowed() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file1', 'level':'DEBUG'}",
        inputMarkerAudit));
  }
  
  @Test
  public void testLogConfigHandler_emptyDataAllowed() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed((String)null, inputMarkerService));
    assertTrue(FilterLogData.INSTANCE.isAllowed("", inputMarkerService));
    assertTrue(FilterLogData.INSTANCE.isAllowed(Collections.<String, Object> emptyMap(), inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_notConfiguredLogAllowed() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'not_configured_log_file1', 'level':'WARN'}",
        inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_notConfiguredLogNotAllowed() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'not_configured_log_file1', 'level':'TRACE'}",
        inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_configuredDataAllow() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file1', 'level':'INFO'}",
        inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_configuredDataDontAllow() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file1', 'level':'DEBUG'}",
        inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_overridenConfiguredData() throws Exception {
    assertTrue(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file2', 'level':'DEBUG'}",
        inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_overridenConfiguredDataDifferentHost() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host2', 'type':'configured_log_file2', 'level':'DEBUG'}",
        inputMarkerService));
  }
  
  @Test
  public void testLogConfigHandler_overridenConfiguredDataExpired() throws Exception {
    assertFalse(FilterLogData.INSTANCE.isAllowed("{'host':'host1', 'type':'configured_log_file3', 'level':'DEBUG'}",
        inputMarkerService));
  }
}
