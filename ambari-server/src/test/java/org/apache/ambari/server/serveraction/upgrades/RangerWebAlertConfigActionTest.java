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

package org.apache.ambari.server.serveraction.upgrades;


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import junit.framework.Assert;


/**
 * Test for @link RangerWebAlertConfigAction to ensure
 * ranger service alert configs are updated.
 */
@RunWith(MockitoJUnitRunner.class)
public class RangerWebAlertConfigActionTest {


  private RangerWebAlertConfigAction rangerWebAlertConfigAction;
  private Clusters m_clusters;
  private AlertDefinitionDAO alertDefinitionDAO;
  private AlertDefinitionEntity alertDefinitionEntity;
  private AlertDefinitionHash alertDefinitionHash;
  private AmbariEventPublisher eventPublisher;
  private Field clustersField;



  @Before
  public void setup() throws Exception {
    alertDefinitionDAO = Mockito.mock(AlertDefinitionDAO.class);
    alertDefinitionEntity = Mockito.mock(AlertDefinitionEntity.class);
    alertDefinitionHash = Mockito.mock(AlertDefinitionHash.class);
    eventPublisher = Mockito.mock(AmbariEventPublisher.class);
    m_clusters = Mockito.mock(Clusters.class);
    rangerWebAlertConfigAction = new RangerWebAlertConfigAction();
    clustersField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    clustersField.setAccessible(true);
  }

  @Test
  public void testExecute() throws Exception {

    String CLUSTER_NAME = "cl1";
    String RANGER_ADMIN_PROCESS = "ranger_admin_process";
    String pathname = "src/test/resources/stacks/HDP/2.5.0/services/RANGER/alerts.json";

    Cluster cluster = Mockito.mock(Cluster.class);

    Mockito.when(m_clusters.getCluster(Mockito.anyString())).thenReturn(cluster);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", CLUSTER_NAME);


    ExecutionCommand m_executionCommand = new ExecutionCommand();
    m_executionCommand.setCommandParams(commandParams);
    m_executionCommand.setClusterName(CLUSTER_NAME);

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(m_executionCommand)).anyTimes();
    replay(hrc);

    Mockito.when(alertDefinitionDAO.findByName(1L, RANGER_ADMIN_PROCESS)).thenReturn(alertDefinitionEntity);


    try {

      File alertsFile = new File(pathname);
      Assert.assertTrue(alertsFile.exists());

      StringBuilder rangerAlertsConfigFile = new StringBuilder((int) alertsFile.length());
      Scanner scanner = new Scanner(alertsFile);
      String lineSeparator = System.getProperty("line.separator");

      try {
        while (scanner.hasNextLine()) {
          rangerAlertsConfigFile.append(scanner.nextLine()).append(lineSeparator);
        }
        Mockito.when(alertDefinitionEntity.getSource()).thenReturn(rangerAlertsConfigFile.toString());
      } finally {
        scanner.close();
      }
    } catch (Exception e) {

      e.printStackTrace();
    }

    rangerWebAlertConfigAction.alertDefinitionDAO = alertDefinitionDAO;
    clustersField.set(rangerWebAlertConfigAction, m_clusters);
    rangerWebAlertConfigAction.alertDefinitionHash = alertDefinitionHash;
    rangerWebAlertConfigAction.eventPublisher = eventPublisher;

    rangerWebAlertConfigAction.setExecutionCommand(m_executionCommand);
    rangerWebAlertConfigAction.setHostRoleCommand(hrc);

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();
    CommandReport commandReport = null;
    try {
      commandReport = rangerWebAlertConfigAction.execute(requestSharedDataContext);
    } catch (Exception e) {
      e.printStackTrace();
    }

    Assert.assertNotNull(commandReport);
    Assert.assertEquals(0, commandReport.getExitCode());
    Assert.assertEquals(HostRoleStatus.COMPLETED.toString(), commandReport.getStatus());
    Assert.assertEquals("Ranger service alert check configuration has been updated successfully.", commandReport.getStdOut());
  }
}
