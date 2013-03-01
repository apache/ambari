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

package org.apache.ambari.server.metadata;

import junit.framework.Assert;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.junit.Test;

public class RoleGraphTest {

  @Test
  public void testValidateOrder() {
    RoleCommandOrder rco = new RoleCommandOrder();
    RoleCommandOrder.initialize();

    RoleGraphNode datanode_upgrade = new RoleGraphNode(Role.DATANODE, RoleCommand.UPGRADE);
    RoleGraphNode hdfs_client_upgrade = new RoleGraphNode(Role.HDFS_CLIENT, RoleCommand.UPGRADE);
    Assert.assertEquals(-1, rco.order(datanode_upgrade, hdfs_client_upgrade));
    Assert.assertEquals(1, rco.order(hdfs_client_upgrade, datanode_upgrade));

    RoleGraphNode namenode_upgrade = new RoleGraphNode(Role.NAMENODE, RoleCommand.UPGRADE);
    RoleGraphNode ganglia_server_upgrade = new RoleGraphNode(Role.GANGLIA_SERVER, RoleCommand.UPGRADE);
    Assert.assertEquals(1, rco.order(ganglia_server_upgrade, hdfs_client_upgrade));
    Assert.assertEquals(1, rco.order(ganglia_server_upgrade, datanode_upgrade));
    Assert.assertEquals(-1, rco.order(namenode_upgrade, ganglia_server_upgrade));

    RoleGraphNode datanode_start = new RoleGraphNode(Role.DATANODE, RoleCommand.START);
    RoleGraphNode datanode_install = new RoleGraphNode(Role.DATANODE, RoleCommand.INSTALL);
    RoleGraphNode jobtracker_start = new RoleGraphNode(Role.JOBTRACKER, RoleCommand.START);
    Assert.assertEquals(1, rco.order(datanode_start, datanode_install));
    Assert.assertEquals(1, rco.order(jobtracker_start, datanode_start));

    Assert.assertEquals(0, rco.order(jobtracker_start, jobtracker_start));
  }
}
