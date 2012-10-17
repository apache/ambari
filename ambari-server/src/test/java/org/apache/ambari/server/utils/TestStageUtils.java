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
package org.apache.ambari.server.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;

public class TestStageUtils {
  private static Log LOG = LogFactory.getLog(TestStageUtils.class);
  
  @Test
  public void testGetATestStage() {
    Stage s = StageUtils.getATestStage(1, 2, "host2");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommand> cmds = s.getExecutionCommands(hostname);
    for (ExecutionCommand cmd : cmds) {
      assertEquals("cluster1", cmd.getClusterName());
      assertEquals(StageUtils.getActionId(1, 2), cmd.getCommandId());
      assertEquals(hostname, cmd.getHostname());
    }
  }
  
  @Test
  public void testJaxbToString() throws Exception {
    Stage s = StageUtils.getATestStage(1, 2, "host1");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommand> cmds = s.getExecutionCommands(hostname);
    for (ExecutionCommand cmd: cmds) {
      LOG.info("Command is " + StageUtils.jaxbToString(cmd));
    }
    assertEquals(StageUtils.getActionId(1, 2), s.getActionId());
  }

  @Test
  public void testJasonToExecutionCommand() throws JsonGenerationException,
      JsonMappingException, JAXBException, IOException {
    Stage s = StageUtils.getATestStage(1, 2, "host1");
    ExecutionCommand cmd = s.getExecutionCommands("host1").get(0);
    String json = StageUtils.jaxbToString(cmd);
    ExecutionCommand cmdDes = StageUtils.stringToExecutionCommand(json);
    assertEquals(cmd.toString(), cmdDes.toString());
    assertEquals(cmd, cmdDes);
  }
}
