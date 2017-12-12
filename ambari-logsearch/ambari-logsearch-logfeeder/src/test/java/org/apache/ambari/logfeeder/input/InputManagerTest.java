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

package org.apache.ambari.logfeeder.input;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.junit.Test;

public class InputManagerTest {

  @Test
  public void testInputManager_addAndRemoveInputs() {
    Input input1 = strictMock(Input.class);
    Input input2 = strictMock(Input.class);
    Input input3 = strictMock(Input.class);
    Input input4 = strictMock(Input.class);
    
    expect(input3.getShortDescription()).andReturn("").times(2);
    expect(input4.getShortDescription()).andReturn("").once();
    
    replay(input1, input2, input3, input4);
    
    InputManager manager = new InputManager();
    manager.add("serviceName", input1);
    manager.add("serviceName", input2);
    manager.add("serviceName", input3);
    
    manager.removeInput(input3);
    manager.removeInput(input4);
    
    verify(input1, input2, input3, input4);
    
    List<Input> inputList = manager.getInputList("serviceName");
    assertEquals(inputList.size(), 2);
    assertEquals(inputList.get(0), input1);
    assertEquals(inputList.get(1), input2);
  }

  @Test
  public void testInputManager_monitor() throws Exception {
    Input input1 = strictMock(Input.class);
    Input input2 = strictMock(Input.class);
    Input input3 = strictMock(Input.class);

    LogFeederProps logFeederProps = new LogFeederProps();

    input1.init(logFeederProps); expectLastCall();
    input2.init(logFeederProps); expectLastCall();
    input3.init(logFeederProps); expectLastCall();
    
    expect(input1.isReady()).andReturn(true);
    expect(input2.isReady()).andReturn(true);
    expect(input3.isReady()).andReturn(false);
    
    expect(input1.monitor()).andReturn(false);
    expect(input2.monitor()).andReturn(false);
    expect(input3.getShortDescription()).andReturn("").once();
    
    replay(input1, input2, input3);
    
    InputManager manager = new InputManager();
    manager.setLogFeederProps(logFeederProps);
    manager.add("serviceName", input1);
    manager.add("serviceName", input2);
    manager.add("serviceName", input3);
    
    manager.startInputs("serviceName");
    
    verify(input1, input2, input3);
  }
  

  @Test
  public void testInputManager_addMetricsContainers() throws Exception {
    List<MetricData> metrics = new ArrayList<MetricData>();
    
    Input input1 = strictMock(Input.class);
    Input input2 = strictMock(Input.class);
    Input input3 = strictMock(Input.class);
    
    input1.addMetricsContainers(metrics); expectLastCall();
    input2.addMetricsContainers(metrics); expectLastCall();
    input3.addMetricsContainers(metrics); expectLastCall();
    
    expect(input1.isReady()).andReturn(true);
    expect(input2.isReady()).andReturn(true);
    expect(input3.isReady()).andReturn(false);
    
    replay(input1, input2, input3);
    
    InputManager manager = new InputManager();
    manager.add("serviceName", input1);
    manager.add("serviceName", input2);
    manager.add("serviceName", input3);
    
    manager.addMetricsContainers(metrics);
    
    verify(input1, input2, input3);
  }

  @Test
  public void testInputManager_logStat() throws Exception {
    Input input1 = strictMock(Input.class);
    Input input2 = strictMock(Input.class);
    Input input3 = strictMock(Input.class);
    
    input1.logStat(); expectLastCall();
    input2.logStat(); expectLastCall();
    input3.logStat(); expectLastCall();
    
    expect(input1.isReady()).andReturn(true);
    expect(input2.isReady()).andReturn(true);
    expect(input3.isReady()).andReturn(false);
    
    replay(input1, input2, input3);
    
    InputManager manager = new InputManager();
    manager.add("serviceName", input1);
    manager.add("serviceName", input2);
    manager.add("serviceName", input3);
    
    manager.logStats();
    
    verify(input1, input2, input3);
  }

  @Test
  public void testInputManager_checkInAll() throws Exception {
    Input input1 = strictMock(Input.class);
    Input input2 = strictMock(Input.class);
    Input input3 = strictMock(Input.class);
    
    input1.lastCheckIn(); expectLastCall();
    input2.lastCheckIn(); expectLastCall();
    input3.lastCheckIn(); expectLastCall();
    
    replay(input1, input2, input3);
    
    InputManager manager = new InputManager();
    manager.add("serviceName", input1);
    manager.add("serviceName", input2);
    manager.add("serviceName", input3);
    
    manager.checkInAll();
    
    verify(input1, input2, input3);
  }

  @Test
  public void testInputManager_close() throws Exception {
    Input input1 = strictMock(Input.class);
    Input input2 = strictMock(Input.class);
    Input input3 = strictMock(Input.class);
    
    input1.setDrain(true); expectLastCall();
    input2.setDrain(true); expectLastCall();
    input3.setDrain(true); expectLastCall();
    
    expect(input1.isClosed()).andReturn(true);
    expect(input2.isClosed()).andReturn(true);
    expect(input3.isClosed()).andReturn(true);
    
    replay(input1, input2, input3);
    
    InputManager manager = new InputManager();
    manager.add("serviceName", input1);
    manager.add("serviceName", input2);
    manager.add("serviceName", input3);
    
    manager.close();
    
    verify(input1, input2, input3);
  }
}
