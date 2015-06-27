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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Provider;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * Unit tests for ServicesUpCheck
 *
 */
@RunWith(PowerMockRunner.class)               // Allow mocking static methods
@PrepareForTest(HostComponentSummary.class)   // This class has a static method that will be mocked
public class ServicesUpCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  @Test
  public void testIsApplicable() throws Exception {
    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.2.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.2"));

    Assert.assertTrue(new ServicesUpCheck().isApplicable(checkRequest));
  }

  @Test
  public void testPerform() throws Exception {
    PowerMockito.mockStatic(HostComponentSummary.class);

    final ServicesUpCheck servicesUpCheck = new ServicesUpCheck();
    servicesUpCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    servicesUpCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return Mockito.mock(AmbariMetaInfo.class);
      }
    };


    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP", "2.2"));
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final Service hdfsService = Mockito.mock(Service.class);
    final Service tezService = Mockito.mock(Service.class);
    final Service amsService = Mockito.mock(Service.class);

    HashMap<String, Service> clusterServices = new HashMap<String, Service>();
    clusterServices.put("HDFS", hdfsService);
    clusterServices.put("TEZ", tezService);
    clusterServices.put("AMBARI_METRICS", amsService);

    Mockito.when(hdfsService.getName()).thenReturn("HDFS");
    Mockito.when(tezService.getName()).thenReturn("TEZ");
    Mockito.when(amsService.getName()).thenReturn("AMBARI_METRICS");

    Mockito.when(hdfsService.isClientOnlyService()).thenReturn(false);
    Mockito.when(tezService.isClientOnlyService()).thenReturn(true);
    Mockito.when(amsService.isClientOnlyService()).thenReturn(false);

    Mockito.when(cluster.getServices()).thenReturn(clusterServices);


    // Put Components inside Services
    // HDFS
    Map<String, ServiceComponent> hdfsComponents = new HashMap<String, ServiceComponent>();

    ServiceComponent nameNode = Mockito.mock(ServiceComponent.class);
    Mockito.when(nameNode.getName()).thenReturn("NAMENODE");
    Mockito.when(nameNode.isClientComponent()).thenReturn(false);
    Mockito.when(nameNode.isVersionAdvertised()).thenReturn(true);

    ServiceComponent dataNode = Mockito.mock(ServiceComponent.class);
    Mockito.when(dataNode.getName()).thenReturn("DATANODE");
    Mockito.when(dataNode.isClientComponent()).thenReturn(false);
    Mockito.when(dataNode.isVersionAdvertised()).thenReturn(true);

    ServiceComponent zkfc = Mockito.mock(ServiceComponent.class);
    Mockito.when(zkfc.getName()).thenReturn("ZKFC");
    Mockito.when(zkfc.isClientComponent()).thenReturn(false);
    Mockito.when(zkfc.isVersionAdvertised()).thenReturn(false);

    hdfsComponents.put("NAMENODE", nameNode);
    hdfsComponents.put("DATANODE", dataNode);
    hdfsComponents.put("ZKFC", zkfc);

    Mockito.when(hdfsService.getServiceComponents()).thenReturn(hdfsComponents);

    // TEZ
    Map<String, ServiceComponent> tezComponents = new HashMap<String, ServiceComponent>();

    ServiceComponent tezClient = Mockito.mock(ServiceComponent.class);
    Mockito.when(tezClient.getName()).thenReturn("TEZ_CLIENT");
    Mockito.when(tezClient.isClientComponent()).thenReturn(true);
    Mockito.when(tezClient.isVersionAdvertised()).thenReturn(true);

    tezComponents.put("TEZ_CLIENT", tezClient);

    Mockito.when(tezService.getServiceComponents()).thenReturn(tezComponents);

    // AMS
    Map<String, ServiceComponent> amsComponents = new HashMap<String, ServiceComponent>();

    ServiceComponent metricsCollector = Mockito.mock(ServiceComponent.class);
    Mockito.when(metricsCollector.getName()).thenReturn("METRICS_COLLECTOR");
    Mockito.when(metricsCollector.isClientComponent()).thenReturn(false);
    Mockito.when(metricsCollector.isVersionAdvertised()).thenReturn(false);

    ServiceComponent metricsMonitor = Mockito.mock(ServiceComponent.class);
    Mockito.when(metricsMonitor.getName()).thenReturn("METRICS_MONITOR");
    Mockito.when(metricsMonitor.isClientComponent()).thenReturn(false);
    Mockito.when(metricsMonitor.isVersionAdvertised()).thenReturn(false);

    amsComponents.put("METRICS_COLLECTOR", metricsCollector);
    amsComponents.put("METRICS_MONITOR", metricsMonitor);

    Mockito.when(amsService.getServiceComponents()).thenReturn(amsComponents);

    final HostComponentSummary hcsNameNode = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsDataNode = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsZKFC = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsTezClient = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsMetricsCollector = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsMetricsMonitor = Mockito.mock(HostComponentSummary.class);

    List<HostComponentSummary> allHostComponentSummaries = new ArrayList<HostComponentSummary>();
    allHostComponentSummaries.add(hcsNameNode);
    allHostComponentSummaries.add(hcsDataNode);
    allHostComponentSummaries.add(hcsZKFC);
    allHostComponentSummaries.add(hcsTezClient);
    allHostComponentSummaries.add(hcsMetricsCollector);
    allHostComponentSummaries.add(hcsMetricsMonitor);

    // Mock the static method
    Mockito.when(HostComponentSummary.getHostComponentSummaries("HDFS", "NAMENODE")).thenReturn(new ArrayList<HostComponentSummary>(){{ add(hcsNameNode); }});
    Mockito.when(HostComponentSummary.getHostComponentSummaries("HDFS", "DATANODE")).thenReturn(new ArrayList<HostComponentSummary>(){{ add(hcsDataNode); }});
    Mockito.when(HostComponentSummary.getHostComponentSummaries("HDFS", "ZKFC")).thenReturn(new ArrayList<HostComponentSummary>(){{ add(hcsZKFC); }});
    Mockito.when(HostComponentSummary.getHostComponentSummaries("TEZ", "TEZ_CLIENT")).thenReturn(new ArrayList<HostComponentSummary>(){{ add(hcsTezClient); }});
    Mockito.when(HostComponentSummary.getHostComponentSummaries("AMBARI_METRICS", "METRICS_COLLECTOR")).thenReturn(new ArrayList<HostComponentSummary>(){{ add(hcsMetricsCollector); }});
    Mockito.when(HostComponentSummary.getHostComponentSummaries("AMBARI_METRICS", "METRICS_MONITOR")).thenReturn(new ArrayList<HostComponentSummary>(){{ add(hcsMetricsMonitor); }});

    // Case 1. Initialize with good values
    for (HostComponentSummary hcs : allHostComponentSummaries) {
      Mockito.when(hcs.getDesiredState()).thenReturn(State.INSTALLED);
      Mockito.when(hcs.getCurrentState()).thenReturn(State.STARTED);
    }
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    servicesUpCheck.perform(check, new PrereqCheckRequest("cluster"));

    // Case 2. Change some desired states to STARTED, should still pass
    Mockito.when(hcsNameNode.getDesiredState()).thenReturn(State.STARTED);
    Mockito.when(hcsDataNode.getDesiredState()).thenReturn(State.STARTED);

    check = new PrerequisiteCheck(null, null);
    servicesUpCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Case 3. Ensure that ZKFC and AMS are ignored even if their current state is not STARTED
    Mockito.when(hcsZKFC.getCurrentState()).thenReturn(State.INSTALLED);
    Mockito.when(hcsMetricsCollector.getCurrentState()).thenReturn(State.INSTALLED);
    Mockito.when(hcsMetricsMonitor.getCurrentState()).thenReturn(State.INSTALLED);

    check = new PrerequisiteCheck(null, null);
    servicesUpCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Case 4. Change HDFS current states to INSTALLED, should fail.
    Mockito.when(hcsNameNode.getCurrentState()).thenReturn(State.INSTALLED);
    Mockito.when(hcsDataNode.getCurrentState()).thenReturn(State.INSTALLED);

    check = new PrerequisiteCheck(null, null);
    servicesUpCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
  }
}
