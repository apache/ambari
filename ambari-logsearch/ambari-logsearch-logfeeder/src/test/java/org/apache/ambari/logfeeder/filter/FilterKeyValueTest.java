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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.output.OutputManager;
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

  public void init(Map<String, Object> config) throws Exception {
    mockOutputManager = EasyMock.strictMock(OutputManager.class);
    capture = EasyMock.newCapture(CaptureType.LAST);

    filterKeyValue = new FilterKeyValue();
    filterKeyValue.loadConfig(config);
    filterKeyValue.setOutputManager(mockOutputManager);
    filterKeyValue.init();
  }

  @Test
  public void testFilterKeyValue_extraction() throws Exception {
    LOG.info("testFilterKeyValue_extraction()");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("source_field", "keyValueField");
    config.put("field_split", "&");
    // using default value split:
    init(config);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ keyValueField: 'name1=value1&name2=value2' }", new InputMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Original missing!", "name1=value1&name2=value2", jsonParams.remove("keyValueField"));
    assertEquals("Incorrect extraction: name1", "value1", jsonParams.remove("name1"));
    assertEquals("Incorrect extraction: name2", "value2", jsonParams.remove("name2"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testFilterKeyValue_missingSourceField() throws Exception {
    LOG.info("testFilterKeyValue_missingSourceField()");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("field_split", "&");
    // using default value split: =
    init(config);

    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputMarker.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ keyValueField: 'name1=value1&name2=value2' }", new InputMarker(null, null, 0));

    EasyMock.verify(mockOutputManager);
    assertFalse("Something was captured!", capture.hasCaptured());
  }

  @Test
  public void testFilterKeyValue_noSourceFieldPresent() throws Exception {
    LOG.info("testFilterKeyValue_noSourceFieldPresent()");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("source_field", "keyValueField");
    config.put("field_split", "&");
    init(config);

    // using default value split: =
    mockOutputManager.write(EasyMock.capture(capture), EasyMock.anyObject(InputMarker.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(mockOutputManager);

    filterKeyValue.apply("{ otherField: 'name1=value1&name2=value2' }", new InputMarker(null, null, 0));

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
