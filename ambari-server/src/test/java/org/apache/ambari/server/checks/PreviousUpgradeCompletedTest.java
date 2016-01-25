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
package org.apache.ambari.server.checks;

import com.google.inject.Provider;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;


/**
 * Unit tests for PreviousUpgradeCompleted
 *
 */
public class PreviousUpgradeCompletedTest {

  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final ClusterVersionDAO clusterVersionDAO = Mockito.mock(ClusterVersionDAO.class);
  private AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);
  private final RequestDAO requestDAO = Mockito.mock(RequestDAO.class);
  private HostRoleCommandDAO hrcDAO = Mockito.mock(HostRoleCommandDAO.class);
  private UpgradeDAO upgradeDAO = Mockito.mock(UpgradeDAO.class);

  private StackId sourceStackId = new StackId("HDP", "2.2");
  private StackId targetStackId = new StackId("HDP", "2.2");
  private String sourceRepositoryVersion = "2.2.6.0-1234";
  private String destRepositoryVersion = "2.2.8.0-5678";
  private String clusterName = "cluster";

  private PreviousUpgradeCompleted puc;
  /**
   *
   */
  @Before
  public void setup() {
    puc = new PreviousUpgradeCompleted();
    puc.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    puc.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ambariMetaInfo;
      }
    };

    puc.clusterVersionDAOProvider = new Provider<ClusterVersionDAO>() {
      @Override
      public ClusterVersionDAO get() {
        return clusterVersionDAO;
      }
    };

    puc.requestDaoProvider = new Provider<RequestDAO>() {
      @Override
      public RequestDAO get() {
        return requestDAO;
      }
    };

    puc.hostRoleCommandDaoProvider = new Provider<HostRoleCommandDAO>() {
      @Override
      public HostRoleCommandDAO get() {
        return hrcDAO;
      }
    };

    puc.upgradeDaoProvider = new Provider<UpgradeDAO>() {
      @Override
      public UpgradeDAO get() {
        return upgradeDAO;
      }
    };
  }

  @Test
  public void testPerform() throws Exception {
    StackId stackId = new StackId("HDP", "2.2");

    StackEntity stack = new StackEntity();
    stack.setStackName(stackId.getStackName());
    stack.setStackVersion(stackId.getStackVersion());
    RepositoryVersionEntity rve = new RepositoryVersionEntity(stack, sourceRepositoryVersion, sourceRepositoryVersion, "rhel6");
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterName()).thenReturn(clusterName);
    final long clusterId = 1L;
    Mockito.when(cluster.getClusterId()).thenReturn(clusterId);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest(clusterName);
    checkRequest.setRepositoryVersion(sourceRepositoryVersion);
    checkRequest.setSourceStackId(sourceStackId);
    checkRequest.setTargetStackId(targetStackId);

    List<UpgradeEntity> upgrades = new ArrayList<>();
    Mockito.when(upgradeDAO.findAll()).thenReturn(upgrades);

    Mockito.when(clusters.getCluster(clusterName)).thenReturn(cluster);

    
    // Case 1. No previous upgrades
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    puc.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());


    // Case 2. Initialize with only one completed upgrade
    final long upgradeRequestId = 1L;
    RequestEntity upgradeRequest = Mockito.mock(RequestEntity.class);
    Mockito.when(upgradeRequest.getRequestId()).thenReturn(upgradeRequestId);
    Mockito.when(upgradeRequest.getStartTime()).thenReturn(System.currentTimeMillis() - 1000);
    Mockito.when(requestDAO.findByPK(upgradeRequestId)).thenReturn(upgradeRequest);

    UpgradeEntity upgrade = Mockito.mock(UpgradeEntity.class);
    Mockito.when(upgrade.getClusterId()).thenReturn(clusterId);
    Mockito.when(upgrade.getRequestId()).thenReturn(upgradeRequestId);
    Mockito.when(upgrade.getDirection()).thenReturn(Direction.UPGRADE);
    Mockito.when(upgrade.getFromVersion()).thenReturn(sourceRepositoryVersion);
    Mockito.when(upgrade.getToVersion()).thenReturn(destRepositoryVersion);

    upgrades.add(upgrade);
    Mockito.when(upgradeDAO.findAll()).thenReturn(upgrades);

    check = new PrerequisiteCheck(null, null);
    puc.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());


    // Case 3. Initialize with a successful downgrade.
    final long downgradeRequestId = 2L;
    RequestEntity downgradeRequest = Mockito.mock(RequestEntity.class);
    Mockito.when(downgradeRequest.getRequestId()).thenReturn(downgradeRequestId);
    Mockito.when(downgradeRequest.getStartTime()).thenReturn(System.currentTimeMillis() - 500);
    Mockito.when(requestDAO.findByPK(downgradeRequestId)).thenReturn(downgradeRequest);

    UpgradeEntity downgrade = Mockito.mock(UpgradeEntity.class);
    Mockito.when(downgrade.getClusterId()).thenReturn(clusterId);
    Mockito.when(downgrade.getRequestId()).thenReturn(downgradeRequestId);
    Mockito.when(downgrade.getDirection()).thenReturn(Direction.DOWNGRADE);
    Mockito.when(downgrade.getFromVersion()).thenReturn(sourceRepositoryVersion);
    Mockito.when(downgrade.getToVersion()).thenReturn(sourceRepositoryVersion);

    upgrades.clear();
    upgrades.add(upgrade);
    upgrades.add(downgrade);
    Mockito.when(upgradeDAO.findAll()).thenReturn(upgrades);

    check = new PrerequisiteCheck(null, null);
    puc.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());


    // Case 4. The upgrade has no downgrade, and it has a COMPLETED "Save Cluster State" step, so it should pass.
    HostRoleCommandEntity finalizeCommand = Mockito.mock(HostRoleCommandEntity.class);
    Mockito.when(finalizeCommand.getStatus()).thenReturn(HostRoleStatus.COMPLETED);
    Mockito.when(finalizeCommand.getTaskId()).thenReturn(1000L);
    List<HostRoleCommandEntity> commands = new ArrayList<>();
    commands.add(finalizeCommand);
    Mockito.when(hrcDAO.findSortedCommandsByRequestIdAndCustomCommandName(upgradeRequestId, PreviousUpgradeCompleted.FINALIZE_ACTION_CLASS_NAME)).thenReturn(commands);

    upgrades.clear();
    upgrades.add(upgrade);
    Mockito.when(upgradeDAO.findAll()).thenReturn(upgrades);

    check = new PrerequisiteCheck(null, null);
    puc.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());


    // Case 5. The upgrade has no downgrade, and it has an ABORTED "Save Cluster State" step, so it should fail.
    Mockito.when(finalizeCommand.getStatus()).thenReturn(HostRoleStatus.ABORTED);
    upgrades.clear();
    upgrades.add(upgrade);
    Mockito.when(upgradeDAO.findAll()).thenReturn(upgrades);

    check = new PrerequisiteCheck(null, null);
    puc.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}
