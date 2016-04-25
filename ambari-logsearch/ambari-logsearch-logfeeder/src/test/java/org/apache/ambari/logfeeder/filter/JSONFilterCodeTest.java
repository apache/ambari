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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.OutputMgr;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.log4j.Logger;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JSONFilterCodeTest {
  private static final Logger LOG = Logger.getLogger(JSONFilterCodeTest.class);

  private JSONFilterCode jsonFilterCode;
  private OutputMgr mockOutputMgr;
  private Capture<Map<String, Object>> capture;

  public void init(Map<String, Object> params) throws Exception {
    mockOutputMgr = EasyMock.strictMock(OutputMgr.class);
    capture = EasyMock.newCapture(CaptureType.LAST);

    jsonFilterCode = new JSONFilterCode();
    jsonFilterCode.loadConfig(params);
    jsonFilterCode.setOutputMgr(mockOutputMgr);
    jsonFilterCode.init();
  }

  @Test
  public void testJSONFilterCode_convertFields() throws Exception {
    LOG.info("testJSONFilterCode_convertFields()");

    init(new HashMap<String, Object>());

    mockOutputMgr.write(EasyMock.capture(capture), EasyMock.anyObject(InputMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputMgr);

    Date d = new Date();
    DateFormat sdf = new SimpleDateFormat(LogFeederUtil.SOLR_DATE_FORMAT);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String dateString = sdf.format(d);
    jsonFilterCode.apply("{ logtime: '" + d.getTime() + "', line_number: 100 }", new InputMarker());

    EasyMock.verify(mockOutputMgr);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Incorrect decoding: log time", dateString, jsonParams.remove("logtime"));
    assertEquals("Incorrect decoding: line number", 100l, jsonParams.remove("line_number"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testJSONFilterCode_logTimeOnly() throws Exception {
    LOG.info("testJSONFilterCode_logTimeOnly()");

    init(new HashMap<String, Object>());

    mockOutputMgr.write(EasyMock.capture(capture), EasyMock.anyObject(InputMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputMgr);

    Date d = new Date();
    DateFormat sdf = new SimpleDateFormat(LogFeederUtil.SOLR_DATE_FORMAT);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String dateString = sdf.format(d);
    jsonFilterCode.apply("{ logtime: '" + d.getTime() + "', some_field: 'abc' }", new InputMarker());

    EasyMock.verify(mockOutputMgr);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Incorrect decoding: log time", dateString, jsonParams.remove("logtime"));
    assertEquals("Incorrect decoding: some field", "abc", jsonParams.remove("some_field"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @Test
  public void testJSONFilterCode_lineNumberOnly() throws Exception {
    LOG.info("testJSONFilterCode_lineNumberOnly()");

    init(new HashMap<String, Object>());

    mockOutputMgr.write(EasyMock.capture(capture), EasyMock.anyObject(InputMarker.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mockOutputMgr);

    jsonFilterCode.apply("{ line_number: 100, some_field: 'abc' }", new InputMarker());

    EasyMock.verify(mockOutputMgr);
    Map<String, Object> jsonParams = capture.getValue();

    assertEquals("Incorrect decoding: line number", 100l, jsonParams.remove("line_number"));
    assertEquals("Incorrect decoding: some field", "abc", jsonParams.remove("some_field"));
    assertTrue("jsonParams are not empty!", jsonParams.isEmpty());
  }

  @After
  public void cleanUp() {
    capture.reset();
  }
}
