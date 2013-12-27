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
package org.apache.ambari.server.actionmanager;

import static org.junit.Assert.*;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.Test;

public class TestStage {

  private static final String CLUSTER_HOST_INFO = "cluster_host_info";

  @Test
  public void testTaskTimeout() {
    Stage s = StageUtils.getATestStage(1, 1, "h1", CLUSTER_HOST_INFO);
    s.addHostRoleExecutionCommand("h1", Role.DATANODE, RoleCommand.INSTALL,
        null, "c1", "HDFS");
    s.addHostRoleExecutionCommand("h1", Role.HBASE_MASTER, RoleCommand.INSTALL,
        null, "c1", "HBASE");
    assertEquals(3*60000, s.getTaskTimeout());
  }

  @Test
  public void testGetRequestContext() {

    Stage stage = new Stage(1, "/logDir", "c1", "My Context", CLUSTER_HOST_INFO);
    assertEquals("My Context", stage.getRequestContext());
    assertEquals(CLUSTER_HOST_INFO, stage.getClusterHostInfo());
  }


}
