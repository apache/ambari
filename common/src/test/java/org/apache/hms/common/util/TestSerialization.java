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

package org.apache.hms.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hms.common.entity.Status;
import org.apache.hms.common.entity.action.Action;
import org.apache.hms.common.entity.action.ActionDependency;
import org.apache.hms.common.entity.action.DaemonAction;
import org.apache.hms.common.entity.action.ActionStatus;
import org.apache.hms.common.entity.action.ScriptAction;
import org.apache.hms.common.entity.cluster.MachineState.StateEntry;
import org.apache.hms.common.entity.cluster.MachineState.StateType;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.CommandStatus.ActionEntry;
import org.apache.hms.common.entity.command.CommandStatus.HostStatusPair;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test Java Object to JSON serialization using Jackson
 *
 */
public class TestSerialization {
  
  /**
   * Test polymorphic handing of jackson serialization
   */
  @Test
  public void testPolymorphHandling() {
    DaemonAction x = new DaemonAction();
    x.setActionType("start");
    x.setDaemonName("hadoop-namenode");
    Action y = null;
    try {
      y = JAXBUtil.read(JAXBUtil.write(x), Action.class);
      if(y instanceof DaemonAction) {
        Assert.assertEquals(y.getClass().getCanonicalName(),x.getClass().getName());
        DaemonAction z = (DaemonAction) y;
        Assert.assertEquals(z.getActionType(), "start");
      } else {
        Assert.fail("y is not instance of DaemonAction");
      }
    } catch (IOException e) {
      Assert.fail("Serialization failed. "+e.getStackTrace());
    } catch (Exception e) {
      Assert.fail(x.getClass().getName()+" and "+y.getClass().getCanonicalName()+" does not match.");
    }
  }
  
  /**
   * Test action status serialization
   */
  @Test
  public void testActionStatus() {
    ActionStatus s = new ActionStatus();
    DaemonAction expected = new DaemonAction();
    expected.setActionType("start");
    expected.setDaemonName("hadoop-namenode");
    s.setCode(0);
    s.setAction(expected);
    s.setOutput("output");
    s.setError("error");
    
    try {
      ActionStatus e = JAXBUtil.read(JAXBUtil.write(s), ActionStatus.class);
      DaemonAction actual = (DaemonAction) e.getAction();
      Assert.assertEquals(actual.getActionType(), expected.getActionType());
      Assert.assertEquals(actual.getDaemonName(), expected.getDaemonName());
      Assert.assertEquals(e.getCode(), s.getCode());
      Assert.assertEquals(e.getOutput(), s.getOutput());
      Assert.assertEquals(e.getError(), s.getError());

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }
  
  /**
   * Test command status serialization
   */
  @Test
  public void testCommandStatus() {
    String test = "test";
    CommandStatus cs = new CommandStatus();
    List<ActionDependency> ad = new ArrayList<ActionDependency>();
    ActionDependency actionDep = new ActionDependency();
    List<String> hosts = new ArrayList<String>();
    hosts.add("localhost");
    actionDep.setHosts(hosts);
    List<StateEntry> states = new ArrayList<StateEntry>();    
    states.add(new StateEntry(StateType.DAEMON, "hadoop-namenode", Status.INSTALLED));
    actionDep.setStates(states);
    ad.add(actionDep);
    
    ScriptAction sa = new ScriptAction();
    sa.setScript("ls");
    sa.setDependencies(ad);
    
    String[] parameters = { "-l" };
    sa.setParameters(parameters);
    ActionEntry actionEntry = new ActionEntry();
    actionEntry.setAction(sa);
    List<HostStatusPair> hostStatus = new ArrayList<HostStatusPair>();
    actionEntry.setHostStatus(hostStatus);
    
    List<ActionEntry> alist = new ArrayList<ActionEntry>();
    alist.add(actionEntry);
    
    cs.setActionEntries(alist);
    cs.setClusterName(test);
    try {
      CommandStatus read = JAXBUtil.read(JAXBUtil.write(cs), CommandStatus.class);
      Assert.assertEquals(read.getClusterName(), cs.getClusterName());
    } catch(Exception e) {
      Assert.fail(ExceptionUtil.getStackTrace(e));
    }
  }
}
