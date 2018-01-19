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

import java.util.Map;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterGrokDescriptor;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.FilterGrokDescriptorImpl;
import org.apache.log4j.Logger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FilterGrokTest {
  private static final Logger LOG = Logger.getLogger(FilterGrokTest.class);

  private FilterGrok filterGrok;
  private OutputManager mockOutputManager;
  private Capture<Map<String, Object>> capture;

  public void init(FilterGrokDescriptor filterGrokDescriptor) throws Exception {
    mockOutputManager = EasyMock.strictMock(OutputManager.class);
    capture = EasyMock.newCapture(CaptureType.LAST);

    filterGrok = new FilterGrok();
    filterGrok.loadConfig(filterGrokDescriptor);
    filterGrok.setOutputManager(mockOutputManager);
    filterGrok.setInput(EasyMock.mock(Input.class));
    filterGrok.init(new LogFeederProps());
  }

  @Test
  public void testFilterGrok_parseMessage() throws Exception {
    LOG.info("testFilterGrok_parseMessage()");

    FilterGrokDescriptorImpl filterGrokDescriptor = new FilterGrokDescriptorImpl();
    filterGrokDescriptor.setMessagePattern("(?m)^%{TIMESTAMP_ISO8601:logtime}%{SPACE}%{LOGLEVEL:level}%{SPACE}%{GREEDYDATA:log_message}");
    filterGrokDescriptor.setMultilinePattern("^(%{TIMESTAMP_ISO8601:logtime})");
    init(filterGrokDescriptor);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    filterGrok.apply("2016-04-08 15:55:23,548 INFO This is a test", new InputFileMarker(null, null, 0));
    filterGrok.apply("2016-04-08 15:55:24,548 WARN Next message", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertNotNull(jsonParams);
    assertEquals("Incorrect parsing: log time", "2016-04-08 15:55:23,548", jsonParams.remove("logtime"));
    assertEquals("Incorrect parsing: log level", "INFO", jsonParams.remove("level"));
    assertEquals("Incorrect parsing: log message", "This is a test", jsonParams.remove("log_message"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testFilterGrok_parseMultiLineMessage() throws Exception {
    LOG.info("testFilterGrok_parseMultiLineMessage()");

    FilterGrokDescriptorImpl filterGrokDescriptor = new FilterGrokDescriptorImpl();
    filterGrokDescriptor.setMessagePattern("(?m)^%{TIMESTAMP_ISO8601:logtime}%{SPACE}%{LOGLEVEL:level}%{SPACE}%{GREEDYDATA:log_message}");
    filterGrokDescriptor.setMultilinePattern("^(%{TIMESTAMP_ISO8601:logtime})");
    init(filterGrokDescriptor);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    String multiLineMessage = "This is a multiline test message\r\n" + "having multiple lines\r\n"
        + "as one may expect";
    String[] messageLines = multiLineMessage.split("\r\n");
    for (int i = 0; i < messageLines.length; i++)
      filterGrok.apply((i == 0 ? "2016-04-08 15:55:23,548 INFO " : "") + messageLines[i], new InputFileMarker(null, null, 0));
    filterGrok.flush();

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertNotNull(jsonParams);
    assertEquals("Incorrect parsing: log time", "2016-04-08 15:55:23,548", jsonParams.remove("logtime"));
    assertEquals("Incorrect parsing: log level", "INFO", jsonParams.remove("level"));
    assertEquals("Incorrect parsing: log message", multiLineMessage, jsonParams.remove("log_message"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testFilterGrok_notMatchingMesagePattern() throws Exception {
    LOG.info("testFilterGrok_notMatchingMesagePattern()");

    FilterGrokDescriptorImpl filterGrokDescriptor = new FilterGrokDescriptorImpl();
    filterGrokDescriptor.setMessagePattern("(?m)^%{TIMESTAMP_ISO8601:logtime}%{SPACE}%{LOGLEVEL:level}%{SPACE}%{GREEDYDATA:log_message}");
    filterGrokDescriptor.setMultilinePattern("^(%{TIMESTAMP_ISO8601:logtime})");
    init(filterGrokDescriptor);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(mockOutputManager);

    filterGrok.apply("04/08/2016 15:55:23,548 INFO This is a test", new InputFileMarker(null, null, 0));
    filterGrok.apply("04/08/2016 15:55:24,548 WARN Next message", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    assertFalse("Something was captured!", capture.hasCaptured());
  }

  @Test
  public void testFilterGrok_noMesagePattern() throws Exception {
    LOG.info("testFilterGrok_noMesagePattern()");

    FilterGrokDescriptorImpl filterGrokDescriptor = new FilterGrokDescriptorImpl();
    filterGrokDescriptor.setMultilinePattern("^(%{TIMESTAMP_ISO8601:logtime})");
    init(filterGrokDescriptor);

    EasyMock.replay(mockOutputManager);

    filterGrok.apply("2016-04-08 15:55:23,548 INFO This is a test", new InputFileMarker(null, null, 0));
    filterGrok.apply("2016-04-08 15:55:24,548 WARN Next message", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    assertFalse("Something was captured", capture.hasCaptured());
  }

  @After
  public void cleanUp() {
    capture.reset();
  }
}
