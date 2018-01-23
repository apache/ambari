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

package org.apache.ambari.logfeeder.filter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.common.LogFeederException;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.FilterJsonDescriptorImpl;
import org.apache.log4j.Logger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FilterJSONTest {
  private static final Logger LOG = Logger.getLogger(FilterJSONTest.class);

  private FilterJSON filterJson;
  private OutputManager mockOutputManager;
  private Capture<Map<String, Object>> capture;

  public void init(FilterJsonDescriptorImpl filterJsonDescriptor) throws Exception {
    mockOutputManager = EasyMock.strictMock(OutputManager.class);
    capture = EasyMock.newCapture(CaptureType.LAST);

    filterJson = new FilterJSON();
    filterJson.loadConfig(filterJsonDescriptor);
    filterJson.setOutputManager(mockOutputManager);
    filterJson.init(new LogFeederProps());
  }

  @Test
  public void testJSONFilterCode_convertFields() throws Exception {
    LOG.info("testJSONFilterCode_convertFields()");

    init(new FilterJsonDescriptorImpl());

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    Date d = new Date();
    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String dateString = sdf.format(d);
    filterJson.apply("{ logtime: '" + d.getTime() + "', line_number: 100 }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Incorrect decoding: log time", dateString, jsonParams.remove("logtime"));
    assertEquals("Incorrect decoding: in memory timestamp", d.getTime(), jsonParams.remove(LogFeederConstants.IN_MEMORY_TIMESTAMP));
    assertEquals("Incorrect decoding: line number", 100l, jsonParams.remove("line_number"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testJSONFilterCode_logTimeOnly() throws Exception {
    LOG.info("testJSONFilterCode_logTimeOnly()");

    init(new FilterJsonDescriptorImpl());

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    Date d = new Date();
    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String dateString = sdf.format(d);
    filterJson.apply("{ logtime: '" + d.getTime() + "', some_field: 'abc' }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Incorrect decoding: log time", dateString, jsonParams.remove("logtime"));
    assertEquals("Incorrect decoding: in memory timestamp", d.getTime(), jsonParams.remove(LogFeederConstants.IN_MEMORY_TIMESTAMP));
    assertEquals("Incorrect decoding: some field", "abc", jsonParams.remove("some_field"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testJSONFilterCode_lineNumberOnly() throws Exception {
    LOG.info("testJSONFilterCode_lineNumberOnly()");

    init(new FilterJsonDescriptorImpl());

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    filterJson.apply("{ line_number: 100, some_field: 'abc' }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Incorrect decoding: line number", 100l, jsonParams.remove("line_number"));
    assertEquals("Incorrect decoding: some field", "abc", jsonParams.remove("some_field"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }
  
  
  @Test
  public void testJSONFilterCode_invalidJson() throws Exception {
    LOG.info("testJSONFilterCode_invalidJson()");
    
    init(new FilterJsonDescriptorImpl());
    
    String inputStr = "invalid json";
    try{
      filterJson.apply(inputStr,new InputFileMarker(null, null, 0));
      fail("Expected LogFeederException was not occured");
    } catch(LogFeederException logFeederException) {
      assertEquals("Json parsing failed for inputstr = " + inputStr, logFeederException.getLocalizedMessage());
    }
  }

  @After
  public void cleanUp() {
    capture.reset();
  }
}
