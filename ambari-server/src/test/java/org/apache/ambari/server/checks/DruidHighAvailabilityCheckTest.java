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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;

/**
 * Unit tests for SecondaryNamenodeDeletedCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DruidHighAvailabilityCheckTest
{
  private final Clusters clusters = Mockito.mock(Clusters.class);

  private final DruidHighAvailabilityCheck druidHighAvailabilityCheck = new DruidHighAvailabilityCheck();

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    druidHighAvailabilityCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    druidHighAvailabilityCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return Mockito.mock(AmbariMetaInfo.class);
      }
    };

    Configuration config = Mockito.mock(Configuration.class);
    druidHighAvailabilityCheck.config = config;

    m_services.clear();
    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Service service = Mockito.mock(Service.class);

    m_services.put("DRUID", service);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getServices()).thenReturn(m_services);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    Assert.assertTrue(druidHighAvailabilityCheck.isApplicable(request));

    request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    request.addResult(CheckDescription.DRUID_HA_WARNING, PrereqCheckStatus.PASS);
    Assert.assertTrue(druidHighAvailabilityCheck.isApplicable(request));

    m_services.remove("DRUID");
    Assert.assertFalse(druidHighAvailabilityCheck.isApplicable(request));
  }

  @Test
  public void testPerform() throws Exception {
    final ServiceComponentHost serviceComponentHost= Mockito.mock(ServiceComponentHost.class);
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final Service service = Mockito.mock(Service.class);
    final ServiceComponent serviceComponent = Mockito.mock(ServiceComponent.class);
    final ServiceComponent haComponent = Mockito.mock(ServiceComponent.class);
    Mockito.when(serviceComponent.getServiceComponentHosts()).thenReturn(Collections.singletonMap("host", null));
    Mockito.when(haComponent.getServiceComponentHosts()).thenReturn(ImmutableMap.<String,ServiceComponentHost>of("host1", serviceComponentHost, "host2", serviceComponentHost));

    // All Components Not HA
    Mockito.when(cluster.getService("DRUID")).thenReturn(service);
    Mockito.when(service.getServiceComponent("DRUID_COORDINATOR")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_BROKER")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_MIDDLEMANAGER")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_HISTORICAL")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_OVERLORD")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_ROUTER")).thenReturn(serviceComponent);
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    druidHighAvailabilityCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    Assert.assertEquals("DRUID", check.getFailedOn().toArray(new String[1])[0]);
    Assert.assertEquals("High Availability is not enabled for Druid. Druid Service may have some downtime during upgrade. Deploy multiple instances of DRUID_BROKER, DRUID_COORDINATOR, DRUID_HISTORICAL, DRUID_OVERLORD, DRUID_MIDDLEMANAGER, DRUID_ROUTER in the Cluster to avoid any downtime.", check.getFailReason());

    // Some Components have HA
    Mockito.when(cluster.getService("DRUID")).thenReturn(service);
    Mockito.when(service.getServiceComponent("DRUID_COORDINATOR")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_BROKER")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_MIDDLEMANAGER")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_HISTORICAL")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_OVERLORD")).thenReturn(serviceComponent);
    Mockito.when(service.getServiceComponent("DRUID_ROUTER")).thenReturn(haComponent);
    check = new PrerequisiteCheck(null, null);
    druidHighAvailabilityCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
    Assert.assertEquals("DRUID", check.getFailedOn().toArray(new String[1])[0]);
    Assert.assertEquals("High Availability is not enabled for Druid. Druid Service may have some downtime during upgrade. Deploy multiple instances of DRUID_COORDINATOR, DRUID_OVERLORD, DRUID_MIDDLEMANAGER in the Cluster to avoid any downtime.", check.getFailReason());

    // All components have HA
    Mockito.when(cluster.getService("DRUID")).thenReturn(service);
    Mockito.when(service.getServiceComponent("DRUID_COORDINATOR")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_BROKER")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_MIDDLEMANAGER")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_HISTORICAL")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_OVERLORD")).thenReturn(haComponent);
    Mockito.when(service.getServiceComponent("DRUID_ROUTER")).thenReturn(haComponent);


    check = new PrerequisiteCheck(null, null);
    druidHighAvailabilityCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}
