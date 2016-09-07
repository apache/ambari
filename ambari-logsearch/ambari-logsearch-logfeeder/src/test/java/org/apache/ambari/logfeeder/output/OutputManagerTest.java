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

package org.apache.ambari.logfeeder.output;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.junit.Test;

public class OutputManagerTest {

  @Test
  public void testOutputManager_addAndRemoveOutputs() {
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    Output output4 = strictMock(Output.class);
    
    replay(output1, output2, output3, output4);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.retainUsedOutputs(Arrays.asList(output1, output2, output4));
    
    verify(output1, output2, output3, output4);
    
    List<Output> outputs = manager.getOutputs();
    assertEquals(outputs.size(), 2);
    assertEquals(outputs.get(0), output1);
    assertEquals(outputs.get(1), output2);
  }

  @Test
  public void testOutputManager_init() throws Exception {
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    output1.init(); expectLastCall();
    output2.init(); expectLastCall();
    output3.init(); expectLastCall();
    
    replay(output1, output2, output3);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.init();
    
    verify(output1, output2, output3);
  }

  @Test
  public void testOutputManager_write() throws Exception {
    Map<String, Object> jsonObj = new HashMap<>();
    jsonObj.put("type", "testType");
    jsonObj.put("path", "testPath");
    jsonObj.put("host", "testHost");
    jsonObj.put("ip", "testIp");
    jsonObj.put("level", "testLevel");
    jsonObj.put("id", "testId");
    
    Input mockInput = strictMock(Input.class);
    InputMarker inputMarker = new InputMarker(mockInput, null, 0);
    
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    expect(mockInput.getContextFields()).andReturn(Collections.<String, String> emptyMap());
    expect(mockInput.isUseEventMD5()).andReturn(false);
    expect(mockInput.isGenEventMD5()).andReturn(false);
    expect(mockInput.getOutputList()).andReturn(Arrays.asList(output1, output2, output3));
    
    output1.write(jsonObj, inputMarker); expectLastCall();
    output2.write(jsonObj, inputMarker); expectLastCall();
    output3.write(jsonObj, inputMarker); expectLastCall();
    
    replay(output1, output2, output3, mockInput);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.write(jsonObj, inputMarker);
    
    verify(output1, output2, output3, mockInput);
  }

  @Test
  public void testOutputManager_write2() throws Exception {
    String jsonString = "{}";
    
    Input mockInput = strictMock(Input.class);
    InputMarker inputMarker = new InputMarker(mockInput, null, 0);
    
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    expect(mockInput.getOutputList()).andReturn(Arrays.asList(output1, output2, output3));
    
    output1.write(jsonString, inputMarker); expectLastCall();
    output2.write(jsonString, inputMarker); expectLastCall();
    output3.write(jsonString, inputMarker); expectLastCall();
    
    replay(output1, output2, output3, mockInput);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.write(jsonString, inputMarker);
    
    verify(output1, output2, output3, mockInput);
  }

  @Test
  public void testOutputManager_addMetricsContainers() throws Exception {
    List<MetricData> metrics = new ArrayList<MetricData>();
    
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    output1.addMetricsContainers(metrics); expectLastCall();
    output2.addMetricsContainers(metrics); expectLastCall();
    output3.addMetricsContainers(metrics); expectLastCall();
    
    replay(output1, output2, output3);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.addMetricsContainers(metrics);
    
    verify(output1, output2, output3);
  }

  @Test
  public void testOutputManager_logStat() throws Exception {
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    output1.logStat(); expectLastCall();
    output2.logStat(); expectLastCall();
    output3.logStat(); expectLastCall();
    
    replay(output1, output2, output3);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.logStats();
    
    verify(output1, output2, output3);
  }

  @Test
  public void testOutputManager_copyFile() throws Exception {
    File f = new File("");
    
    Input mockInput = strictMock(Input.class);
    InputMarker inputMarker = new InputMarker(mockInput, null, 0);
    
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    expect(mockInput.getOutputList()).andReturn(Arrays.asList(output1, output2, output3));
    
    output1.copyFile(f, inputMarker); expectLastCall();
    output2.copyFile(f, inputMarker); expectLastCall();
    output3.copyFile(f, inputMarker); expectLastCall();
    
    replay(output1, output2, output3, mockInput);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.copyFile(f, inputMarker);
    
    verify(output1, output2, output3, mockInput);
  }

  @Test
  public void testOutputManager_close() throws Exception {
    Output output1 = strictMock(Output.class);
    Output output2 = strictMock(Output.class);
    Output output3 = strictMock(Output.class);
    
    output1.setDrain(true); expectLastCall();
    output2.setDrain(true); expectLastCall();
    output3.setDrain(true); expectLastCall();
    
    output1.close(); expectLastCall();
    output2.close(); expectLastCall();
    output3.close(); expectLastCall();
    
    expect(output1.isClosed()).andReturn(true);
    expect(output2.isClosed()).andReturn(true);
    expect(output3.isClosed()).andReturn(true);
    
    replay(output1, output2, output3);
    
    OutputManager manager = new OutputManager();
    manager.add(output1);
    manager.add(output2);
    manager.add(output3);
    
    manager.close();
    
    verify(output1, output2, output3);
  }
}
