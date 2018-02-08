/**
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
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterKeyValueDescriptor;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.FilterKeyValueDescriptorImpl;
import org.apache.log4j.Logger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterKeyValueTest {
  private static final Logger LOG = Logger.getLogger(FilterKeyValueTest.class);

  private FilterKeyValue filterKeyValue;
  private OutputManager mockOutputManager;
  private Capture<Map<String, Object>> capture;

  public void init(FilterKeyValueDescriptor filterKeyValueDescriptor) throws Exception {
    mockOutputManager = EasyMock.strictMock(OutputManager.class);
    capture = EasyMock.newCapture(CaptureType.LAST);

    filterKeyValue = new FilterKeyValue();
    filterKeyValue.loadConfig(filterKeyValueDescriptor);
    filterKeyValue.setOutputManager(mockOutputManager);
    filterKeyValue.init(new LogFeederProps());
  }

  @Test
  public void testFilterKeyValue_extraction() throws Exception {
    LOG.info("testFilterKeyValue_extraction()");

    FilterKeyValueDescriptorImpl filterKeyValueDescriptor = new FilterKeyValueDescriptorImpl();
    filterKeyValueDescriptor.setSourceField("keyValueField");
    filterKeyValueDescriptor.setFieldSplit("&");
    init(filterKeyValueDescriptor);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ keyValueField: 'name1=value1&name2=value2' }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Original missing!", "name1=value1&name2=value2", jsonParams.remove("keyValueField"));
    assertEquals("Incorrect extraction: name1", "value1", jsonParams.remove("name1"));
    assertEquals("Incorrect extraction: name2", "value2", jsonParams.remove("name2"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testFilterKeyValue_extractionWithBorders() throws Exception {
    LOG.info("testFilterKeyValue_extractionWithBorders()");

    FilterKeyValueDescriptorImpl filterKeyValueDescriptor = new FilterKeyValueDescriptorImpl();
    filterKeyValueDescriptor.setSourceField("keyValueField");
    filterKeyValueDescriptor.setFieldSplit("&");
    filterKeyValueDescriptor.setValueBorders("()");
    init(filterKeyValueDescriptor);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ keyValueField: 'name1(value1)&name2(value2)' }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Original missing!", "name1(value1)&name2(value2)", jsonParams.remove("keyValueField"));
    assertEquals("Incorrect extraction: name1", "value1", jsonParams.remove("name1"));
    assertEquals("Incorrect extraction: name2", "value2", jsonParams.remove("name2"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testFilterKeyValue_missingSourceField() throws Exception {
    LOG.info("testFilterKeyValue_missingSourceField()");

    FilterKeyValueDescriptorImpl filterKeyValueDescriptor = new FilterKeyValueDescriptorImpl();
    filterKeyValueDescriptor.setFieldSplit("&");
    init(filterKeyValueDescriptor);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ keyValueField: 'name1=value1&name2=value2' }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    assertFalse("Something was captured!", capture.hasCaptured());
  }

  @Test
  public void testFilterKeyValue_noSourceFieldPresent() throws Exception {
    LOG.info("testFilterKeyValue_noSourceFieldPresent()");

    FilterKeyValueDescriptorImpl filterKeyValueDescriptor = new FilterKeyValueDescriptorImpl();
    filterKeyValueDescriptor.setSourceField("keyValueField");
    filterKeyValueDescriptor.setFieldSplit("&");
    init(filterKeyValueDescriptor);

    // using default value split: =
    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputFileMarker.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ otherField: 'name1=value1&name2=value2' }", new InputFileMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Original missing!", "name1=value1&name2=value2", jsonParams.remove("otherField"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @After
  public void cleanUp() {
    capture.reset();
  }
}
